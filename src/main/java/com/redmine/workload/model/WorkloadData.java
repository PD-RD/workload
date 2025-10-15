package com.redmine.workload.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadData {
    private String groupName;
    private String userFullname;
    private String userLogin;
    private String projectName;
    private Long issueId;
    private String issueSubject;
    private LocalDate startDate;
    private LocalDate dueDate;
    private BigDecimal estimatedHours;
    private BigDecimal avgHoursPerDay;
    private String statusName;
    private Boolean isClosed;
    private LocalDate closedOn;
}
