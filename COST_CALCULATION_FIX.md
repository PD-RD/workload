# 修正後的階層式成本計算邏輯

## 🔧 修正內容

### 問題描述
之前的實現中，取消單個 ISSUE 的勾選會導致整個專案也被取消勾選，這是不正確的。

### 修正後的邏輯

#### 1. **ISSUE 層級取消勾選**
當取消勾選某個 ISSUE 時：
- ✅ 該 ISSUE 從成本計算中排除
- ✅ 專案 checkbox 變為**部分選取狀態** (indeterminate)
- ✅ 專案**保持勾選** (checked = true, indeterminate = true)
- ✅ 人員 checkbox 也更新為**部分選取狀態**
- ✅ 成本計算只減掉該 ISSUE 的工時

**範例**：
```
初始狀態：
👤 張三 [✓ 全選] - 24 hrs
  📁 專案A [✓ 全選] - 24 hrs
    - ISSUE #1 [✓] 8 hrs
    - ISSUE #2 [✓] 8 hrs
    - ISSUE #3 [✓] 8 hrs

取消 ISSUE #2：
👤 張三 [✓ 部分選取] - 16 hrs (24 - 8)
  📁 專案A [✓ 部分選取] - 16 hrs (24 - 8)
    - ISSUE #1 [✓] 8 hrs
    - ISSUE #2 [✗] 8 hrs (排除)
    - ISSUE #3 [✓] 8 hrs

統計面板：
- 納入計算工時: 16.0 hrs
- 排除計算工時: 8.0 hrs
- 總工時: 24.0 hrs
- 納入項目數: 2 / 3
```

#### 2. **專案層級取消勾選**
當取消勾選某個專案時：
- ✅ 該專案下**所有 ISSUE** 都被取消勾選
- ✅ 人員 checkbox 更新狀態（全選/部分選取/未選）
- ✅ 成本計算減掉該專案所有 ISSUE 的工時

**範例**：
```
初始狀態：
👤 張三 [✓ 全選] - 40 hrs
  📁 專案A [✓ 全選] - 24 hrs
    - ISSUE #1 [✓] 8 hrs
    - ISSUE #2 [✓] 8 hrs
    - ISSUE #3 [✓] 8 hrs
  📁 專案B [✓ 全選] - 16 hrs
    - ISSUE #4 [✓] 8 hrs
    - ISSUE #5 [✓] 8 hrs

取消專案A：
👤 張三 [✓ 部分選取] - 16 hrs (40 - 24)
  📁 專案A [✗ 未選] - 0 hrs
    - ISSUE #1 [✗] 8 hrs (排除)
    - ISSUE #2 [✗] 8 hrs (排除)
    - ISSUE #3 [✗] 8 hrs (排除)
  📁 專案B [✓ 全選] - 16 hrs
    - ISSUE #4 [✓] 8 hrs
    - ISSUE #5 [✓] 8 hrs

統計面板：
- 納入計算工時: 16.0 hrs
- 排除計算工時: 24.0 hrs
- 總工時: 40.0 hrs
- 納入項目數: 2 / 5
```

#### 3. **人員層級取消勾選**
當取消勾選某個人員時：
- ✅ 該人員下**所有專案**都被取消勾選
- ✅ 該人員下**所有 ISSUE** 都被取消勾選
- ✅ 成本計算減掉該人員所有 ISSUE 的工時

## 🎨 視覺狀態

### Checkbox 狀態
1. **✓ 全選** (`checked = true, indeterminate = false`)
   - 該層級下所有子項目都被勾選
   - 正常顯示，無背景色

2. **✓ 部分選取** (`checked = true, indeterminate = true`)
   - 該層級下部分子項目被勾選
   - 正常顯示，無背景色
   - checkbox 顯示為特殊的 indeterminate 圖示

3. **✗ 未選** (`checked = false, indeterminate = false`)
   - 該層級下所有子項目都未勾選
   - 半透明 + 淡紅色背景

### 重要變更
- **部分選取的項目不會有排除背景色**
- 只有完全未勾選的項目才會顯示排除樣式

## 💻 技術實現細節

### 核心修正

#### 1. `updateProjectCheckboxState()` 函數
```javascript
if (checkedCount === totalCount) {
    // 全部勾選
    projectCheckbox.checked = true;
    projectCheckbox.indeterminate = false;
} else if (checkedCount === 0) {
    // 全部未勾選
    projectCheckbox.checked = false;
    projectCheckbox.indeterminate = false;
} else {
    // 部分勾選 - 關鍵修正！
    projectCheckbox.checked = true; // 保持勾選狀態
    projectCheckbox.indeterminate = true; // 設為部分選取
}
```

#### 2. `updateParentCheckboxState()` 函數
```javascript
// 檢查該人員下的所有 ISSUE 狀態（不只是專案）
const allIssueCheckboxes = document.querySelectorAll(
    `input[data-user="${userName}"][data-issue]:not([data-issue="-1"]):not([data-issue="-2"])`
);

// 計算已勾選的 ISSUE 數量
let checkedCount = 0;
allIssueCheckboxes.forEach(cb => {
    if (cb.checked) checkedCount++;
});

// 根據比例設置人員狀態
if (checkedCount === totalCount) {
    // 全選
} else if (checkedCount === 0) {
    // 未選
} else {
    // 部分選取
    userCheckbox.checked = true;
    userCheckbox.indeterminate = true;
}
```

#### 3. `toggleRowCostExclusion()` 函數
```javascript
if (checkbox.checked) {
    // 完全勾選：移除排除樣式
    row.classList.remove('excluded-from-cost');
} else if (checkbox.indeterminate) {
    // 部分選取：也移除排除樣式（因為仍有部分內容被計入）
    row.classList.remove('excluded-from-cost');
} else {
    // 完全未勾選：添加排除樣式
    row.classList.add('excluded-from-cost');
}
```

## 📊 成本計算邏輯

### 計算規則
1. **只計算實際 ISSUE** (issueId > 0) 的工時
2. **不計算總計行** (issueId = -1 或 -2) 避免重複
3. **根據每個 ISSUE 的 checkbox 狀態**決定是否納入
4. **專案和人員的 indeterminate 狀態不影響計算**

### 計算公式
```javascript
納入工時 = Σ (已勾選的 ISSUE 工時)
排除工時 = Σ (未勾選的 ISSUE 工時)
總工時 = 納入工時 + 排除工時
```

## 🧪 測試場景

### 場景 1: 取消單個 ISSUE
1. 開啟 2D 分析頁面
2. 取消勾選某個 ISSUE
3. **檢查點**：
   - ✅ 該 ISSUE 顯示排除樣式（淡紅色背景）
   - ✅ 所屬專案變為部分選取狀態（✓ 但有 indeterminate 圖示）
   - ✅ 所屬專案**沒有**排除樣式（正常顯示）
   - ✅ 所屬人員變為部分選取狀態
   - ✅ 成本統計正確減少該 ISSUE 的工時

### 場景 2: 取消整個專案
1. 取消勾選某個專案
2. **檢查點**：
   - ✅ 該專案下所有 ISSUE 都被取消勾選
   - ✅ 所有 ISSUE 都顯示排除樣式
   - ✅ 專案本身顯示排除樣式
   - ✅ 人員狀態正確更新
   - ✅ 成本統計正確減少該專案所有工時

### 場景 3: 重新勾選部分 ISSUE
1. 重新勾選之前取消的某個 ISSUE
2. **檢查點**：
   - ✅ 該 ISSUE 移除排除樣式
   - ✅ 專案狀態正確更新（可能從未選→部分選取，或從部分選取→全選）
   - ✅ 人員狀態正確更新
   - ✅ 成本統計正確增加該 ISSUE 的工時

### 場景 4: 混合操作
1. 取消部分 ISSUE
2. 取消某個專案
3. 重新勾選部分項目
4. **檢查點**：
   - ✅ 所有層級的狀態都正確反映子項目的實際狀態
   - ✅ 成本統計始終準確
   - ✅ 視覺樣式正確（部分選取不會有排除背景）

## 🎯 關鍵改進

1. **部分選取狀態的正確處理**
   - `checked = true, indeterminate = true` 表示部分選取
   - 這樣可以保持層級關係，同時正確反映狀態

2. **視覺樣式的改進**
   - 部分選取的項目不顯示排除樣式
   - 只有完全未勾選的項目才顯示排除樣式

3. **成本計算的準確性**
   - 始終只計算 ISSUE 層級的工時
   - 不受總計行的影響
   - 準確反映每個 ISSUE 的勾選狀態

4. **更好的用戶體驗**
   - 清晰的視覺反饋
   - 符合直覺的操作邏輯
   - 即時的成本統計更新

## 📝 注意事項

1. **indeterminate 狀態**是 HTML5 checkbox 的標準屬性
2. **只能通過 JavaScript 設置**，不能在 HTML 中直接設置
3. **瀏覽器會自動顯示特殊圖示**（通常是一個橫線或方塊）
4. **成本計算完全基於 ISSUE 層級**，總計行僅用於顯示和導航

## 🔍 除錯建議

開啟瀏覽器開發者工具 Console，可以看到：
- 專案狀態更新日誌：顯示已勾選/總數比例
- 人員狀態更新日誌：顯示已勾選/總數比例
- 成本計算過程：顯示每個 ISSUE 的處理狀態
- DOM 更新確認：顯示統計面板的更新前後值

測試時建議使用 Console 來確認邏輯執行正確。
