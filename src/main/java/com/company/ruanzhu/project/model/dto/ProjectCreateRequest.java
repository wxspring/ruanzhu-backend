package com.company.ruanzhu.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称最长100个字符")
    private String name;

    @Size(max = 100, message = "客户名称最长100个字符")
    private String customerName;

    // SoftwareSummary fields
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
