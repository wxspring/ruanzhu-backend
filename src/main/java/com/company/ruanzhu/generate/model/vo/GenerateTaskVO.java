package com.company.ruanzhu.generate.model.vo;

import com.company.ruanzhu.generate.enums.TaskStatus;
import com.company.ruanzhu.generate.enums.TaskType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GenerateTaskVO {
    private Long id;
    private Long projectId;
    private TaskType taskType;
    private TaskStatus status;
    private Integer progress;
    private String errorMessage;
    private String resultPath;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
