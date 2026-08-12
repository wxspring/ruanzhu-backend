package com.company.ruanzhu.project.model.vo;

import lombok.Data;

@Data
public class SoftwareSummaryVO {
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
    private String purpose;
    private String targetDomain;
    private String mainFunctions;
    private String techFeatures;
}
