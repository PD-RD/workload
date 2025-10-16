# 異動 ISSUE 或專案時計算緩慢原因分析

## 執行日期：2025年10月15日

## 問題概述
當勾選或取消勾選 ISSUE 或專案時，系統需要計算很久才能完成，Loading 動畫會顯示較長時間。

## 根本原因分析

### 1. **主要瓶頸：updateWorkloadCellsAsync() 函數**

#### 問題點 A：仍在使用 querySelectorAll 進行全域 DOM 掃描
```javascript
// 行 2294 - 掃描所有工作量單元格
const allWorkloadCells = document.querySelectorAll('td.workload-cell[data-period-index]');
// 結果：可能有數千個單元格需要掃描 (每個 ISSUE × 每個時間段)

// 行 2319 - 在迴圈內使用 querySelector 查找 checkbox
const checkbox = document.querySelector(
    `input[data-user="${userName}"][data-project="${projectName}"][data-issue="${issueId}"]`
);
// 結果：每個單元格都要執行一次 DOM 查詢
```

**性能影響：**
- 假設有 100 個 ISSUE，每個 ISSUE 有 12 個時間段（月份）
- 總單元格數：100 × 12 = 1,200 個
- querySelectorAll 執行時間：~50-100ms
- querySelector 在迴圈中執行 1,200 次：~1,200-2,400ms
- **總 DOM 查詢時間：1.3-2.5 秒**

#### 問題點 B：重複掃描人員和專案總計單元格
```javascript
// 行 2368 - 再次掃描人員總計單元格
const userCells = document.querySelectorAll('td.workload-cell[data-user][data-issue="-1"][data-period-index]');

// 行 2403 - 再次掃描專案總計單元格
const projectCells = document.querySelectorAll('td.workload-cell[data-user][data-project][data-issue="-2"][data-period-index]');
```

**性能影響：**
- 額外 2 次全域 DOM 掃描
- 每次 ~50-100ms
- **額外時間：100-200ms**

#### 問題點 C：批次處理的延遲累積
```javascript
// 行 2349 - 每批處理後都延遲 5ms
await new Promise(resolve => setTimeout(resolve, 5));
```

**性能影響：**
- 假設 1,200 個單元格，每批 200 個 = 6 批
- 延遲累積：6 × 5ms = 30ms
- 加上人員和專案總計的批次延遲
- **總延遲時間：50-80ms**

#### 問題點 D：視覺回饋動畫的延遲
```javascript
// 行 2388-2393 - 每個更新的單元格都添加 300ms 動畫
cell.style.transition = 'background-color 0.3s ease';
cell.style.backgroundColor = '#cce5ff';
setTimeout(() => {
    cell.style.backgroundColor = '';
}, 300);
```

**性能影響：**
- 雖然使用 setTimeout，但大量的動畫會消耗 GPU 資源
- 如果同時更新 100+ 個單元格，會造成明顯卡頓

---

### 2. **次要瓶頸：scheduleCostAndSummaryRecalc() 執行流程**

執行順序（行 2033-2055）：
```javascript
// 第1步：更新成本統計
await updateCostStatisticsAsync();        // ~10-30ms

// 第2步：更新總計行工時統計
await updateSummaryRowsHoursAsyncOnly();  // ~20-50ms

// 第3步：更新每日工時（最慢）
await updateWorkloadCellsAsync();         // ~1,500-3,000ms ⚠️

// 第4步：更新圖表
await updateCharts();                     // ~50-200ms
```

**總執行時間：1.6-3.3 秒**

---

## 性能數據估算

### 小型專案（50 個 ISSUE）
- DOM 查詢：~800ms
- 數據處理：~200ms
- 視覺更新：~300ms
- **總計：~1.3 秒**

### 中型專案（150 個 ISSUE）
- DOM 查詢：~2,400ms
- 數據處理：~600ms
- 視覺更新：~500ms
- **總計：~3.5 秒** ⚠️

### 大型專案（300+ 個 ISSUE）
- DOM 查詢：~5,000ms
- 數據處理：~1,200ms
- 視覺更新：~1,000ms
- **總計：~7.2 秒** 🔴

---

## 優化建議

### 🚀 高優先級優化（預期提升 70-80%）

#### 1. 將 updateWorkloadCellsAsync 改用內存快取
```javascript
async function updateWorkloadCellsAsync() {
    const cache = window.__WL_CACHE__;
    
    // ✅ 使用快取中的 workloadCells 取代 querySelectorAll
    const allWorkloadCells = cache.workloadCells;
    
    // ✅ 使用快取中的 checkboxByKey 取代迴圈內的 querySelector
    for (const cellObj of allWorkloadCells) {
        if (cellObj.issueId > 0) {
            const key = `${cellObj.user}|${cellObj.project}|${cellObj.issueId}`;
            const checkbox = cache.checkboxByKey[key];
            // ... 處理邏輯
        }
    }
    
    // ✅ 使用快取的 cellsByUser 和 cellsByProject
    for (const userName in cache.cellsByUser) {
        const userCells = cache.cellsByUser[userName];
        // 只處理 issueId === '-1' 的單元格
    }
}
```

**預期提升：**
- DOM 查詢時間：2,400ms → 0ms（消除）
- **提升 70%+ 性能**

#### 2. 移除不必要的批次延遲
```javascript
// ❌ 移除這些延遲
// await new Promise(resolve => setTimeout(resolve, 5));

// ✅ 只在真正需要時使用 requestAnimationFrame
await new Promise(resolve => requestAnimationFrame(resolve));
```

**預期提升：**
- 減少 50-80ms 累積延遲

#### 3. 優化視覺回饋動畫
```javascript
// ❌ 不要為每個單元格單獨設置動畫
// cell.style.transition = 'background-color 0.3s ease';

// ✅ 使用 CSS class 批量處理
cell.classList.add('cell-updated');
setTimeout(() => {
    cell.classList.remove('cell-updated');
}, 300);
```

---

### 🔧 中優先級優化（預期提升 10-20%）

#### 4. 使用增量更新而非全量更新
```javascript
// 只更新受影響的單元格
function updateAffectedCells(issueId, userName, projectName) {
    const issueKey = `${userName}|${projectName}|${issueId}`;
    const affectedCells = cache.cellsByIssue[issueKey];
    
    // 只更新這些單元格的人員和專案總計
    // 而不是掃描所有單元格
}
```

#### 5. 延遲圖表更新
```javascript
// 圖表更新可以延後執行，不影響數據準確性
setTimeout(() => {
    if (typeof updateCharts === 'function') {
        updateCharts();
    }
}, 100);
```

---

## 優化後預期性能

### 小型專案（50 個 ISSUE）
- **優化前：~1.3 秒**
- **優化後：~0.2 秒**
- **提升：85%** ⚡

### 中型專案（150 個 ISSUE）
- **優化前：~3.5 秒**
- **優化後：~0.5 秒**
- **提升：86%** ⚡

### 大型專案（300+ 個 ISSUE）
- **優化前：~7.2 秒**
- **優化後：~1.0 秒**
- **提升：86%** ⚡

---

## 實施計劃

### Phase 1：緊急優化（今天完成）
1. ✅ updateWorkloadCellsAsync 改用快取
2. ✅ 移除批次延遲
3. ✅ 優化視覺動畫

### Phase 2：進階優化（本週完成）
1. 增量更新機制
2. 延遲非關鍵更新
3. 添加性能監控

### Phase 3：架構優化（下週完成）
1. Web Worker 處理密集計算
2. 虛擬滾動優化大量數據
3. 防抖節流優化使用者互動

---

## 結論

**主要問題：**
`updateWorkloadCellsAsync()` 函數中仍在使用大量的 `querySelectorAll` 和 `querySelector`，導致在處理中大型專案時，每次勾選操作都需要 2-7 秒的處理時間。

**解決方案：**
完全改用已建立的內存快取 `window.__WL_CACHE__`，可以將處理時間從 2-7 秒降低到 0.2-1 秒，提升 85%+ 的性能。

**立即行動：**
需要重構 `updateWorkloadCellsAsync()` 函數，使用：
- `cache.workloadCells` 取代 `querySelectorAll()`
- `cache.checkboxByKey` 取代迴圈內的 `querySelector()`
- `cache.cellsByUser` 和 `cache.cellsByProject` 直接訪問總計單元格
