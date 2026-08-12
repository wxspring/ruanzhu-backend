package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * Generates operation manual document (说明文档) in Word format.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ManualDocGenerator implements DocumentGenerator {

    private final ProjectService projectService;
    private final Long projectId;

    public ManualDocGenerator(ProjectService projectService, Long projectId) {
        this.projectService = projectService;
        this.projectId = projectId;
    }

    @Override
    public byte[] generateWord() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();

        try (XWPFDocument document = new XWPFDocument()) {
            // Title
            addTitle(document, project.getName() + " 操作说明书");

            // Version info
            addVersionInfo(document, summary);

            // Table of Contents placeholder
            addTocPlaceholder(document);

            // Chapter 1: Introduction
            addChapter1(document, summary);

            // Chapter 2: Installation
            addChapter2(document, summary);

            // Chapter 3: Main Functions
            addChapter3(document, summary);

            // Chapter 4: Operation Guide
            addChapter4(document, summary);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate manual document", e);
            throw new RuntimeException("生成说明文档失败", e);
        }
    }

    @Override
    public String getFileName() {
        ProjectVO project = projectService.getProjectById(projectId);
        return project.getName() + "+说明.docx";
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph para = document.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingAfter(200);

        XWPFRun run = para.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(22);
        run.setFontFamily("黑体");
    }

    private void addVersionInfo(XWPFDocument document, SoftwareSummaryVO summary) {
        XWPFParagraph para = document.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingAfter(400);

        XWPFRun run = para.createRun();
        String version = summary != null && summary.getVersion() != null ? summary.getVersion() : "V1.0";
        run.setText("版本：" + version);
        run.setFontSize(12);
        run.setFontFamily("宋体");
    }

    private void addTocPlaceholder(XWPFDocument document) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(200);

        XWPFRun run = para.createRun();
        run.setText("目录（请在Word中右键更新域以生成目录）");
        run.setItalic(true);
        run.setColor("666666");

        // Add TOC field
        para = document.createParagraph();
        XWPFRun tocRun = para.createRun();
        tocRun.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        tocRun = para.createRun();
        tocRun.getCTR().addNewInstrText().setStringValue(" TOC \\o \"1-3\" \\h \\z \\u ");
        tocRun = para.createRun();
        tocRun.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);

        document.createParagraph().setSpacingAfter(200);
    }

    private void addChapter1(XWPFDocument document, SoftwareSummaryVO summary) {
        addHeading1(document, "第一章 软件概述");

        addHeading2(document, "1.1 软件简介");
        String purpose = summary != null && summary.getPurpose() != null
                ? summary.getPurpose()
                : "本软件是一款专业的企业管理解决方案，旨在帮助用户提升工作效率，优化业务流程。";
        addBodyText(document, purpose);

        addHeading2(document, "1.2 应用领域");
        String domain = summary != null && summary.getTargetDomain() != null
                ? summary.getTargetDomain()
                : "本软件适用于各类中小型企业的日常运营管理。";
        addBodyText(document, domain);

        addHeading2(document, "1.3 技术特点");
        String features = summary != null && summary.getTechFeatures() != null
                ? summary.getTechFeatures()
                : "本软件采用先进的技术架构，具有良好的可扩展性和维护性。";
        addBodyText(document, features);
    }

    private void addChapter2(XWPFDocument document, SoftwareSummaryVO summary) {
        addHeading1(document, "第二章 安装与配置");

        addHeading2(document, "2.1 系统要求");
        String devHardware = summary != null && summary.getRunHardware() != null
                ? summary.getRunHardware()
                : "- 处理器：Intel Core i5 或同等性能\n- 内存：4GB 以上\n- 硬盘：10GB 可用空间";
        addBodyText(document, "硬件要求：\n" + devHardware);

        String runPlatform = summary != null && summary.getRunPlatform() != null
                ? summary.getRunPlatform()
                : "Windows 10/11 或 Linux 发行版";
        addBodyText(document, "操作系统：" + runPlatform);

        addHeading2(document, "2.2 安装步骤");
        addBodyText(document, "1. 获取软件安装包\n2. 双击运行安装程序\n3. 按照向导完成安装\n4. 启动软件并进行初始化配置");

        addHeading2(document, "2.3 初始配置");
        addBodyText(document, "首次启动时，请按照以下步骤进行配置：\n1. 设置管理员账号\n2. 配置系统参数\n3. 导入基础数据");
    }

    private void addChapter3(XWPFDocument document, SoftwareSummaryVO summary) {
        addHeading1(document, "第三章 功能说明");

        String functions = summary != null && summary.getMainFunctions() != null
                ? summary.getMainFunctions()
                : "本软件包含以下主要功能模块：\n1. 用户管理\n2. 数据处理\n3. 报表统计\n4. 系统配置";
        addBodyText(document, functions);
    }

    private void addChapter4(XWPFDocument document, SoftwareSummaryVO summary) {
        addHeading1(document, "第四章 操作指南");

        addHeading2(document, "4.1 登录系统");
        addBodyText(document, "1. 打开软件\n2. 输入用户名和密码\n3. 点击登录按钮");

        addHeading2(document, "4.2 主界面说明");
        addBodyText(document, "登录后将进入主界面，界面包含以下区域：\n- 顶部导航栏：显示当前用户和主要功能入口\n- 左侧菜单栏：功能模块导航\n- 中央工作区：显示当前操作内容\n- 底部状态栏：显示系统状态信息");

        addHeading2(document, "4.3 常用操作");
        addBodyText(document, "数据录入：点击相应功能菜单，填写表单信息，点击保存。\n数据查询：在搜索框输入关键词，点击查询按钮。\n数据导出：选择需要导出的数据，点击导出按钮，选择导出格式。");

        addHeading2(document, "4.4 常见问题");
        addBodyText(document, "Q: 忘记密码怎么办？\nA: 请联系管理员重置密码。\n\nQ: 数据导出失败？\nA: 请检查网络连接和磁盘空间。");
    }

    private void addHeading1(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(400);
        para.setSpacingAfter(200);

        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(16);
        run.setFontFamily("黑体");
    }

    private void addHeading2(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingBefore(200);
        para.setSpacingAfter(100);

        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("黑体");
    }

    private void addBodyText(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setSpacingAfter(100);
        para.setIndentationFirstLine(480); // 2 chars indent

        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.setFontFamily("宋体");
    }
}
