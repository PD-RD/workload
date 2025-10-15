# 🚀 Redmine 工作負載統計系統

> 基於 Spring Boot 3.1.5 開發的企業級 Redmine 工作負載分析與視覺化系統

## 📋 專案概述

本系統專為連接 **Redmine 3.4.4** 設計，提供全方位的工作負載統計、分析與視覺化功能。支援多維度數據分析、智能工時分配計算，以及直觀的 2D 分析介面。

### 🎯 核心功能

- **📊 多維度統計分析** - 部門、人員、專案層級統計
- **�️ 靈活時間粒度** - 支援日/週/月三種時間維度
- **🎨 智能視覺化** - 2D 分析表格與顏色編碼工作量等級
- **📱 響應式設計** - 現代化 UI/UX，支援各種設備
- **� 精確工時計算** - 智能假日排除與每日工時分配

## 🛠️ 技術架構

### 後端技術棧
- **Spring Boot 3.1.5** - 主要應用框架
- **JDK 17** - Java 運行環境
- **Gradle 8.4** - 建置管理工具
- **MySQL 8.0** - 資料庫 (連接 Redmine)
- **HikariCP** - 高效能連接池
- **Spring Data JPA** - 資料存取層

### 前端技術棧
- **Thymeleaf** - 伺服器端模板引擎
- **Bootstrap 風格 CSS** - 響應式樣式設計
- **Vanilla JavaScript** - 互動式使用者介面
- **Chart.js Ready** - 圖表視覺化準備

## 🚀 快速開始

### 環境需求
- Java 17+
- MySQL 8.0+
- Gradle 8.4+ (或使用內建 gradlew)

### 資料庫配置
```properties
# application.properties
spring.datasource.url=jdbc:mysql://192.168.51.37:3306/bitnami_redmine?useSSL=false&serverTimezone=Asia/Taipei
spring.datasource.username=root
spring.datasource.password=your_password
```

### 啟動應用程式
```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

應用程式將在 `http://localhost:8080` 啟動

## 📊 功能特色

### 1. 工作負載統計 📈
- **手風琴式 UI** - 直觀的專案分組顯示
- **即時統計** - 總議題數、總工時、平均工時
- **狀態追蹤** - 開放/關閉議題統計
- **時間範圍查詢** - 彈性的日期區間選擇

### 2. 2D 分析視覺化 🎯
- **階層式展開** - 人員 → 專案 → 議題 三層結構
- **時間粒度選擇** - 日/週/月動態切換
- **智能工時分配** - 每日平均分散 (自動扣除假日)
- **顏色編碼系統** - 視覺化工作量等級

### 3. 顏色編碼工作量等級 🎨
| 工作量等級 | 顏色標示 | 條件 |
|------------|----------|------|
| 🔘 無工作量 | 灰色背景 | 0.0 小時 |
| 🟢 正常工作量 | 淺綠色背景 | < 8.0 小時 |
| 🟠 高工作量 | 橙色背景 | ≥ 8.0 小時 |
| 🔴 逾期工作 | 紅色背景 | 超過截止日期 |

### 4. 智能計算邏輯 🧮
- **精確日期計算** - 工作日智能識別
- **假日排除機制** - 週末自動排除計算
- **底層往上聚合** - 議題 → 專案 → 人員 層級聚合
- **動態時間範圍** - 使用者輸入範圍精確處理

## 🏗️ 架構設計

```
src/
├── main/
│   ├── java/com/redmine/workload/
│   │   ├── WorkloadApplication.java         # 主程式入口
│   │   ├── controller/
│   │   │   ├── WorkloadController.java      # 主要控制器
│   │   │   └── TestController.java          # 測試控制器
│   │   ├── model/
│   │   │   ├── WorkloadData.java           # 工作負載數據模型
│   │   │   ├── WorkloadStatistics.java     # 統計數據模型
│   │   │   └── WorkloadAnalysis2D.java     # 2D分析數據模型
│   │   ├── service/
│   │   │   └── WorkloadService.java        # 業務邏輯服務
│   │   └── repository/
│   │       └── WorkloadRepository.java     # 數據存取層
│   └── resources/
│       ├── application.properties          # 應用程式配置
│       └── templates/
│           ├── index.html                  # 主頁面
│           └── workload2d.html            # 2D分析頁面
```

## 🔧 設定與配置

### 資料庫連線
系統設計連接到 Redmine 3.4.4 的 MySQL 資料庫：
- **主機**: 192.168.51.37:3306
- **資料庫**: bitnami_redmine
- **表格**: issues, projects, users, groups_users, issue_statuses

### 效能最佳化
- **連接池**: HikariCP 高效能連接管理
- **SQL 最佳化**: 索引友善的查詢設計
- **記憶體管理**: 大數據集分頁處理
- **快取機制**: 群組與使用者資料快取

## 🐛 偵錯與維護

### SQL 查詢日誌
系統提供詳細的 SQL 執行日誌：
```
=== Executing 2D Analysis Query ===
Group Name: 服務開發處
User Fullname: 
Start Date: 2025-10-14
End Date: 2025-12-31
=== Final 2D Analysis SQL Command ===
[完整 SQL 查詢語句]
```

### 常見問題排解
1. **資料庫連線問題** - 檢查 application.properties 設定
2. **記憶體不足** - 調整 JVM 參數 `-Xmx2g`
3. **查詢效能** - 檢查資料庫索引設定
4. **模板解析錯誤** - 檢查 Thymeleaf 語法

## 📈 未來發展規劃

- [ ] **圖表視覺化** - 整合 Chart.js 動態圖表
- [ ] **匯出功能** - Excel/PDF 報表匯出
- [ ] **即時通知** - 工作量預警系統
- [ ] **API 開放** - RESTful API 介面
- [ ] **行動端支援** - PWA 漸進式網頁應用
- [ ] **多語言支援** - 國際化 i18n 功能

## 🤝 貢獻指南

歡迎提交 Issue 和 Pull Request！請遵循以下規範：
1. Fork 專案並建立功能分支
2. 遵循現有程式碼風格
3. 添加適當的單元測試
4. 更新相關文件

## 📄 授權條款

本專案採用 [MIT License](LICENSE) 授權條款。

---

**開發團隊**: PD-RD  
**最後更新**: 2025年10月15日  
**版本**: v1.0.0
- **資料庫連線**: 傳統 JDBC Template

## 專案結構

```
workload2/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/redmine/workload/
│       │       ├── WorkloadApplication.java       # 主程式
│       │       ├── controller/
│       │       │   └── WorkloadController.java    # 控制器
│       │       ├── service/
│       │       │   └── WorkloadService.java       # 服務層
│       │       ├── repository/
│       │       │   └── WorkloadRepository.java    # 資料存取層
│       │       └── model/
│       │           ├── WorkloadData.java          # 資料模型
│       │           └── WorkloadStatistics.java    # 統計模型
│       └── resources/
│           ├── application.properties              # 應用程式設定
│           └── templates/
│               └── index.html                      # 前端頁面
├── build.gradle                                    # Gradle 建置檔
└── README.md                                       # 說明文件
```

## 資料庫連線設定

- **主機**: 192.168.51.37
- **埠號**: 3306
- **資料庫**: bitnami_redmine
- **密碼**: 70759028Rd

## 如何執行

### 方式 1: 使用 Gradle Wrapper (建議)

```powershell
.\gradlew.bat bootRun
```

### 方式 2: 建置後執行

```powershell
.\gradlew.bat build
java -jar build\libs\redmine-workload-1.0.0.jar
```

## 存取應用程式

啟動後,在瀏覽器開啟:
```
http://localhost:8080
```

## 功能說明

### 1. 部門與使用者選擇
- 動態載入部門群組
- 根據部門自動載入使用者

### 2. 日期區間選擇
- 預設開始日期: 當天
- 預設結束日期: 當年度最後一天

### 3. 統計資訊顯示
- 總工時估計
- 日均工時
- 總議題數
- 已完成議題數
- 進行中議題數
- 完成率

### 4. 圖表分析
- 各專案工時分布長條圖

### 5. 詳細資料表格
- 議題編號
- 專案名稱
- 議題主旨
- 開始/結束日期
- 估計工時
- 日均工時
- 狀態

## RWD 響應式設計

系統支援多種裝置:
- 桌面電腦 (>768px)
- 平板電腦 (768px)
- 手機 (<768px)

## 注意事項

1. 確保已安裝 JDK 17
2. 確保資料庫連線正常
3. 確保防火牆允許存取 192.168.51.37:3306
4. 建議使用 Chrome、Firefox 或 Edge 瀏覽器

## 開發者資訊

- Spring Boot Version: 3.1.5
- Java Version: 17
- Gradle Version: 8.x
- Database: MySQL 8.0 (Redmine 3.4.4)
