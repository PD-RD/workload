# 人員層級勾選功能測試指南

## 🎯 測試目的
驗證修復後的 `handleUserLevelToggleSync()` 函數是否正確處理人員層級的勾選/取消勾選操作。

---

## 🐛 修復內容

### Bug 描述
**檔案**：`workload2d.html` (行 1349)
**問題**：使用 `for...of` 遍歷物件 `userObj.projects`，導致迴圈無法執行

### 修復前
```javascript
// ❌ 錯誤：userObj.projects 是物件，不是可迭代陣列
for (const projectName of userObj.projects) {
    const projectKey = `${userName}|${projectName}`;
    const projectObj = cache.projects[projectKey];
    // ...
}
```

### 修復後
```javascript
// ✅ 正確：使用 Object.entries() 遍歷物件
for (const [projectName, projectObj] of Object.entries(userObj.projects)) {
    // 直接使用 projectObj，無需再次查找
    // ...
}
```

---

## 📋 測試步驟

### 前置條件
1. 確保應用程式在 http://localhost:8080 運行
2. 打開瀏覽器開發者工具 (F12)
3. 切換到 Console 標籤

### 測試案例 1：勾選人員（小規模）
**目標使用者**：劉安元（155 個 ISSUE）

#### 步驟
1. 展開「劉安元」的所有專案和 ISSUE
   - 點擊人員左側的 ▶ 展開專案列表
   - 點擊每個專案左側的 ▶ 展開 ISSUE 列表

2. 確認初始狀態
   - 人員 checkbox：☐ 未勾選
   - 所有專案 checkbox：☐ 未勾選
   - 所有 ISSUE checkbox：☐ 未勾選

3. **勾選人員 checkbox**
   - 點擊「劉安元」列的 checkbox

4. ✅ **預期結果**
   - 人員 checkbox：☑ 已勾選
   - **所有專案 checkbox：☑ 已勾選**
   - **所有 ISSUE checkbox：☑ 已勾選**
   - Console 輸出：`[Cache] 人員 劉安元 階層勾選: true, 影響 156 個項目` (1 人員 + N 專案 + 155 ISSUE)
   - 執行時間：約 100-200ms

5. 檢查工時統計
   - 「劉安元」列的總工時應顯示所有 ISSUE 的總和
   - 每個週期的工時也應該更新

### 測試案例 2：取消勾選人員（小規模）
接續測試案例 1

#### 步驟
1. **取消勾選人員 checkbox**
   - 再次點擊「劉安元」列的 checkbox

2. ✅ **預期結果**
   - 人員 checkbox：☐ 未勾選
   - **所有專案 checkbox：☐ 未勾選**
   - **所有 ISSUE checkbox：☐ 未勾選**
   - Console 輸出：`[Cache] 人員 劉安元 階層勾選: false, 影響 156 個項目`
   - 執行時間：約 100-200ms

3. 檢查工時統計
   - 「劉安元」列的總工時應歸零
   - 每個週期的工時也應歸零

---

### 測試案例 3：勾選人員（大規模）
**目標使用者**：游汶艗（820 個 ISSUE）⭐ 關鍵測試

#### 步驟
1. 展開「游汶艗」的所有專案和 ISSUE
   - 點擊人員左側的 ▶
   - **注意**：如果專案太多，可以只展開幾個專案的 ISSUE

2. 確認初始狀態
   - 人員 checkbox：☐ 未勾選
   - 所有可見專案 checkbox：☐ 未勾選
   - 所有可見 ISSUE checkbox：☐ 未勾選

3. **勾選人員 checkbox**
   - 點擊「游汶艗」列的 checkbox

4. ✅ **預期結果**
   - 人員 checkbox：☑ 已勾選
   - **所有專案 checkbox（包括未展開的）：☑ 已勾選**
   - **所有 ISSUE checkbox：☑ 已勾選**
   - Console 輸出：`[Cache] 人員 游汶艗 階層勾選: true, 影響 831 個項目` (1 人員 + 10 專案 + 820 ISSUE)
   - 執行時間：約 200-400ms ⚠️

5. 驗證未展開的項目
   - 展開一個之前未展開的專案
   - 確認該專案的 checkbox 也是 ☑ 已勾選
   - 確認該專案的所有 ISSUE checkbox 也是 ☑ 已勾選

6. 檢查工時統計
   - 「游汶艗」列的總工時應顯示所有 820 個 ISSUE 的總和
   - 工時數字應該很大（例如：3500+ 小時）

---

### 測試案例 4：部分勾選狀態（進階）
驗證父層 checkbox 的三種狀態顯示

#### 步驟
1. 展開「劉安元」的第一個專案（例如：「ADAS-新專案」）

2. 只勾選該專案的第一個 ISSUE
   - 點擊該 ISSUE 的 checkbox

3. ✅ **預期結果**
   - ISSUE checkbox：☑ 已勾選
   - 專案 checkbox：◐ 部分勾選（indeterminate 狀態）
   - 人員 checkbox：◐ 部分勾選（indeterminate 狀態）

4. **勾選人員 checkbox**
   - 點擊「劉安元」列的 checkbox

5. ✅ **預期結果**
   - 人員 checkbox：☑ 已勾選
   - 所有專案 checkbox：☑ 已勾選
   - 所有 ISSUE checkbox：☑ 已勾選（包括之前勾選的那一個）

---

## 🐛 如果測試失敗...

### 問題 A：專案/ISSUE checkbox 沒有被勾選
**可能原因**：
1. `Object.entries()` 沒有正確遍歷 `userObj.projects`
2. `projectObj` 為 `undefined`

**檢查方式**：
```javascript
// 在 Console 輸入
const cache = window.__WL_CACHE__;
console.log(cache.users['劉安元'].projects);
// 應該顯示一個物件，包含多個專案
```

### 問題 B：Console 顯示錯誤訊息
**常見錯誤**：
```
TypeError: userObj.projects is not iterable
```
**原因**：修復沒有生效，仍然使用 `for...of userObj.projects`

**解決方式**：
1. 確認 `workload2d.html` 已保存
2. 重新啟動應用程式
3. 清除瀏覽器快取（Ctrl + F5）

### 問題 C：執行時間超過 1 秒
**可能原因**：
1. `toggleRowCostExclusion()` 函數性能問題
2. DOM 操作過多

**檢查方式**：
```javascript
// 在 Console 輸入
const cache = window.__WL_CACHE__;
cache.DEBUG = true;

// 然後勾選人員，觀察 Console 輸出的執行時間
```

---

## 📊 性能基準

### 小規模使用者（150-200 個 ISSUE）
- **執行時間**：100-200ms ✅
- **更新項目數**：~150-210 個
- **使用者體驗**：瞬間完成，無感知

### 中規模使用者（300-500 個 ISSUE）
- **執行時間**：150-300ms ✅
- **更新項目數**：~310-520 個
- **使用者體驗**：快速完成，輕微延遲

### 大規模使用者（800+ 個 ISSUE）
- **執行時間**：200-400ms ⚠️
- **更新項目數**：~831 個
- **使用者體驗**：可接受的延遲

---

## ✅ 測試檢查清單

- [ ] 劉安元（小規模）勾選測試通過
- [ ] 劉安元取消勾選測試通過
- [ ] 游汶艗（大規模）勾選測試通過
- [ ] 部分勾選狀態顯示正確
- [ ] 工時統計正確更新
- [ ] Console 無錯誤訊息
- [ ] 執行時間符合預期
- [ ] 父層 checkbox 狀態正確（全選/部分/未選）

---

## 🎯 成功標準

### 功能正確性
✅ 勾選人員時，所有下層專案和 ISSUE 的 checkbox 都被勾選
✅ 取消勾選人員時，所有下層專案和 ISSUE 的 checkbox 都被取消
✅ 工時統計正確反映勾選狀態

### 性能標準
✅ 小規模使用者：<200ms
✅ 中規模使用者：<300ms
⚠️ 大規模使用者：<500ms（可接受）

### 使用者體驗
✅ 操作流暢，無卡頓
✅ 視覺回饋即時（checkbox 狀態更新）
✅ 工時數字即時更新

---

## 📝 測試報告範例

### 測試環境
- **瀏覽器**：Chrome 120.0.6099.110
- **OS**：Windows 11
- **測試日期**：2025-10-15

### 測試結果
| 測試案例 | 使用者 | ISSUE 數量 | 執行時間 | 結果 |
|---------|--------|-----------|---------|------|
| 勾選人員（小） | 劉安元 | 155 | 145ms | ✅ 通過 |
| 取消勾選（小） | 劉安元 | 155 | 132ms | ✅ 通過 |
| 勾選人員（大） | 游汶艗 | 820 | 286ms | ✅ 通過 |
| 部分勾選狀態 | 劉安元 | 155 | 150ms | ✅ 通過 |

### 結論
✅ 所有測試案例通過，功能正常運作

---

## 🔧 除錯工具

### 啟用 DEBUG 模式
```javascript
// 在 Console 輸入
window.__WL_CACHE__.DEBUG = true;
```

### 檢查 cache 結構
```javascript
// 查看使用者的專案結構
console.log(window.__WL_CACHE__.users['劉安元'].projects);

// 查看某個專案的 ISSUE
console.log(window.__WL_CACHE__.projects['劉安元|ADAS-新專案'].issues);
```

### 手動測試函數
```javascript
// 直接調用函數測試
handleUserLevelToggleSync('劉安元', true);  // 勾選
handleUserLevelToggleSync('劉安元', false); // 取消勾選
```

---

**重要提示**：
1. 每次修改代碼後，必須重新啟動應用程式並刷新瀏覽器（Ctrl + F5）
2. 測試時建議打開 Console 觀察輸出
3. 如果測試失敗，先檢查是否有 JavaScript 錯誤
4. 大規模測試（游汶艗）可能需要較長時間，請耐心等待

**測試完成後，請將結果記錄在此文件中！** ✍️
