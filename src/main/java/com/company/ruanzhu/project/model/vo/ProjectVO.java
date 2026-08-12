package com.company.ruanzhu.project.model.vo;

import com.company.ruanzhu.project.enums.ProjectStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectVO {
    private Long id;
    private String name;
    private String customerName;
    private ProjectStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SoftwareSummaryVO softwareSummary;
}
