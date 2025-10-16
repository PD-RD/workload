package com.redmine.workload.controller;

import com.redmine.workload.model.WorkloadStatistics;
import com.redmine.workload.model.WorkloadAnalysis2D;
import com.redmine.workload.service.WorkloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WorkloadController {

    @Autowired
    private WorkloadService workloadService;

    @GetMapping("/")
    public String index(Model model) {
        List<String> groups = workloadService.getAllGroups();
        model.addAttribute("groups", groups);
        
        // 設定預設日期
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);
        
        model.addAttribute("defaultStartDate", today.toString());
        model.addAttribute("defaultEndDate", endOfYear.toString());
        
        return "index";
    }

    @GetMapping("/api/users/{groupName}")
    @ResponseBody
    public List<String> getUsersByGroup(@PathVariable String groupName) {
        return workloadService.getUsersByGroup(groupName);
    }

    @PostMapping("/workload")
    public String getWorkload(
            @RequestParam("groupName") String groupName,
            @RequestParam(value = "userFullname", required = false) String[] userFullnames,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        System.out.println("========================================");
        System.out.println("POST /workload - Request Parameters:");
        System.out.println("Group Name: [" + groupName + "]");
        System.out.println("User Fullnames: " + (userFullnames != null ? Arrays.toString(userFullnames) : "[]"));
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        
        // 處理空的使用者選擇
        List<String> selectedUsers = new ArrayList<>();
        if (userFullnames != null) {
            for (String user : userFullnames) {
                if (user != null && !user.trim().isEmpty()) {
                    selectedUsers.add(user.trim());
                }
            }
        }
        
        // 當選擇使用者時（包含單選和多選），都按使用者分組顯示；只有查詢整個群組時才不分組
        boolean isGroupQuery = !selectedUsers.isEmpty();
        System.out.println("Is Group Query: " + isGroupQuery);
        System.out.println("Selected Users: " + selectedUsers);
        System.out.println("========================================");
        
        // 使用新的多使用者查詢方法
        WorkloadStatistics statistics = workloadService.getWorkloadStatisticsForMultipleUsers(
            groupName, selectedUsers, startDate, endDate);
        
        System.out.println("Statistics - Total Issues: " + statistics.getTotalIssues());
        System.out.println("Statistics - Total Hours: " + statistics.getTotalEstimatedHours());
        
        List<String> groups = workloadService.getAllGroups();
        List<String> users = workloadService.getUsersByGroup(groupName);
        
        model.addAttribute("statistics", statistics);
        model.addAttribute("groups", groups);
        model.addAttribute("users", users);
        model.addAttribute("selectedGroup", groupName);
        model.addAttribute("selectedUsers", selectedUsers);
        model.addAttribute("selectedStartDate", startDate.toString());
        model.addAttribute("selectedEndDate", endDate.toString());
        model.addAttribute("isGroupQuery", isGroupQuery);
        
        return "index";
    }

    @GetMapping("/workload2d")
    public String getWorkload2DPage(Model model) {
        // 提供初始空白頁面，包含篩選表單
        List<String> groups = workloadService.getAllGroups();
        model.addAttribute("groups", groups);
        
        // 設定預設日期
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);
        
        model.addAttribute("defaultStartDate", today.toString());
        model.addAttribute("defaultEndDate", endOfYear.toString());
        model.addAttribute("selectedStartDate", today.toString());
        model.addAttribute("selectedEndDate", endOfYear.toString());
        
        // 初始狀態：沒有分析數據
        model.addAttribute("analysis2D", null);
        model.addAttribute("selectedGroup", "");
        model.addAttribute("selectedUsers", new ArrayList<>());
        model.addAttribute("users", new ArrayList<>());
        model.addAttribute("timeGranularity", "daily");
        
        return "workload2d";
    }

    @PostMapping("/workload2d")
    public String getWorkload2D(
            @RequestParam("groupName") String groupName,
            @RequestParam(value = "userFullname", required = false) String[] userFullnames,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        // 時間顆粒度固定為每日
        String timeGranularity = "daily";
        
        System.out.println("========================================");
        System.out.println("POST /workload2d - Request Parameters:");
        System.out.println("Group Name: " + groupName);
        System.out.println("User Fullnames: " + (userFullnames != null ? Arrays.toString(userFullnames) : "[]"));
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        System.out.println("Time Granularity: " + timeGranularity + " (固定值)");
        
        // 處理空的使用者選擇
        List<String> selectedUsers = new ArrayList<>();
        if (userFullnames != null) {
            for (String user : userFullnames) {
                if (user != null && !user.trim().isEmpty()) {
                    selectedUsers.add(user.trim());
                }
            }
        }
        
        // 當選擇使用者時（包含單選和多選），都按使用者分組顯示；只有查詢整個群組時才不分組
        boolean isGroupQuery = !selectedUsers.isEmpty();
        System.out.println("Is Group Query: " + isGroupQuery);
        System.out.println("Selected Users: " + selectedUsers);
        System.out.println("========================================");
        
        // 使用新的多使用者查詢方法
        List<WorkloadAnalysis2D> analysis2D = workloadService.getWorkloadAnalysis2DForMultipleUsers(
            groupName, selectedUsers, startDate, endDate, timeGranularity);
        
        System.out.println("2D Analysis - Total Items: " + analysis2D.size());
        
        List<String> groups = workloadService.getAllGroups();
        List<String> users = workloadService.getUsersByGroup(groupName);
        
        // 計算日期範圍的天數
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        // 計算週次範圍 - 使用更簡單的方法
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault());
        
        // 找出開始日期和結束日期所在的週的週一
        LocalDate startWeekMonday = startDate.with(DayOfWeek.MONDAY);
        LocalDate endWeekMonday = endDate.with(DayOfWeek.MONDAY);
        
        // 計算實際的週數
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(startWeekMonday, endWeekMonday) + 1;
        int weekCount = (int) weeksBetween;
        
        // 產生週次列表
        java.util.List<Integer> weekNumbers = new java.util.ArrayList<>();
        LocalDate currentWeek = startWeekMonday;
        for (int i = 0; i < weekCount; i++) {
            int weekNumber = currentWeek.get(weekFields.weekOfWeekBasedYear());
            weekNumbers.add(weekNumber);
            currentWeek = currentWeek.plusWeeks(1);
        }
        
        // 取得開始週次用於顯示
        int startWeek = startDate.get(weekFields.weekOfWeekBasedYear());
        int startYear = startDate.get(weekFields.weekBasedYear());
        
        // 計算月份範圍
        int monthCount = (endDate.getYear() - startDate.getYear()) * 12 + 
                        endDate.getMonthValue() - startDate.getMonthValue() + 1;
        
        model.addAttribute("analysis2D", analysis2D);
        model.addAttribute("groups", groups);
        model.addAttribute("users", users);
        model.addAttribute("selectedGroup", groupName);
        model.addAttribute("selectedUsers", selectedUsers);
        model.addAttribute("selectedStartDate", startDate.toString());
        model.addAttribute("selectedEndDate", endDate.toString());
        model.addAttribute("timeGranularity", timeGranularity);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("daysBetween", daysBetween);
        model.addAttribute("startWeek", startWeek);
        model.addAttribute("weekCount", weekCount);
        model.addAttribute("weekNumbers", weekNumbers);
        model.addAttribute("monthCount", monthCount);
        model.addAttribute("startYear", startYear);
        model.addAttribute("isGroupQuery", isGroupQuery);
        
        return "workload2d";
    }
}
