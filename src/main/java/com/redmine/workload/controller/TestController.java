package com.redmine.workload.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/groups")
    public String testGroups() {
        StringBuilder result = new StringBuilder();
        result.append("<h2>測試群組查詢</h2>");
        
        try {
            // 測試查詢所有群組
            String sql = "SELECT DISTINCT g.lastname FROM users g WHERE g.type = 'Group' ORDER BY g.lastname";
            List<String> groups = jdbcTemplate.queryForList(sql, String.class);
            
            result.append("<p>找到 ").append(groups.size()).append(" 個群組:</p>");
            result.append("<ul>");
            for (String group : groups) {
                result.append("<li>").append(group).append("</li>");
            }
            result.append("</ul>");
            
        } catch (Exception e) {
            result.append("<p style='color:red'>錯誤: ").append(e.getMessage()).append("</p>");
            e.printStackTrace();
        }
        
        return result.toString();
    }

    @GetMapping("/users")
    public String testUsers() {
        StringBuilder result = new StringBuilder();
        result.append("<h2>測試使用者查詢</h2>");
        
        try {
            // 測試查詢產品開發部的使用者
            String sql = "SELECT DISTINCT CONCAT(u.lastname, u.firstname) AS user_fullname " +
                        "FROM users u " +
                        "JOIN groups_users gu ON gu.user_id = u.id " +
                        "JOIN users g ON g.id = gu.group_id " +
                        "WHERE g.lastname = '產品開發部' " +
                        "ORDER BY user_fullname";
            
            List<String> users = jdbcTemplate.queryForList(sql, String.class);
            
            result.append("<p>產品開發部找到 ").append(users.size()).append(" 個使用者:</p>");
            result.append("<ul>");
            for (String user : users) {
                result.append("<li>").append(user).append("</li>");
            }
            result.append("</ul>");
            
        } catch (Exception e) {
            result.append("<p style='color:red'>錯誤: ").append(e.getMessage()).append("</p>");
            e.printStackTrace();
        }
        
        return result.toString();
    }

    @GetMapping("/issues")
    public String testIssues() {
        StringBuilder result = new StringBuilder();
        result.append("<h2>測試議題查詢</h2>");
        
        try {
            // 測試查詢產品開發部的所有議題統計
            String sql = "SELECT " +
                "    g.lastname AS group_name, " +
                "    CONCAT(u.lastname, u.firstname) AS user_fullname, " +
                "    MIN(i.start_date) AS earliest_start, " +
                "    MAX(i.due_date) AS latest_due, " +
                "    COUNT(*) AS issue_count " +
                "FROM issues i " +
                "JOIN projects p ON p.id = i.project_id " +
                "JOIN users u ON u.id = i.assigned_to_id " +
                "JOIN groups_users gu ON gu.user_id = u.id " +
                "JOIN users g ON g.id = gu.group_id " +
                "JOIN issue_statuses s ON s.id = i.status_id " +
                "WHERE " +
                "    g.lastname = '產品開發部' " +
                "    AND i.start_date IS NOT NULL " +
                "    AND i.due_date IS NOT NULL " +
                "    AND i.estimated_hours IS NOT NULL " +
                "GROUP BY g.lastname, user_fullname " +
                "ORDER BY user_fullname";
            
            List<Map<String, Object>> issues = jdbcTemplate.queryForList(sql);
            
            result.append("<p>產品開發部找到 ").append(issues.size()).append(" 位使用者有議題資料:</p>");
            result.append("<table border='1' style='border-collapse:collapse; font-size:12px'>");
            result.append("<tr><th>部門</th><th>使用者</th><th>最早開始日</th><th>最晚到期日</th><th>議題數</th></tr>");
            for (Map<String, Object> row : issues) {
                result.append("<tr>");
                result.append("<td>").append(row.get("group_name")).append("</td>");
                result.append("<td>").append(row.get("user_fullname")).append("</td>");
                result.append("<td>").append(row.get("earliest_start")).append("</td>");
                result.append("<td>").append(row.get("latest_due")).append("</td>");
                result.append("<td>").append(row.get("issue_count")).append("</td>");
                result.append("</tr>");
            }
            result.append("</table>");
            
        } catch (Exception e) {
            result.append("<p style='color:red'>錯誤: ").append(e.getMessage()).append("</p>");
            e.printStackTrace();
        }
        
        return result.toString();
    }

    @GetMapping("/connection")
    public String testConnection() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "<h2>資料庫連線測試</h2><p style='color:green'>✓ 連線成功! 測試查詢結果: " + result + "</p>";
        } catch (Exception e) {
            return "<h2>資料庫連線測試</h2><p style='color:red'>✗ 連線失敗: " + e.getMessage() + "</p>";
        }
    }
}
