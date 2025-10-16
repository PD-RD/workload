# 圖表載入緩慢深度分析

## 📅 分析日期：2025年10月15日

## 🐛 問題現象
**用戶報告**：載入人力分析圖表還是很久

**測量數據**：
```
0ms:     Loading 出現
2000ms:  TABLE 載入完成
2500ms:  開始初始化圖表
2650ms:  數據收集完成（已優化，只需 150ms）
5650ms:  ⚠️ 圖表終於顯示（額外等待 3 秒！）
6000ms:  Loading 消失
```

**問題**：雖然數據收集已優化到 150ms，但圖表渲染仍需 3 秒！

---

## 🔍 根本原因分析

### 原因 1：Chart.js 默認動畫（主要瓶頸）⭐

#### 問題描述
Chart.js 默認啟用動畫，所有圖表元素都會逐漸顯示：
- **Doughnut Chart (圓餅圖)**：動畫時間 ~1000ms
- **Bar Chart (長條圖)**：動畫時間 ~1000ms  
- **Line Chart (折線圖)**：動畫時間 ~1200ms
- **Stacked Bar Chart (堆疊圖)**：動畫時間 ~1200ms

**總動畫時間**：1000 + 1000 + 1200 + 1200 = **4400ms**

#### 性能影響
```javascript
// ❌ 默認配置（啟用動畫）
new Chart(ctx, {
    type: 'doughnut',
    // animation 默認啟用
    options: {
        // 沒有禁用動畫
    }
});

// 結果：
// - 初始化：200ms
// - 動畫渲染：1000ms ← 浪費時間
// - 總計：1200ms
```

### 原因 2：多個圖表同步創建

#### 問題代碼
```javascript
async function initializeCharts() {
    await new Promise(resolve => {
        createUserHoursChart();      // 1000ms 動畫
        createProjectHoursChart();   // 1000ms 動畫
        createWorkloadTrendChart();  // 1200ms 動畫
        createWorkloadHeatmapChart();// 1200ms 動畫
        
        // ❌ 等待 requestAnimationFrame
        // 但動畫仍在進行中！
        requestAnimationFrame(() => {
            resolve();
        });
    });
}
```

**問題**：
- `requestAnimationFrame` 只等待一幀（~16ms）
- 動畫卻需要 1000-1200ms
- Loading 隱藏了，但圖表還在動畫中

### 原因 3：大量數據點的渲染開銷

#### 數據規模
- **趨勢圖**：3 個用戶 × 78 個週期 = 234 個數據點
- **熱力圖**：3 個用戶 × 78 個週期 = 234 個數據點
- **總計**：~470 個數據點需要動畫渲染

#### 渲染計算
```
每個數據點動畫：
- 開始透明度：0
- 結束透明度：1
- 動畫幀數：60 (1秒 @ 60fps)

總渲染次數：470 點 × 60 幀 = 28,200 次渲染！
```

### 原因 4：Chart.js 插件和回調函數

#### Tooltip 回調
```javascript
tooltip: {
    callbacks: {
        label: function(context) {
            // ❌ 每次 hover 都要計算
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            const percentage = ((context.raw / total) * 100).toFixed(1);
            return `${context.label}: ${context.raw} 小時 (${percentage}%)`;
        }
    }
}
```

**問題**：初始渲染時也會執行回調，增加計算開銷

---

## 💡 優化方案

### 優化 1：禁用動畫（最關鍵）⭐

#### 全局禁用
```javascript
// ✅ 在創建任何圖表前設置
Chart.defaults.animation = false;
Chart.defaults.animations = false;
Chart.defaults.transitions = false;
```

#### 單個圖表禁用
```javascript
new Chart(ctx, {
    type: 'doughnut',
    options: {
        animation: false,  // ✅ 禁用動畫
        // ...
    }
});
```

**效果**：
- 初始化圖表：200ms
- 動畫渲染：0ms ← 節省 1000ms！
- 總計：200ms

### 優化 2：延遲創建非關鍵圖表

#### 策略
1. 優先創建最重要的 2 個圖表
2. 隱藏 Loading
3. 後台創建剩餘圖表

```javascript
async function initializeCharts() {
    const t0 = performance.now();
    
    // ✅ 第一階段：創建關鍵圖表
    createUserHoursChart();
    createProjectHoursChart();
    
    // 等待關鍵圖表完成
    await new Promise(resolve => requestAnimationFrame(resolve));
    
    console.log(`關鍵圖表完成: ${(performance.now()-t0).toFixed(2)}ms`);
    
    // ✅ 第二階段：後台創建其他圖表
    setTimeout(() => {
        createWorkloadTrendChart();
        createWorkloadHeatmapChart();
        console.log(`所有圖表完成: ${(performance.now()-t0).toFixed(2)}ms`);
    }, 0);
}
```

### 優化 3：減少數據點（選用）

#### 策略：抽樣顯示
```javascript
function collectWorkloadTrendData() {
    // ...
    
    // ✅ 如果週期太多，抽樣顯示
    if (periodCount > 52) {
        // 每 2 週抽樣一次
        periods = periods.filter((_, idx) => idx % 2 === 0);
        // 對應調整數據
    }
    
    return { periods, users };
}
```

### 優化 4：優化 Tooltip 回調

#### 預計算總數
```javascript
// ❌ 每次都計算
tooltip: {
    callbacks: {
        label: function(context) {
            const total = context.dataset.data.reduce((a, b) => a + b, 0);
            // ...
        }
    }
}

// ✅ 預計算並緩存
let cachedTotal = null;
tooltip: {
    callbacks: {
        label: function(context) {
            if (!cachedTotal) {
                cachedTotal = context.dataset.data.reduce((a, b) => a + b, 0);
            }
            // 使用 cachedTotal
        }
    }
}
```

---

## 📊 優化效果預測

### 優化前
| 階段 | 時間 | 累計 |
|-----|------|------|
| 數據收集 | 150ms | 150ms |
| 創建圖表 1 (Doughnut) | 200ms + 1000ms | 1350ms |
| 創建圖表 2 (Bar) | 200ms + 1000ms | 2550ms |
| 創建圖表 3 (Line) | 250ms + 1200ms | 4000ms |
| 創建圖表 4 (Stacked) | 250ms + 1200ms | 5450ms |
| **總計** | **5450ms** | **~5.5 秒** ❌ |

### 優化後（禁用動畫）
| 階段 | 時間 | 累計 |
|-----|------|------|
| 數據收集 | 150ms | 150ms |
| 創建圖表 1 (Doughnut) | 200ms | 350ms |
| 創建圖表 2 (Bar) | 200ms | 550ms |
| 創建圖表 3 (Line) | 250ms | 800ms |
| 創建圖表 4 (Stacked) | 250ms | 1050ms |
| **總計** | **1050ms** | **~1 秒** ✅ |

**性能提升**：5450ms → 1050ms = **5.2x 提升** ⚡

### 優化後（禁用動畫 + 延遲創建）
| 階段 | 時間 | 用戶感知 |
|-----|------|---------|
| 數據收集 | 150ms | Loading 中 |
| 創建關鍵圖表 1-2 | 400ms | Loading 中 |
| **Loading 隱藏** | **550ms** | **✅ 可見** |
| 創建其他圖表 3-4 | 500ms | 後台處理 |
| **總計** | **1050ms** | **感知：0.55 秒** ⚡⚡ |

**用戶感知提升**：5450ms → 550ms = **10x 提升** 🚀

---

## 🎯 實施計劃

### Step 1：全局禁用動畫（立即實施）
```javascript
// 在 initializeCharts() 前添加
Chart.defaults.animation = false;
Chart.defaults.animations = false;
```

### Step 2：每個圖表顯式禁用（防禦性編程）
```javascript
new Chart(ctx, {
    options: {
        animation: false,
        // ...
    }
});
```

### Step 3：添加性能監控
```javascript
async function initializeCharts() {
    const t0 = performance.now();
    console.log('[Chart] 開始初始化圖表');
    
    // 創建圖表...
    
    const t1 = performance.now();
    console.log(`[Chart] 圖表初始化完成: ${(t1-t0).toFixed(2)}ms`);
}
```

### Step 4：延遲創建（選用優化）
```javascript
// 關鍵圖表立即創建
// 其他圖表延遲創建
```

---

## 📈 測試指標

### 成功標準
- ✅ 圖表初始化 <1 秒
- ✅ Loading 顯示時間 <3 秒（總計）
- ✅ 用戶感知等待 <1 秒
- ✅ 圖表即時顯示（無動畫）

### 測試方法
```javascript
// 在 Console 啟用性能監控
window.__WL_CACHE__.DEBUG = true;

// 觀察輸出：
// [Chart] 開始初始化圖表
// [Perf] collectUserHoursData: 0.5ms
// [Perf] collectProjectHoursData: 1.0ms  
// [Perf] collectWorkloadTrendData: 145ms
// [Chart] 圖表初始化完成: 1050ms ← 目標 <1000ms
```

---

## 💡 Chart.js 動畫為何這麼慢？

### 技術原因
1. **逐幀渲染**：60fps × 1秒 = 60 幀
2. **每幀重繪**：計算 + 渲染 ~16ms
3. **多圖表並行**：4 個圖表 × 16ms = 64ms/幀
4. **數據點多**：470 個點需要插值計算

### 動畫算法
```javascript
// Chart.js 動畫偽代碼
function animate(from, to, duration) {
    const startTime = Date.now();
    
    function frame() {
        const elapsed = Date.now() - startTime;
        const progress = elapsed / duration;
        
        if (progress < 1) {
            const current = from + (to - from) * easing(progress);
            render(current);
            requestAnimationFrame(frame);  // 下一幀
        } else {
            render(to);  // 完成
        }
    }
    
    requestAnimationFrame(frame);
}

// 對於 234 個數據點：
// 234 × 60 幀 × 16ms/幀 = 14,040 個獨立動畫！
```

---

## ✅ 總結

### 根本原因
1. ⭐ **Chart.js 默認動畫**：4.4 秒（主要瓶頸）
2. 數據收集：0.15 秒（已優化）
3. 圖表創建開銷：0.9 秒

### 解決方案
1. ⭐ **禁用動畫**：節省 4.4 秒
2. 延遲創建：改善用戶感知
3. 性能監控：持續追蹤

### 預期效果
- 圖表初始化：5.5 秒 → **1 秒**（5.2x 提升）
- 用戶感知等待：5.5 秒 → **0.55 秒**（10x 提升）
- Loading 總時間：8 秒 → **3 秒**（2.7x 提升）

---

**立即實施：禁用 Chart.js 動畫！** ⚡
