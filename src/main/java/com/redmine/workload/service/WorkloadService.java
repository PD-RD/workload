package com.redmine.workload.service;

import com.redmine.workload.model.WorkloadData;
import com.redmine.workload.model.WorkloadStatistics;
import com.redmine.workload.model.WorkloadAnalysis2D;
import com.redmine.workload.repository.WorkloadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WorkloadService {

    @Autowired
    private WorkloadRepository workloadRepository;

    public WorkloadStatistics getWorkloadStatistics(String groupName, String userFullname,
                                                    LocalDate startDate, LocalDate endDate) {
        List<WorkloadData> workloadList = workloadRepository.getWorkloadData(
            groupName, userFullname, startDate, endDate);

        WorkloadStatistics statistics = new WorkloadStatistics();
        statistics.setWorkloadList(workloadList);
        statistics.setTotalIssues(workloadList.size());

        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal totalAvgHours = BigDecimal.ZERO;
        int closedCount = 0;

        for (WorkloadData data : workloadList) {
            if (data.getEstimatedHours() != null) {
                totalHours = totalHours.add(data.getEstimatedHours());
            }
            if (data.getAvgHoursPerDay() != null) {
                totalAvgHours = totalAvgHours.add(data.getAvgHoursPerDay());
            }
            if (data.getIsClosed() != null && data.getIsClosed()) {
                closedCount++;
            }
        }

        statistics.setTotalEstimatedHours(totalHours.setScale(2, RoundingMode.HALF_UP));
        statistics.setClosedIssues(closedCount);
        statistics.setOpenIssues(workloadList.size() - closedCount);
        
        if (workloadList.size() > 0) {
            statistics.setAvgHoursPerDay(
                totalAvgHours.divide(BigDecimal.valueOf(workloadList.size()), 2, RoundingMode.HALF_UP));
            statistics.setCompletionRate(
                (double) closedCount / workloadList.size() * 100);
        } else {
            statistics.setAvgHoursPerDay(BigDecimal.ZERO);
            statistics.setCompletionRate(0.0);
        }

        return statistics;
    }

    public List<String> getAllGroups() {
        return workloadRepository.getAllGroups();
    }

    public List<String> getUsersByGroup(String groupName) {
        return workloadRepository.getUsersByGroup(groupName);
    }

    public List<WorkloadAnalysis2D> getWorkloadAnalysis2D(String groupName, String userFullname,
                                                          LocalDate startDate, LocalDate endDate, String timeGranularity) {
        List<WorkloadAnalysis2D> rawData = workloadRepository.getWorkloadAnalysis2D(
            groupName, userFullname, startDate, endDate);
        
        // 根據時間顆粒度選擇處理邏輯
        if ("weekly".equals(timeGranularity)) {
            return processWeeklyAnalysis(rawData, startDate, endDate);
        } else if ("monthly".equals(timeGranularity)) {
            return processMonthlyAnalysis(rawData, startDate, endDate);
        } else {
            return processDailyAnalysis(rawData, startDate, endDate);
        }
    }
    
    private List<WorkloadAnalysis2D> processDailyAnalysis(List<WorkloadAnalysis2D> rawData,
                                                          LocalDate startDate, LocalDate endDate) {
        // 按使用者分組數據
        Map<String, List<WorkloadAnalysis2D>> userDataMap = rawData.stream()
            .collect(Collectors.groupingBy(WorkloadAnalysis2D::getUserFullname));
        
        List<WorkloadAnalysis2D> result = new ArrayList<>();
        
        for (Map.Entry<String, List<WorkloadAnalysis2D>> userEntry : userDataMap.entrySet()) {
            String userName = userEntry.getKey();
            List<WorkloadAnalysis2D> userItems = userEntry.getValue();
            
            // 創建一個Map來存儲該使用者每天的總工時
            Map<LocalDate, BigDecimal> userDailyTotals = new HashMap<>();
            
            // 初始化使用者每日工時為0
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                userDailyTotals.put(current, BigDecimal.ZERO);
                current = current.plusDays(1);
            }
            
            // 按專案分組該使用者的數據
            Map<String, List<WorkloadAnalysis2D>> projectDataMap = userItems.stream()
                .collect(Collectors.groupingBy(WorkloadAnalysis2D::getProjectName));
            
            // 存儲專案每日工時的Map
            Map<String, Map<LocalDate, BigDecimal>> projectDailyTotalsMap = new HashMap<>();
            
            // 處理每個專案
            for (Map.Entry<String, List<WorkloadAnalysis2D>> projectEntry : projectDataMap.entrySet()) {
                String projectName = projectEntry.getKey();
                List<WorkloadAnalysis2D> projectItems = projectEntry.getValue();
                
                // 創建專案每日工時Map
                Map<LocalDate, BigDecimal> projectDailyTotals = new HashMap<>();
                current = startDate;
                while (!current.isAfter(endDate)) {
                    projectDailyTotals.put(current, BigDecimal.ZERO);
                    current = current.plusDays(1);
                }
                
                // 處理專案內的每個議題
                for (WorkloadAnalysis2D item : projectItems) {
                    // 使用議題本身的開始和結束時間（不受查詢區間限制）
                    LocalDate issueStart = item.getStartDate();
                    LocalDate issueEnd = item.getDueDate();
                    
                    // 計算議題本身期間的工作日數（排除週末）
                    long issueWorkDays = 0;
                    LocalDate issueDate = issueStart;
                    while (!issueDate.isAfter(issueEnd)) {
                        if (issueDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                            issueDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            issueWorkDays++;
                        }
                        issueDate = issueDate.plusDays(1);
                    }
                    
                    // 計算議題每日平均工時（基於議題本身的工作日數）
                    BigDecimal issueDailyHours = BigDecimal.ZERO;
                    if (issueWorkDays > 0 && item.getEstimatedHours() != null) {
                        issueDailyHours = item.getEstimatedHours().divide(
                            BigDecimal.valueOf(issueWorkDays), 2, RoundingMode.HALF_UP);
                    }
                    
                    // 為議題創建每日工作量分佈
                    List<WorkloadAnalysis2D.DailyWorkload> issueDailyWorkloads = new ArrayList<>();
                    current = startDate;
                    while (!current.isAfter(endDate)) {
                        WorkloadAnalysis2D.DailyWorkload dailyWorkload = new WorkloadAnalysis2D.DailyWorkload();
                        dailyWorkload.setDate(current);
                        dailyWorkload.setWeekend(current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                                                current.getDayOfWeek() == DayOfWeek.SUNDAY);
                        
                        // 檢查是否在議題的實際時間範圍內且為工作日
                        if (current.isBefore(issueStart) || current.isAfter(issueEnd) || dailyWorkload.isWeekend()) {
                            // 不在議題時間範圍內或是週末
                            dailyWorkload.setHours(BigDecimal.ZERO);
                            dailyWorkload.setStatus("0.0");
                        } else {
                            // 在議題工作日範圍內，分配每日工時
                            dailyWorkload.setHours(issueDailyHours);
                            dailyWorkload.setStatus(issueDailyHours.toString());
                            
                            // 累加到專案每日總計
                            projectDailyTotals.put(current, 
                                projectDailyTotals.get(current).add(issueDailyHours));
                            
                            // 累加到使用者每日總計
                            userDailyTotals.put(current, 
                                userDailyTotals.get(current).add(issueDailyHours));
                        }
                        
                        issueDailyWorkloads.add(dailyWorkload);
                        current = current.plusDays(1);
                    }
                    
                    item.setDailyWorkloads(issueDailyWorkloads);
                }
                
                // 保存專案每日總計
                projectDailyTotalsMap.put(projectName, projectDailyTotals);
            }
            
            // 計算使用者總工時
            BigDecimal userTotalHours = userItems.stream()
                .map(WorkloadAnalysis2D::getEstimatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // 創建使用者層級的匯總數據
            WorkloadAnalysis2D userSummary = new WorkloadAnalysis2D();
            userSummary.setGroupName(userItems.get(0).getGroupName());
            userSummary.setUserFullname(userName);
            userSummary.setProjectName("總計");
            userSummary.setIssueId(-1L);
            userSummary.setIssueSubject("總工時: " + userTotalHours + " 小時");
            userSummary.setEstimatedHours(userTotalHours);
            
            // 為使用者總計創建每日工作量分佈
            List<WorkloadAnalysis2D.DailyWorkload> userDailyWorkloads = new ArrayList<>();
            current = startDate;
            while (!current.isAfter(endDate)) {
                WorkloadAnalysis2D.DailyWorkload dailyWorkload = new WorkloadAnalysis2D.DailyWorkload();
                dailyWorkload.setDate(current);
                dailyWorkload.setWeekend(current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                                        current.getDayOfWeek() == DayOfWeek.SUNDAY);
                
                BigDecimal dayTotal = userDailyTotals.get(current);
                dailyWorkload.setHours(dayTotal);
                dailyWorkload.setStatus(dayTotal.toString());
                
                userDailyWorkloads.add(dailyWorkload);
                current = current.plusDays(1);
            }
            userSummary.setDailyWorkloads(userDailyWorkloads);
            result.add(userSummary);
            
            // 添加專案層級數據
            for (Map.Entry<String, List<WorkloadAnalysis2D>> projectEntry : projectDataMap.entrySet()) {
                String projectName = projectEntry.getKey();
                List<WorkloadAnalysis2D> projectItems = projectEntry.getValue();
                
                // 計算專案總工時
                BigDecimal projectTotalHours = projectItems.stream()
                    .map(WorkloadAnalysis2D::getEstimatedHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                // 創建專案層級的匯總數據
                WorkloadAnalysis2D projectSummary = new WorkloadAnalysis2D();
                projectSummary.setGroupName(userItems.get(0).getGroupName());
                projectSummary.setUserFullname(userName);
                projectSummary.setProjectName(projectName);
                projectSummary.setIssueId(-2L);
                projectSummary.setIssueSubject("專案總工時: " + projectTotalHours + " 小時");
                projectSummary.setEstimatedHours(projectTotalHours);
                
                // 為專案創建每日工作量分佈
                List<WorkloadAnalysis2D.DailyWorkload> projectDailyWorkloads = new ArrayList<>();
                Map<LocalDate, BigDecimal> projectDailyTotals = projectDailyTotalsMap.get(projectName);
                
                current = startDate;
                while (!current.isAfter(endDate)) {
                    WorkloadAnalysis2D.DailyWorkload dailyWorkload = new WorkloadAnalysis2D.DailyWorkload();
                    dailyWorkload.setDate(current);
                    dailyWorkload.setWeekend(current.getDayOfWeek() == DayOfWeek.SATURDAY || 
                                            current.getDayOfWeek() == DayOfWeek.SUNDAY);
                    
                    BigDecimal dayTotal = projectDailyTotals.get(current);
                    dailyWorkload.setHours(dayTotal);
                    dailyWorkload.setStatus(dayTotal.toString());
                    
                    projectDailyWorkloads.add(dailyWorkload);
                    current = current.plusDays(1);
                }
                projectSummary.setDailyWorkloads(projectDailyWorkloads);
                result.add(projectSummary);
                
                // 添加專案下的具體議題（已在上面處理過每日工作量）
                result.addAll(projectItems);
            }
        }
        
        return result;
    }
    
    private List<WorkloadAnalysis2D> processWeeklyAnalysis(List<WorkloadAnalysis2D> rawData,
                                                           LocalDate startDate, LocalDate endDate) {
        // 按使用者分組數據
        Map<String, List<WorkloadAnalysis2D>> userDataMap = rawData.stream()
            .collect(Collectors.groupingBy(WorkloadAnalysis2D::getUserFullname));
        
        List<WorkloadAnalysis2D> result = new ArrayList<>();
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4); // 週一開始的週
        
        for (Map.Entry<String, List<WorkloadAnalysis2D>> userEntry : userDataMap.entrySet()) {
            String userName = userEntry.getKey();
            List<WorkloadAnalysis2D> userItems = userEntry.getValue();
            
            // 創建一個Map來存儲該使用者每週的總工時
            Map<String, BigDecimal> userWeeklyTotals = new HashMap<>();
            
            // 按專案分組該使用者的數據
            Map<String, List<WorkloadAnalysis2D>> projectDataMap = userItems.stream()
                .collect(Collectors.groupingBy(WorkloadAnalysis2D::getProjectName));
            
            // 處理每個專案
            for (Map.Entry<String, List<WorkloadAnalysis2D>> projectEntry : projectDataMap.entrySet()) {
                String projectName = projectEntry.getKey();
                List<WorkloadAnalysis2D> projectItems = projectEntry.getValue();
                
                Map<String, BigDecimal> projectWeeklyTotals = new HashMap<>();
                
                // 處理專案內的每個議題
                for (WorkloadAnalysis2D item : projectItems) {
                    LocalDate issueStart = item.getStartDate();
                    LocalDate issueEnd = item.getDueDate();
                    
                    // 計算議題的工作日數
                    long issueWorkDays = 0;
                    LocalDate issueDate = issueStart;
                    while (!issueDate.isAfter(issueEnd)) {
                        if (issueDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                            issueDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            issueWorkDays++;
                        }
                        issueDate = issueDate.plusDays(1);
                    }
                    
                    BigDecimal issueDailyHours = BigDecimal.ZERO;
                    if (issueWorkDays > 0 && item.getEstimatedHours() != null) {
                        issueDailyHours = item.getEstimatedHours().divide(
                            BigDecimal.valueOf(issueWorkDays), 2, RoundingMode.HALF_UP);
                    }
                    
                    // 按週分配工時
                    List<WorkloadAnalysis2D.PeriodWorkload> periodWorkloads = new ArrayList<>();
                    Map<String, BigDecimal> issueWeeklyTotals = new HashMap<>();
                    
                    // 從使用者輸入的開始日期開始，按週計算
                    LocalDate current = startDate.with(DayOfWeek.MONDAY); // 確保從週一開始
                    while (!current.isAfter(endDate)) {
                        LocalDate weekStart = current;
                        LocalDate weekEnd = current.with(DayOfWeek.SUNDAY);
                        
                        // 限制週末日期在使用者指定範圍內
                        if (weekStart.isBefore(startDate)) weekStart = startDate;
                        if (weekEnd.isAfter(endDate)) weekEnd = endDate;
                        
                        // 產生週次標籤，包含月份資訊
                        int year = weekStart.getYear();
                        int weekOfYear = weekStart.get(weekFields.weekOfWeekBasedYear());
                        String monthInfo = weekStart.format(DateTimeFormatter.ofPattern("MM月"));
                        if (!weekStart.getMonth().equals(weekEnd.getMonth())) {
                            monthInfo += "~" + weekEnd.format(DateTimeFormatter.ofPattern("MM月"));
                        }
                        String weekKey = year + "-W" + String.format("%02d", weekOfYear) + "(" + monthInfo + ")";
                        
                        // 計算該週在議題期間內的工作日數，且在使用者指定範圍內
                        BigDecimal weekHours = BigDecimal.ZERO;
                        LocalDate weekDay = weekStart;
                        while (!weekDay.isAfter(weekEnd)) {
                            if (weekDay.getDayOfWeek() != DayOfWeek.SATURDAY && 
                                weekDay.getDayOfWeek() != DayOfWeek.SUNDAY &&
                                !weekDay.isBefore(issueStart) && !weekDay.isAfter(issueEnd) &&
                                !weekDay.isBefore(startDate) && !weekDay.isAfter(endDate)) {
                                weekHours = weekHours.add(issueDailyHours);
                            }
                            weekDay = weekDay.plusDays(1);
                        }
                        
                        if (weekHours.compareTo(BigDecimal.ZERO) > 0) {
                            issueWeeklyTotals.put(weekKey, weekHours);
                            projectWeeklyTotals.put(weekKey, 
                                projectWeeklyTotals.getOrDefault(weekKey, BigDecimal.ZERO).add(weekHours));
                            userWeeklyTotals.put(weekKey, 
                                userWeeklyTotals.getOrDefault(weekKey, BigDecimal.ZERO).add(weekHours));
                        }
                        
                        current = current.plusWeeks(1);
                    }
                    
                    // 為議題創建週期工作量
                    for (Map.Entry<String, BigDecimal> weekEntry : issueWeeklyTotals.entrySet()) {
                        WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                        periodWorkload.setPeriod(weekEntry.getKey());
                        periodWorkload.setHours(weekEntry.getValue());
                        periodWorkload.setStatus(weekEntry.getValue().toString());
                        periodWorkload.setGranularity("weekly");
                        periodWorkloads.add(periodWorkload);
                    }
                    
                    item.setPeriodWorkloads(periodWorkloads);
                }
                
                // 為專案創建週工作量摘要
                WorkloadAnalysis2D projectSummary = new WorkloadAnalysis2D();
                projectSummary.setGroupName(projectItems.get(0).getGroupName());
                projectSummary.setUserFullname(userName);
                projectSummary.setProjectName(projectName);
                projectSummary.setIssueId(-2L);
                
                BigDecimal projectTotalHours = projectItems.stream()
                    .map(WorkloadAnalysis2D::getEstimatedHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                projectSummary.setIssueSubject("專案總工時: " + projectTotalHours + " 小時");
                projectSummary.setEstimatedHours(projectTotalHours);
                
                List<WorkloadAnalysis2D.PeriodWorkload> projectPeriodWorkloads = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> weekEntry : projectWeeklyTotals.entrySet()) {
                    WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                    periodWorkload.setPeriod(weekEntry.getKey());
                    periodWorkload.setHours(weekEntry.getValue());
                    periodWorkload.setStatus(weekEntry.getValue().toString());
                    periodWorkload.setGranularity("weekly");
                    projectPeriodWorkloads.add(periodWorkload);
                }
                projectSummary.setPeriodWorkloads(projectPeriodWorkloads);
                result.add(projectSummary);
                result.addAll(projectItems);
            }
            
            // 創建使用者層級的匯總數據
            WorkloadAnalysis2D userSummary = new WorkloadAnalysis2D();
            userSummary.setGroupName(userItems.get(0).getGroupName());
            userSummary.setUserFullname(userName);
            userSummary.setProjectName("總計");
            userSummary.setIssueId(-1L);
            
            BigDecimal userTotalHours = userItems.stream()
                .map(WorkloadAnalysis2D::getEstimatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            userSummary.setIssueSubject("總工時: " + userTotalHours + " 小時");
            userSummary.setEstimatedHours(userTotalHours);
            
            List<WorkloadAnalysis2D.PeriodWorkload> userPeriodWorkloads = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> weekEntry : userWeeklyTotals.entrySet()) {
                WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                periodWorkload.setPeriod(weekEntry.getKey());
                periodWorkload.setHours(weekEntry.getValue());
                periodWorkload.setStatus(weekEntry.getValue().toString());
                periodWorkload.setGranularity("weekly");
                userPeriodWorkloads.add(periodWorkload);
            }
            userSummary.setPeriodWorkloads(userPeriodWorkloads);
            result.add(userSummary);
        }
        
        return result;
    }
    
    private List<WorkloadAnalysis2D> processMonthlyAnalysis(List<WorkloadAnalysis2D> rawData,
                                                            LocalDate startDate, LocalDate endDate) {
        // 按使用者分組數據
        Map<String, List<WorkloadAnalysis2D>> userDataMap = rawData.stream()
            .collect(Collectors.groupingBy(WorkloadAnalysis2D::getUserFullname));
        
        List<WorkloadAnalysis2D> result = new ArrayList<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        for (Map.Entry<String, List<WorkloadAnalysis2D>> userEntry : userDataMap.entrySet()) {
            String userName = userEntry.getKey();
            List<WorkloadAnalysis2D> userItems = userEntry.getValue();
            
            // 創建一個Map來存儲該使用者每月的總工時
            Map<String, BigDecimal> userMonthlyTotals = new HashMap<>();
            
            // 按專案分組該使用者的數據
            Map<String, List<WorkloadAnalysis2D>> projectDataMap = userItems.stream()
                .collect(Collectors.groupingBy(WorkloadAnalysis2D::getProjectName));
            
            // 處理每個專案
            for (Map.Entry<String, List<WorkloadAnalysis2D>> projectEntry : projectDataMap.entrySet()) {
                String projectName = projectEntry.getKey();
                List<WorkloadAnalysis2D> projectItems = projectEntry.getValue();
                
                Map<String, BigDecimal> projectMonthlyTotals = new HashMap<>();
                
                // 處理專案內的每個議題
                for (WorkloadAnalysis2D item : projectItems) {
                    LocalDate issueStart = item.getStartDate();
                    LocalDate issueEnd = item.getDueDate();
                    
                    // 計算議題的工作日數
                    long issueWorkDays = 0;
                    LocalDate issueDate = issueStart;
                    while (!issueDate.isAfter(issueEnd)) {
                        if (issueDate.getDayOfWeek() != DayOfWeek.SATURDAY && 
                            issueDate.getDayOfWeek() != DayOfWeek.SUNDAY) {
                            issueWorkDays++;
                        }
                        issueDate = issueDate.plusDays(1);
                    }
                    
                    BigDecimal issueDailyHours = BigDecimal.ZERO;
                    if (issueWorkDays > 0 && item.getEstimatedHours() != null) {
                        issueDailyHours = item.getEstimatedHours().divide(
                            BigDecimal.valueOf(issueWorkDays), 2, RoundingMode.HALF_UP);
                    }
                    
                    // 按月分配工時
                    List<WorkloadAnalysis2D.PeriodWorkload> periodWorkloads = new ArrayList<>();
                    Map<String, BigDecimal> issueMonthlyTotals = new HashMap<>();
                    
                    // 從使用者輸入的開始日期所在月份開始
                    LocalDate current = startDate.with(TemporalAdjusters.firstDayOfMonth());
                    while (!current.isAfter(endDate)) {
                        String monthKey = current.format(monthFormatter);
                        LocalDate monthStart = current.with(TemporalAdjusters.firstDayOfMonth());
                        LocalDate monthEnd = current.with(TemporalAdjusters.lastDayOfMonth());
                        
                        // 限制月份範圍在使用者指定範圍內
                        if (monthStart.isBefore(startDate)) monthStart = startDate;
                        if (monthEnd.isAfter(endDate)) monthEnd = endDate;
                        
                        // 計算該月在議題期間內的工作日數
                        BigDecimal monthHours = BigDecimal.ZERO;
                        LocalDate monthDay = monthStart;
                        while (!monthDay.isAfter(monthEnd)) {
                            if (monthDay.getDayOfWeek() != DayOfWeek.SATURDAY && 
                                monthDay.getDayOfWeek() != DayOfWeek.SUNDAY &&
                                !monthDay.isBefore(issueStart) && !monthDay.isAfter(issueEnd) &&
                                !monthDay.isBefore(startDate) && !monthDay.isAfter(endDate)) {
                                monthHours = monthHours.add(issueDailyHours);
                            }
                            monthDay = monthDay.plusDays(1);
                        }
                        
                        if (monthHours.compareTo(BigDecimal.ZERO) > 0) {
                            issueMonthlyTotals.put(monthKey, monthHours);
                            projectMonthlyTotals.put(monthKey, 
                                projectMonthlyTotals.getOrDefault(monthKey, BigDecimal.ZERO).add(monthHours));
                            userMonthlyTotals.put(monthKey, 
                                userMonthlyTotals.getOrDefault(monthKey, BigDecimal.ZERO).add(monthHours));
                        }
                        
                        current = current.plusMonths(1);
                    }
                    
                    // 為議題創建週期工作量
                    for (Map.Entry<String, BigDecimal> monthEntry : issueMonthlyTotals.entrySet()) {
                        WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                        periodWorkload.setPeriod(monthEntry.getKey());
                        periodWorkload.setHours(monthEntry.getValue());
                        periodWorkload.setStatus(monthEntry.getValue().toString());
                        periodWorkload.setGranularity("monthly");
                        periodWorkloads.add(periodWorkload);
                    }
                    
                    item.setPeriodWorkloads(periodWorkloads);
                }
                
                // 為專案創建月工作量摘要
                WorkloadAnalysis2D projectSummary = new WorkloadAnalysis2D();
                projectSummary.setGroupName(projectItems.get(0).getGroupName());
                projectSummary.setUserFullname(userName);
                projectSummary.setProjectName(projectName);
                projectSummary.setIssueId(-2L);
                
                BigDecimal projectTotalHours = projectItems.stream()
                    .map(WorkloadAnalysis2D::getEstimatedHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                projectSummary.setIssueSubject("專案總工時: " + projectTotalHours + " 小時");
                projectSummary.setEstimatedHours(projectTotalHours);
                
                List<WorkloadAnalysis2D.PeriodWorkload> projectPeriodWorkloads = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> monthEntry : projectMonthlyTotals.entrySet()) {
                    WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                    periodWorkload.setPeriod(monthEntry.getKey());
                    periodWorkload.setHours(monthEntry.getValue());
                    periodWorkload.setStatus(monthEntry.getValue().toString());
                    periodWorkload.setGranularity("monthly");
                    projectPeriodWorkloads.add(periodWorkload);
                }
                projectSummary.setPeriodWorkloads(projectPeriodWorkloads);
                result.add(projectSummary);
                result.addAll(projectItems);
            }
            
            // 創建使用者層級的匯總數據
            WorkloadAnalysis2D userSummary = new WorkloadAnalysis2D();
            userSummary.setGroupName(userItems.get(0).getGroupName());
            userSummary.setUserFullname(userName);
            userSummary.setProjectName("總計");
            userSummary.setIssueId(-1L);
            
            BigDecimal userTotalHours = userItems.stream()
                .map(WorkloadAnalysis2D::getEstimatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            userSummary.setIssueSubject("總工時: " + userTotalHours + " 小時");
            userSummary.setEstimatedHours(userTotalHours);
            
            List<WorkloadAnalysis2D.PeriodWorkload> userPeriodWorkloads = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> monthEntry : userMonthlyTotals.entrySet()) {
                WorkloadAnalysis2D.PeriodWorkload periodWorkload = new WorkloadAnalysis2D.PeriodWorkload();
                periodWorkload.setPeriod(monthEntry.getKey());
                periodWorkload.setHours(monthEntry.getValue());
                periodWorkload.setStatus(monthEntry.getValue().toString());
                periodWorkload.setGranularity("monthly");
                userPeriodWorkloads.add(periodWorkload);
            }
            userSummary.setPeriodWorkloads(userPeriodWorkloads);
            result.add(userSummary);
        }
        
        return result;
    }
}
