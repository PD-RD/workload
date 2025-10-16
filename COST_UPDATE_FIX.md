# 成本連動問題修正說明

## 問題描述
使用者反應：勾選/取消勾選 ISSUE 或專案時，成本統計沒有即時更新。

## 根本原因分析

### 1. 去抖動延遲感知
- **原設計**：所有操作統一使用 120ms 去抖動延遲
- **用戶感受**：單次點擊感覺反應遲鈍

### 2. 快取初始化時機
- **潛在問題**：如果快取未正確初始化或 periodHours 為 0，會導致計算結果錯誤

### 3. 視覺反饋不足
- **原設計**：成本數字直接更新，無明顯變化提示
- **用戶感受**：不確定系統是否有響應

## 修正內容

### 修正 1：動態調整延遲策略
```javascript
// 單一 toggle 操作：50ms（更快反應）
// 批次操作（toggleAll）：120ms（避免卡頓）
const delay = immediate ? 0 : (reason === 'toggle' ? 50 : 120);
```

### 修正 2：強化快取保護
```javascript
async function updateCostStatisticsAsync() {
    if (!window.__WL_CACHE__) {
        buildWorkloadCache();
        refreshIssuePeriodHours(); // ✅ 確保 periodHours 已計算
    }
    // ...
}
```

### 修正 3：即時讀取勾選狀態
```javascript
// ✅ 每次都即時讀取 checkbox.checked，不依賴快取的舊狀態
if (issue.checkbox.checked) { includedHours += hours; }
```

### 修正 4：視覺反饋增強
```javascript
// 成本數字更新時閃爍藍色（300ms），提供明顯的視覺反饋
includedElement.style.color = '#2196F3';
setTimeout(() => { includedElement.style.color = ''; }, 300);
```

### 修正 5：移除不必要的 async
```javascript
// toggleCostCalculation 改為同步函數（無需 await）
function toggleCostCalculation(checkbox) { ... }
```

## 測試驗證步驟

### 1. 開啟 DEBUG 模式
在瀏覽器 Console 執行：
```javascript
__WL_CACHE__.DEBUG = true;
```

### 2. 測試單一 ISSUE 勾選
1. 勾選任意一個 ISSUE checkbox
2. **預期結果**：
   - Checkbox 立即勾選 ✓
   - 50ms 後 Console 顯示 `[Sched] 排程重算 reason= toggle`
   - 成本統計數字更新並閃爍藍色
   - Console 顯示 `[Perf] updateCostStatisticsAsync ms= X.X`

### 3. 測試專案層級勾選
1. 勾選一個專案的 checkbox
2. **預期結果**：
   - 專案及其下所有 ISSUE 立即勾選 ✓
   - 50ms 後成本統計更新
   - 專案總工時 span 文字更新

### 4. 測試人員層級勾選
1. 勾選一個人員的 checkbox
2. **預期結果**：
   - 人員、其下所有專案及 ISSUE 立即勾選 ✓
   - 50ms 後成本統計更新
   - 人員總工時 span 文字更新

### 5. 測試連續快速點擊
1. 快速連續勾選 5-10 個 ISSUE
2. **預期結果**：
   - 所有 checkbox 立即響應
   - Console 只顯示 1 次重算（去抖動生效）
   - 最後一次點擊 50ms 後成本更新

## 效能對比

| 場景 | 修正前 | 修正後 | 改善 |
|------|--------|--------|------|
| 單次勾選延遲 | 120ms | 50ms | **58% ↓** |
| 視覺反饋 | 無 | 藍色閃爍 | ✅ 新增 |
| 快取健壯性 | 可能失效 | 自動重建 | ✅ 增強 |
| 連續點擊合併 | ✅ | ✅ | 保持 |

## 常見問題排查

### Q1: 成本統計仍然沒更新
**排查步驟**：
1. 檢查 Console 是否有錯誤
2. 執行 `window.__WL_CACHE__` 檢查快取是否存在
3. 執行 `buildWorkloadCache(); refreshIssuePeriodHours();` 手動重建
4. 檢查 `includedHours` 元素是否存在：`document.getElementById('includedHours')`

### Q2: 藍色閃爍效果看不到
**可能原因**：
- CSS transition 被其他樣式覆蓋
- 瀏覽器渲染太快（正常現象，計算仍然執行了）

### Q3: Console 顯示 "快取仍在執行"
**說明**：
- 這是正常的保護機制
- 表示前一次計算還在進行，會自動重新排程

## 後續優化建議

### 選項 1：合併計算循環
將 `updateCostStatisticsAsync` 和 `updateSummaryRowsHoursAsync` 合併為單一循環：
```javascript
async function updateAllStatistics() {
    // 一次迴圈同時計算 cost 與 summary
    for (const issue of cache.issues) {
        // 累計 cost
        // 累計 user/project aggregation
    }
}
```
**預期收益**：再減少 20-30% 耗時

### 選項 2：增量更新
對於只有少數 checkbox 變化的情況，僅重算受影響的部分：
```javascript
function incrementalUpdate(affectedUser, affectedProject) {
    // 只重算特定 user/project 的統計
}
```
**預期收益**：大幅提升大資料集效能

### 選項 3：更明顯的計算中提示
在成本統計區域添加「⏳ 計算中...」浮動提示：
```javascript
function showCalculatingHint() {
    const hint = document.createElement('div');
    hint.textContent = '⏳ 計算中...';
    hint.style.cssText = 'position:fixed; top:10px; right:10px; ...';
    document.body.appendChild(hint);
    setTimeout(() => hint.remove(), 500);
}
```

---

**修正版本**：1.1  
**修正日期**：2025-10-15  
**測試狀態**：待驗證
