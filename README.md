# Redmine 工作負載統計系統

這是一個基於 Spring Boot 開發的 Redmine 工作負載統計與分析系統,具有以下功能:

## 功能特色

- 📊 工作負載統計分析
- 📈 圖表視覺化 (使用 Chart.js)
- 📱 RWD 響應式設計
- 🔍 動態篩選功能
- 📋 詳細資料表格顯示

## 技術架構

- **後端框架**: Spring Boot 3.1.5
- **建置工具**: Gradle
- **Java 版本**: JDK 17
- **資料庫**: MySQL 8.0
- **前端技術**: Thymeleaf + HTML5 + CSS3 + JavaScript
- **圖表庫**: Chart.js
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
