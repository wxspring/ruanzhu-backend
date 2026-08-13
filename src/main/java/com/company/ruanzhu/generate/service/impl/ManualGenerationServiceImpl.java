package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
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
        String runPlatform = summary.getRunPlatform() != null ? summary.getRunPlatform() : "Windows 10/11, Linux";

        return String.format("""
                你是一个专业的技术文档工程师。请为以下软件生成一份完整的操作手册（HTML格式）。

                软件信息：
                - 软件名称：%s
                - 版本号：%s
                - 开发目的：%s
                - 面向领域：%s
                - 主要功能：%s
                - 开发环境：%s
                - 运行平台：%s

                要求：
                1. 生成完整的 HTML 格式文档（包含 <!DOCTYPE html>、<html>、<head>、<body> 标签）
                2. 使用清晰的章节结构，包含目录
                3. 包含以下章节：
                   - 第一章：软件概述（简介、功能特点、系统要求）
                   - 第二章：安装与配置（安装步骤、初始配置、环境要求）
                   - 第三章：快速入门（基本操作、界面说明）
                   - 第四章：功能详解（详细说明每个主要功能的使用方法）
                   - 第五章：常见问题（FAQ、故障排除）
                   - 第六章：技术支持（联系方式、更新日志）
                4. 每个章节要有详细的说明和操作步骤
                5. 使用专业的技术文档语言
                6. 内容要充实、实用，总字数在 3000-5000 字左右
                7. 使用适当的 HTML 标签（h1, h2, h3, p, ul, ol, li, table 等）
                8. 直接输出 HTML 代码，不要有其他说明文字

                注意：生成的操作手册应该专业、完整，可以直接用于软件著作权申请。
                """, softwareName, version, purpose, domain, functions, devOs, runPlatform);
    }
}
