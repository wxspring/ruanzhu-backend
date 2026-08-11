package com.company.ruanzhu.generate.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.generate.enums.TaskStatus;
import com.company.ruanzhu.generate.enums.TaskType;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("generate_task")
public class GenerateTask {
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
