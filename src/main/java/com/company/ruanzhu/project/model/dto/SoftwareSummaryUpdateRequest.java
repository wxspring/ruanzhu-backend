package com.company.ruanzhu.project.model.dto;

import lombok.Data;

@Data
public class SoftwareSummaryUpdateRequest {
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
    private String systemOverview;
    private String functionalFeatures;
    private String functionMenu;
}
