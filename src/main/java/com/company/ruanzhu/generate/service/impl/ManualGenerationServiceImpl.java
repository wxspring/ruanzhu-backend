package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.util.LanguageEnvironment;
import com.company.ruanzhu.generate.ai.AiClient;
import com.company.ruanzhu.generate.service.ManualGenerationService;
import com.company.ruanzhu.project.model.Project;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.repository.ProjectRepository;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of manual generation service.
 * Generates operation manual HTML content using AI based on project information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualGenerationServiceImpl implements ManualGenerationService {

    private final ProjectRepository projectRepository;
    private final SoftwareSummaryRepository softwareSummaryRepository;
    private final AiClient aiClient;

    @Override
    public String generateManual(Long projectId) {
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        SoftwareSummary summary = softwareSummaryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUMMARY_NOT_FOUND));

        log.info("Generating operation manual for project: {}", project.getName());

        String prompt = buildPrompt(project, summary);
        String htmlContent = aiClient.generate(prompt);

        log.info("Generated operation manual with {} characters", htmlContent.length());
        return htmlContent;
    }

    private String buildPrompt(Project project, SoftwareSummary summary) {
        String softwareName = project.getName();
        String version = summary.getVersion() != null ? summary.getVersion() : "V1.0";
        String purpose = summary.getPurpose() != null ? summary.getPurpose() : "提供专业的软件解决方案";
        String domain = summary.getTargetDomain() != null ? summary.getTargetDomain() : "企业管理";
        String functions = summary.getMainFunctions() != null ? summary.getMainFunctions() : "基础管理功能";
        String devOs = summary.getDevOs() != null ? summary.getDevOs() : "Windows 10/11";
        String runPlatform = LanguageEnvironment.runPlatformOr(summary.getLanguage(), summary.getRunPlatform());
        String functionMenu = summary.getFunctionMenu() != null ? summary.getFunctionMenu() : "";

        return String.format("""
                你是一个专业的软件著作权申请材料撰写专家。请为以下软件生成一份操作手册（HTML格式）。

                软件信息：
                - 软件名称：%s
                - 版本号：%s
                - 开发目的：%s
                - 面向领域：%s
                - 主要功能：%s
                - 开发环境：%s
                - 运行平台：%s
                - 功能菜单（一级菜单为模块名）：
                %s

                要求：
                1. 生成完整的 HTML 格式文档
                2. 文档结构精简为两章：
                   - 第一章 软件介绍：1.1 软件概述（200-300字，简洁说明软件定位与核心价值）
                   - 第二章 功能介绍：2.1 登录，然后逐一介绍功能菜单中的每个模块（每个模块100-150字，说明该模块的列表页查看内容和新增/录入页需填写的字段）
                3. 模块名称必须与功能菜单中的一级菜单名称完全一致
                4. 语言正式、专业，符合软著申请材料规范
                5. 内容简洁实用，总字数 1500-2500 字
                6. 使用 HTML 标签（h1, h2, h3, p）
                7. 直接输出 HTML 代码，不要包含 markdown 代码围栏（```）
                8. 不要使用"待补充""XXX"等占位符

                注意：手册应专业简洁，以功能操作为核心，适合配合系统截图使用。
                """, softwareName, version, purpose, domain, functions, devOs, runPlatform, functionMenu);
    }
}
