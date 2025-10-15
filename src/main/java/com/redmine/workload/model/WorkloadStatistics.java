package com.redmine.workload.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkloadStatistics {
    private List<WorkloadData> workloadList;
    private BigDecimal totalEstimatedHours;
    private BigDecimal avgHoursPerDay;
    private int totalIssues;
    private int closedIssues;
    private int openIssues;
    private double completionRate;
}
