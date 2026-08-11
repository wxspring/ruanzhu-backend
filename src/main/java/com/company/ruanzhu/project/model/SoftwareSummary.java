package com.company.ruanzhu.project.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("software_summary")
public class SoftwareSummary {
    private Long id;
    private Long projectId;
    private String version;
    private String category;
    private String devHardware;
    private String runHardware;
    private String devOs;
    private String devTools;
    private String runPlatform;
    private String runSupport;
    private String language;
    private Integer codeLines;
    private String purpose;
    private String targetDomain;
    private String mainFunctions;
    private String techFeatures;
    private String techFeatureOptions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
