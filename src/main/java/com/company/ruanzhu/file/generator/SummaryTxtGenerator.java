package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Generates software summary TXT file.
 */
@Slf4j
@Component
public class SummaryTxtGenerator {

    private final ProjectService projectService;
    private final Long projectId;

    public SummaryTxtGenerator(ProjectService projectService, Long projectId) {
        this.projectService = projectService;
        this.projectId = projectId;
    }

    public byte[] generateTxt() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();

        StringBuilder sb = new StringBuilder();

        sb.append("软著名称：").append(project.getName()).append("\n");
        sb.append("版本号：").append(getOrDefault(summary != null ? summary.getVersion() : null, "V1.0")).append("\n");
        sb.append("软件分类：").append(getOrDefault(summary != null ? summary.getCategory() : null, "应用软件")).append("\n");
        sb.append("开发的硬件环境：").append(getOrDefault(summary != null ? summary.getDevHardware() : null, "Intel Core i5 及以上，8GB 内存")).append("\n");
        sb.append("运行的硬件环境：").append(getOrDefault(summary != null ? summary.getRunHardware() : null, "Intel Core i3 及以上，4GB 内存")).append("\n");
        sb.append("开发该软件的操作系统：").append(getOrDefault(summary != null ? summary.getDevOs() : null, "Windows 10/11")).append("\n");
        sb.append("软件开发环境/开发工具：").append(getOrDefault(summary != null ? summary.getDevTools() : null, "IntelliJ IDEA, Maven")).append("\n");
        sb.append("该软件的运行平台/操作系统：").append(getOrDefault(summary != null ? summary.getRunPlatform() : null, "Windows 10/11, Linux")).append("\n");
        sb.append("软件运行支撑环境/支持软件：").append(getOrDefault(summary != null ? summary.getRunSupport() : null, "JDK 17, MySQL 8.0")).append("\n");
        sb.append("编程语言：").append(getOrDefault(summary != null ? summary.getLanguage() : null, "Java")).append("\n");
        sb.append("源程序量：约 ").append(summary != null && summary.getCodeLines() != null ? summary.getCodeLines() : 5000).append(" 行\n");
        sb.append("开发目的：").append(getOrDefault(summary != null ? summary.getPurpose() : null, "【待填写】")).append("\n");
        sb.append("面向领域：").append(getOrDefault(summary != null ? summary.getTargetDomain() : null, "【待填写】")).append("\n");
        sb.append("主要功能：").append(getOrDefault(summary != null ? summary.getMainFunctions() : null, "【待填写】")).append("\n");
        sb.append("技术特点：").append(getOrDefault(summary != null ? summary.getTechFeatures() : null, "【待填写】")).append("\n");
        sb.append("软件的技术特点选项：").append(getOrDefault(summary != null ? summary.getTechFeatureOptions() : null, "前后端分离架构")).append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String getFileName() {
        ProjectVO project = projectService.getProjectById(projectId);
        return project.getName() + ".txt";
    }

    private String getOrDefault(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
