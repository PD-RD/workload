package com.redmine.workload.repository;

import com.redmine.workload.model.WorkloadData;
import com.redmine.workload.model.WorkloadAnalysis2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class WorkloadRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String WORKLOAD_QUERY = 
        "SELECT " +
        "    g.lastname AS group_name, " +
        "    CONCAT(u.lastname, u.firstname) AS user_fullname, " +
        "    u.login AS user_login, " +
        "    p.name AS project_name, " +
        "    i.id AS issue_id, " +
        "    i.subject AS issue_subject, " +
        "    i.start_date, " +
        "    i.due_date, " +
        "    i.estimated_hours, " +
        "    ROUND(i.estimated_hours / NULLIF(DATEDIFF(i.due_date, i.start_date) + 1, 0), 2) AS avg_hours_per_day, " +
        "    s.name AS status_name, " +
        "    s.is_closed AS is_closed, " +
        "    i.closed_on " +
        "FROM issues i " +
        "JOIN projects p ON p.id = i.project_id " +
        "JOIN users u ON u.id = i.assigned_to_id " +
        "JOIN groups_users gu ON gu.user_id = u.id " +
        "JOIN users g ON g.id = gu.group_id " +
        "JOIN issue_statuses s ON s.id = i.status_id " +
        "WHERE " +
        "    g.lastname = ? " +
        "    AND CONCAT(u.lastname, u.firstname) = ? " +
        "    AND u.status = 1 " +
        "    AND g.status = 1 " +
        "    AND i.start_date IS NOT NULL " +
        "    AND i.due_date IS NOT NULL " +
        "    AND i.estimated_hours IS NOT NULL " +
        "    AND i.start_date <= ? " +
        "    AND i.due_date >= ? " +
        "ORDER BY g.lastname, user_fullname, p.name, is_closed DESC, i.id ASC";

    private static final String GET_ALL_GROUPS = 
        "SELECT DISTINCT g.lastname " +
        "FROM users g " +
        "WHERE g.type = 'Group' " +
        "ORDER BY g.lastname";

    private static final String GET_USERS_BY_GROUP = 
        "SELECT DISTINCT CONCAT(u.lastname, u.firstname) AS user_fullname " +
        "FROM users u " +
        "JOIN groups_users gu ON gu.user_id = u.id " +
        "JOIN users g ON g.id = gu.group_id " +
        "WHERE g.lastname = ?  and u.status = 1 and g.status = 1 "+
        "ORDER BY user_fullname";

    public List<WorkloadData> getWorkloadData(String groupName, String userFullname, 
                                              LocalDate startDate, LocalDate endDate) {
        System.out.println("=== Executing Workload Query ===");
        System.out.println("Group Name: " + groupName);
        System.out.println("User Fullname: " + userFullname);
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        
        // 如果沒有選擇使用者，查詢整個群組的資料
        if (userFullname == null || userFullname.trim().isEmpty()) {
            return getWorkloadDataByGroup(groupName, startDate, endDate);
        } else {
            System.out.println("Query: " + WORKLOAD_QUERY);
            System.out.println("Parameters: [" + groupName + ", " + userFullname + ", " + startDate + ", " + endDate + "]");
            
            // 生成完整的 SQL 指令用於除錯
            String finalSql = WORKLOAD_QUERY;
            finalSql = finalSql.replaceFirst("\\?", "'" + groupName + "'");
            finalSql = finalSql.replaceFirst("\\?", "'" + userFullname + "'");
            finalSql = finalSql.replaceFirst("\\?", "'" + endDate + "'");
            finalSql = finalSql.replaceFirst("\\?", "'" + startDate + "'");
            System.out.println("=== Final SQL Command ===");
            System.out.println(finalSql);
            System.out.println("========================");
            
            List<WorkloadData> result = jdbcTemplate.query(WORKLOAD_QUERY, 
                new WorkloadRowMapper(), 
                groupName, userFullname, endDate,startDate);
            System.out.println("Result count: " + result.size());
            return result;
        }
    }

    private List<WorkloadData> getWorkloadDataByGroup(String groupName, LocalDate startDate, LocalDate endDate) {
        String GROUP_QUERY = 
            "SELECT " +
            "    g.lastname AS group_name, " +
            "    CONCAT(u.lastname, u.firstname) AS user_fullname, " +
            "    u.login AS user_login, " +
            "    p.name AS project_name, " +
            "    i.id AS issue_id, " +
            "    i.subject AS issue_subject, " +
            "    i.start_date, " +
            "    i.due_date, " +
            "    i.estimated_hours, " +
            "    ROUND(i.estimated_hours / NULLIF(DATEDIFF(i.due_date, i.start_date) + 1, 0), 2) AS avg_hours_per_day, " +
            "    s.name AS status_name, " +
            "    s.is_closed AS is_closed, " +
            "    i.closed_on " +
            "FROM issues i " +
            "JOIN projects p ON p.id = i.project_id " +
            "JOIN users u ON u.id = i.assigned_to_id " +
            "JOIN groups_users gu ON gu.user_id = u.id " +
            "JOIN users g ON g.id = gu.group_id " +
            "JOIN issue_statuses s ON s.id = i.status_id " +
            "WHERE " +
            "    g.lastname = ? " +
            "    AND u.status = 1 " +
            "    AND g.status = 1 " +
            "    AND i.start_date IS NOT NULL " +
            "    AND i.due_date IS NOT NULL " +
            "    AND i.estimated_hours IS NOT NULL " +
            "    AND i.start_date <= ? " +
            "    AND i.due_date >= ? " +
            "ORDER BY g.lastname, user_fullname, p.name, is_closed DESC, i.id ASC";
        
        System.out.println("Group Query: " + GROUP_QUERY);
        System.out.println("Group Parameters: [" + groupName + ", " + startDate + ", " + endDate + "]");
        
        // 生成完整的群組 SQL 指令用於除錯
        String finalGroupSql = GROUP_QUERY
            .replaceFirst("\\?", "'" + groupName + "'")
            .replaceFirst("\\?", "'" +endDate  + "'")
            .replaceFirst("\\?", "'" + startDate + "'");
        System.out.println("=== Final Group SQL Command ===");
        System.out.println(finalGroupSql);
        System.out.println("==============================");
        
        List<WorkloadData> result = jdbcTemplate.query(GROUP_QUERY, 
            new WorkloadRowMapper(), 
            groupName, endDate,startDate);
        System.out.println("Group result count: " + result.size());
        return result;
    }

    public List<String> getAllGroups() {
        System.out.println("=== Getting All Groups ===");
        List<String> groups = jdbcTemplate.queryForList(GET_ALL_GROUPS, String.class);
        System.out.println("Found " + groups.size() + " groups: " + groups);
        return groups;
    }

    public List<String> getUsersByGroup(String groupName) {
        System.out.println("=== Getting Users for Group: " + groupName + " ===");
        List<String> users = jdbcTemplate.queryForList(GET_USERS_BY_GROUP, String.class, groupName);
        System.out.println("Found " + users.size() + " users: " + users);
        return users;
    }

    private static class WorkloadRowMapper implements RowMapper<WorkloadData> {
        @Override
        public WorkloadData mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkloadData data = new WorkloadData();
            data.setGroupName(rs.getString("group_name"));
            data.setUserFullname(rs.getString("user_fullname"));
            data.setUserLogin(rs.getString("user_login"));
            data.setProjectName(rs.getString("project_name"));
            data.setIssueId(rs.getLong("issue_id"));
            data.setIssueSubject(rs.getString("issue_subject"));
            
            if (rs.getDate("start_date") != null) {
                data.setStartDate(rs.getDate("start_date").toLocalDate());
            }
            if (rs.getDate("due_date") != null) {
                data.setDueDate(rs.getDate("due_date").toLocalDate());
            }
            
            data.setEstimatedHours(rs.getBigDecimal("estimated_hours"));
            data.setAvgHoursPerDay(rs.getBigDecimal("avg_hours_per_day"));
            data.setStatusName(rs.getString("status_name"));
            data.setIsClosed(rs.getBoolean("is_closed"));
            
            if (rs.getDate("closed_on") != null) {
                data.setClosedOn(rs.getDate("closed_on").toLocalDate());
            }
            
            return data;
        }
    }

    // 新增2D分析查詢方法
    public List<WorkloadAnalysis2D> getWorkloadAnalysis2D(String groupName, String userFullname, 
                                                          LocalDate startDate, LocalDate endDate) {
        System.out.println("=== Executing 2D Analysis Query ===");
        System.out.println("Group Name: " + groupName);
        System.out.println("User Fullname: " + userFullname);
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);
        
        String query2D = 
            "SELECT " +
            "    g.lastname AS group_name, " +
            "    CONCAT(u.lastname, u.firstname) AS user_fullname, " +
            "    p.name AS project_name, " +
            "    i.id AS issue_id, " +
            "    i.subject AS issue_subject, " +
            "    i.start_date, " +
            "    i.due_date, " +
            "    i.estimated_hours, " +
            "    s.is_closed " +
            "FROM issues i " +
            "JOIN projects p ON p.id = i.project_id " +
            "JOIN users u ON u.id = i.assigned_to_id " +
            "JOIN groups_users gu ON gu.user_id = u.id " +
            "JOIN users g ON g.id = gu.group_id " +
            "JOIN issue_statuses s ON s.id = i.status_id " +
            "WHERE " +
            "    g.lastname = ? " +
            "    AND (? IS NULL OR ? = '' OR CONCAT(u.lastname, u.firstname) = ?) " +
            "    AND u.status = 1 " +
            "    AND g.status = 1 " +
            "    AND i.start_date IS NOT NULL " +
            "    AND i.due_date IS NOT NULL " +
            "    AND i.estimated_hours IS NOT NULL " +
            "    AND (i.start_date <= ? and i.due_date >= ?) " +
            "ORDER BY g.lastname, user_fullname, p.name, i.id ASC";
        
        // 生成完整的 SQL 指令用於除錯
        String finalSql = query2D;
        finalSql = finalSql.replaceFirst("\\?", "'" + groupName + "'");
        finalSql = finalSql.replaceFirst("\\?", userFullname == null ? "NULL" : "'" + userFullname + "'");
        finalSql = finalSql.replaceFirst("\\?", userFullname == null ? "''" : "'" + userFullname + "'");
        finalSql = finalSql.replaceFirst("\\?", userFullname == null ? "NULL" : "'" + userFullname + "'");
        finalSql = finalSql.replaceFirst("\\?", "'" + endDate + "'");
        finalSql = finalSql.replaceFirst("\\?", "'" + startDate + "'");
        System.out.println("=== Final 2D Analysis SQL Command ===");
        System.out.println(finalSql);
        System.out.println("=====================================");
        
        List<WorkloadAnalysis2D> result = jdbcTemplate.query(query2D, 
            new WorkloadAnalysis2DRowMapper(), 
            groupName, userFullname, userFullname, userFullname, endDate, startDate);
        System.out.println("2D Analysis Result count: " + result.size());
        return result;
    }

    private static class WorkloadAnalysis2DRowMapper implements RowMapper<WorkloadAnalysis2D> {
        @Override
        public WorkloadAnalysis2D mapRow(ResultSet rs, int rowNum) throws SQLException {
            WorkloadAnalysis2D data = new WorkloadAnalysis2D();
            data.setGroupName(rs.getString("group_name"));
            data.setUserFullname(rs.getString("user_fullname"));
            data.setProjectName(rs.getString("project_name"));
            data.setIssueId(rs.getLong("issue_id"));
            data.setIssueSubject(rs.getString("issue_subject"));
            data.setStartDate(rs.getDate("start_date").toLocalDate());
            data.setDueDate(rs.getDate("due_date").toLocalDate());
            data.setEstimatedHours(rs.getBigDecimal("estimated_hours"));
            
            return data;
        }
    }
}
