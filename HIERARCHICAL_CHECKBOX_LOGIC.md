# 階層式成本計算 Checkbox 邏輯說明

## 📋 功能概述

實現了三層階層式的成本計算勾選邏輯：
1. **人員層級** (issueId = -1)
2. **專案層級** (issueId = -2)
3. **ISSUE 層級** (issueId > 0)

## 🔄 階層式勾選邏輯

### 1. ISSUE 層級取消勾選 (issueId > 0)
當取消勾選某個 ISSUE 時：
- ✅ 該 ISSUE 立即從成本計算中排除
- ✅ 自動更新該 ISSUE 所屬**專案**的勾選狀態
- ✅ 自動更新該 ISSUE 所屬**人員**的勾選狀態
- ✅ 成本統計即時更新

**範例**：
```
👤 張三 [全選]
  📁 專案A [全選]
    - ISSUE #1 ✓
    - ISSUE #2 ✓ → 取消勾選
    - ISSUE #3 ✓

結果：
👤 張三 [部分選取 - indeterminate]
  📁 專案A [部分選取 - indeterminate]
    - ISSUE #1 ✓
    - ISSUE #2 ✗
    - ISSUE #3 ✓
```

### 2. 專案層級取消勾選 (issueId = -2)
當取消勾選某個專案時：
- ✅ 該專案下的**所有 ISSUE** 全部取消勾選
- ✅ 自動更新該專案所屬**人員**的勾選狀態
- ✅ 成本統計即時更新

**範例**：
```
👤 張三 [全選]
  📁 專案A [全選] → 取消勾選
    - ISSUE #1 ✓
    - ISSUE #2 ✓
    - ISSUE #3 ✓
  📁 專案B [全選]
    - ISSUE #4 ✓

結果：
👤 張三 [部分選取 - indeterminate]
  📁 專案A [取消]
    - ISSUE #1 ✗
    - ISSUE #2 ✗
    - ISSUE #3 ✗
  📁 專案B [全選]
    - ISSUE #4 ✓
```

### 3. 人員層級取消勾選 (issueId = -1)
當取消勾選某個人員時：
- ✅ 該人員下的**所有專案**全部取消勾選
- ✅ 該人員下的**所有 ISSUE** 全部取消勾選
- ✅ 成本統計即時更新

**範例**：
```
👤 張三 [全選] → 取消勾選
  📁 專案A [全選]
    - ISSUE #1 ✓
    - ISSUE #2 ✓
  📁 專案B [全選]
    - ISSUE #3 ✓

結果：
👤 張三 [取消]
  📁 專案A [取消]
    - ISSUE #1 ✗
    - ISSUE #2 ✗
  📁 專案B [取消]
    - ISSUE #3 ✗
```

## 🎨 視覺反饋

### Checkbox 狀態顯示
- ✅ **全選** (`checked = true`): 該層級下所有子項目都被勾選
- ⬜ **未選** (`checked = false`): 該層級下所有子項目都未勾選
- ▣ **部分選取** (`indeterminate = true`): 該層級下部分子項目被勾選

### 行樣式
- **勾選項目**: 正常顯示
- **取消勾選項目**: 半透明 + 淡紅色背景 (`excluded-from-cost` class)

## 💻 技術實現

### 核心函數

1. **toggleCostCalculation(checkbox)**
   - 主入口函數
   - 根據 issueId 判斷層級並調用對應處理函數

2. **handleUserLevelToggle(userName, isChecked)**
   - 處理人員層級的勾選/取消勾選
   - 批量更新該人員下所有專案和 ISSUE

3. **handleProjectLevelToggle(userName, projectName, isChecked)**
   - 處理專案層級的勾選/取消勾選
   - 批量更新該專案下所有 ISSUE
   - 更新父層（人員）的狀態

4. **updateProjectCheckboxState(userName, projectName)**
   - 根據 ISSUE 狀態自動更新專案 checkbox
   - 支持 indeterminate 狀態

5. **updateParentCheckboxState(userName)**
   - 根據專案狀態自動更新人員 checkbox
   - 支持 indeterminate 狀態

6. **updateCostStatistics()**
   - 掃描所有 checkbox 的當前狀態
   - 純前端計算成本統計
   - 即時更新顯示

## 📊 成本統計計算

成本統計只計算**實際 ISSUE** (issueId > 0) 的工時：
- 不計算人員總計行 (issueId = -1)
- 不計算專案總計行 (issueId = -2)
- 根據 checkbox 的 `checked` 狀態決定是否納入成本計算

統計面板顯示：
- **納入計算工時**: 所有勾選 ISSUE 的工時總和
- **排除計算工時**: 所有未勾選 ISSUE 的工時總和
- **總工時**: 所有 ISSUE 的工時總和
- **納入項目數**: 勾選的 ISSUE 數量 / 總 ISSUE 數量

## 🔍 除錯功能

開啟瀏覽器開發者工具 Console 可以看到詳細的執行日誌：
- Checkbox 狀態變更追蹤
- 階層式更新過程
- 成本計算過程
- DOM 更新確認

測試函數：在 Console 輸入 `testCostCalculation()` 可查看所有 checkbox 的狀態。

## 🎯 使用方式

1. **開啟 2D 分析頁面**
2. **執行查詢**，顯示人員、專案、ISSUE 的工時數據
3. **勾選/取消勾選** checkbox 來調整成本計算範圍
4. **觀察統計面板** 即時更新

## ✅ 測試場景

### 場景 1: 取消單個 ISSUE
- 取消勾選任一 ISSUE
- 檢查專案 checkbox 是否變為 indeterminate
- 檢查人員 checkbox 是否變為 indeterminate
- 檢查統計面板數據是否正確

### 場景 2: 取消整個專案
- 取消勾選某個專案
- 檢查該專案下所有 ISSUE 是否都被取消勾選
- 檢查人員 checkbox 狀態
- 檢查統計面板數據是否正確

### 場景 3: 取消整個人員
- 取消勾選某個人員
- 檢查該人員下所有專案是否都被取消勾選
- 檢查該人員下所有 ISSUE 是否都被取消勾選
- 檢查統計面板數據是否正確

### 場景 4: 重新勾選
- 重新勾選之前取消的項目
- 檢查父層狀態是否正確更新
- 檢查統計面板數據是否正確

## 📝 注意事項

1. 所有計算都在**純前端**進行，無需重新請求後端
2. 數據保存在 HTML 的 `data-*` 屬性中
3. 支持多人、多專案、多 ISSUE 的複雜場景
4. 父層 checkbox 的狀態會根據子項目自動更新
5. indeterminate 狀態用於表示部分選取的情況
