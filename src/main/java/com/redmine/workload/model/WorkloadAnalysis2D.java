package com.redmine.workload.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadAnalysis2D {
    private String groupName;
    private String userFullname;
    private String projectName;
    private Long issueId;
    private String issueSubject;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal estimatedHours;
    private List<DailyWorkload> dailyWorkloads;
    private List<PeriodWorkload> periodWorkloads; // 新增：支援週/月顆粒度
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyWorkload {
        private LocalDate date;
        private BigDecimal hours;
        private boolean isWeekend;
        private boolean isOverdue;
        private String status; // 0.0, 正常工作量數值, 或 "過期"
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PeriodWorkload {
        private String period; // 期間標記，例如 "2025-W42" 或 "2025-10"
        private LocalDate startDate; // 期間開始日期
        private LocalDate endDate; // 期間結束日期
        private BigDecimal hours;
        private String status;
        private String granularity; // "weekly" 或 "monthly"
    }
}