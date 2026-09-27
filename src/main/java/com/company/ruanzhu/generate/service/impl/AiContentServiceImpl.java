package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.generate.ai.AiClient;
import com.company.ruanzhu.generate.service.AiContentService;
import com.company.ruanzhu.generate.util.FunctionMenuNormalizer;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of AI content generation service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiContentServiceImpl implements AiContentService {

    private final AiClient aiClient;

    @Override
    public String generatePurpose(String softwareName, String category, String codeAnalysis) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请根据提供的软件信息，撰写"开发目的"部分。
                要求：
                1. 字数在300-500字之间
                2. 清晰阐述软件的开发背景和核心关注点
                3. 说明软件要解决的业务问题
                4. 描述预期的业务价值和技术目标
                5. 语言正式、专业，符合软著申请材料规范
                """;

        String userPrompt = String.format("""
                软件名称：%s
                软件分类：%s
                代码分析结果：%s

                请撰写该软件的开发目的说明。
                """, softwareName, category, codeAnalysis);

        return aiClient.generate(systemPrompt, userPrompt);
    }

    @Override
    public String generateTargetDomain(String softwareName, String category, String codeAnalysis) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请根据提供的软件信息，撰写"面向领域"部分。
                要求：
                1. 字数在200-300字之间
                2. 描述软件适用的行业和应用场景
                3. 说明目标用户群体
                4. 阐述软件解决的核心痛点
                5. 语言正式、专业
                """;

        String userPrompt = String.format("""
                软件名称：%s
                软件分类：%s
                代码分析结果：%s

                请撰写该软件的面向领域说明。
                """, softwareName, category, codeAnalysis);

        return aiClient.generate(systemPrompt, userPrompt);
    }

    @Override
    public String generateMainFunctions(String softwareName, String category, String codeAnalysis) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请根据提供的软件信息，撰写"主要功能"部分。
                要求：
                1. 字数在800-1000字之间
                2. 详细描述每个功能模块的作用
                3. 说明各功能模块之间的关系和衔接流程
                4. 体现功能的完整性和系统性
                5. 使用"1. xxx模块：..."的格式列举
                6. 最后总结各模块的协作关系
                7. 语言正式、专业
                """;

        String userPrompt = String.format("""
                软件名称：%s
                软件分类：%s
                代码分析结果：%s

                请撰写该软件的主要功能说明，要求800-1000字，详细描述各功能模块及其关系。
                """, softwareName, category, codeAnalysis);

        return aiClient.generate(systemPrompt, userPrompt);
    }

    @Override
    public String generateTechFeatures(String softwareName, String language, String frameworks, String codeAnalysis) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请根据提供的软件信息，撰写"技术特点"部分。
                要求：
                1. 字数在150-200字之间
                2. 描述软件的架构选型（如前后端分离、微服务等）
                3. 说明使用的关键技术和框架
                4. 突出技术亮点和创新点
                5. 语言简洁、专业
                """;

        String userPrompt = String.format("""
                软件名称：%s
                编程语言：%s
                使用框架：%s
                代码分析结果：%s

                请撰写该软件的技术特点说明，要求150-200字。
                """, softwareName, language, frameworks, codeAnalysis);

        return aiClient.generate(systemPrompt, userPrompt);
    }

    @Override
    public SoftwareSummaryVO generateAllFields(String softwareName, String category, String language,
                                                String frameworks, String codeAnalysis) {
        log.info("Generating all summary fields for software: {}", softwareName);

        SoftwareSummaryVO vo = new SoftwareSummaryVO();

        try {
            vo.setPurpose(generatePurpose(softwareName, category, codeAnalysis));
        } catch (Exception e) {
            log.error("Failed to generate purpose", e);
            vo.setPurpose("【生成失败，请手动填写】");
        }

        try {
            vo.setTargetDomain(generateTargetDomain(softwareName, category, codeAnalysis));
        } catch (Exception e) {
            log.error("Failed to generate target domain", e);
            vo.setTargetDomain("【生成失败，请手动填写】");
        }

        try {
            vo.setMainFunctions(generateMainFunctions(softwareName, category, codeAnalysis));
        } catch (Exception e) {
            log.error("Failed to generate main functions", e);
            vo.setMainFunctions("【生成失败，请手动填写】");
        }

        try {
            vo.setTechFeatures(generateTechFeatures(softwareName, language, frameworks, codeAnalysis));
        } catch (Exception e) {
            log.error("Failed to generate tech features", e);
            vo.setTechFeatures("【生成失败，请手动填写】");
        }

        try {
            vo.setSystemOverview(generateSystemOverview(softwareName, category, codeAnalysis));
        } catch (Exception e) {
            log.error("Failed to generate system overview", e);
            vo.setSystemOverview("【生成失败，请手动填写】");
        }

        try {
            vo.setFunctionalFeatures(buildFunctionalFeatures(
                softwareName, vo, category, language, frameworks, codeAnalysis
            ));
        } catch (Exception e) {
            log.error("Failed to build functional features", e);
            vo.setFunctionalFeatures("【生成失败，请手动填写】");
        }

        return vo;
    }

    private String generateSystemOverview(String softwareName, String category, String codeAnalysis) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请为软件撰写【系统概述】部分。
                要求：
                1. 字数在300-500字之间
                2. 描述软件解决的核心痛点和业务背景
                3. 说明软件的整体架构与设计理念（例如：数字化管控、全生命周期追踪等）
                4. 说明核心业务流程与关键能力
                5. 强调效率提升、成本降低、质量保障等业务价值
                6. 只输出正文段落，不要加任何标题
                7. 语言正式、专业，符合软著申请材料规范
                """;

        String userPrompt = String.format("""
                软件名称：%s
                软件分类：%s
                种子代码分析：%s

                请撰写该软件的系统概述。
                """, softwareName, category, codeAnalysis);
        return aiClient.generate(systemPrompt, userPrompt);
    }

    private String buildFunctionalFeatures(String softwareName, SoftwareSummaryVO vo,
                                            String category, String language,
                                            String frameworks, String codeAnalysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("软著名称：").append(softwareName).append("\n");
        sb.append("版本号：").append(vo.getVersion() != null ? vo.getVersion() : "V1.0").append("\n");
        sb.append("软件分类：").append(category != null ? category : "应用软件").append("\n");
        sb.append("开发的硬件环境：").append(vo.getDevHardware() != null ? vo.getDevHardware() : "Intel Core i5 及以上，8GB 内存，500GB 硬盘").append("\n");
        sb.append("运行的硬件环境：").append(vo.getRunHardware() != null ? vo.getRunHardware() : "Intel Core i3 及以上，4GB 内存，200GB 硬盘").append("\n");
        sb.append("开发该软件的操作系统：").append(vo.getDevOs() != null ? vo.getDevOs() : "Windows 10/11 64位").append("\n");
        sb.append("软件开发环境/开发工具：").append(vo.getDevTools() != null ? vo.getDevTools() : (language != null && language.equalsIgnoreCase("Java") ? "IntelliJ IDEA, Maven, Git" : "VS Code, Git")).append("\n");
        sb.append("该软件的运行平台/操作系统：").append(vo.getRunPlatform() != null ? vo.getRunPlatform() : "Windows Server 2019 或 CentOS 7.x").append("\n");
        sb.append("软件运行支撑环境/支持软件：").append(vo.getRunSupport() != null ? vo.getRunSupport() : (language != null && language.equalsIgnoreCase("Java") ? "JDK 1.8, Tomcat 9.0, MySQL 8.0, Redis 6.0, Nginx 1.20" : "Node.js 18.x, MySQL 8.0, Redis 6.0, Nginx 1.20")).append("\n");
        sb.append("编程语言：").append(language != null ? language : "Java").append("\n");
        sb.append("源程序量：").append(vo.getCodeLines() != null ? vo.getCodeLines() + " 行" : "12000 行").append("\n");
        sb.append("开发目的：").append(vo.getPurpose() != null ? vo.getPurpose() : "").append("\n");
        sb.append("面向领域：").append(vo.getTargetDomain() != null ? vo.getTargetDomain() : "").append("\n");
        sb.append("主要功能：").append(vo.getMainFunctions() != null ? vo.getMainFunctions() : "").append("\n");
        sb.append("技术特点：").append(vo.getTechFeatures() != null ? vo.getTechFeatures() : "").append("\n");
        return sb.toString();
    }

    @Override
    public String generateCustom(String customPrompt) {
        if (customPrompt == null || customPrompt.isBlank()) {
            throw new IllegalArgumentException("customPrompt must not be empty");
        }
        String generated;
        try {
            generated = aiClient.generate(customPrompt);
        } catch (Exception e) {
            log.error("AI custom generation failed", e);
            throw new RuntimeException("AI 生成失败：" + e.getMessage(), e);
        }
        // 如果是"功能菜单"类型的提示词，无论 AI 返回什么脏内容，都在服务端强制规范化：
        //   恰好 10 条，格式 子菜单（一级菜单），一级菜单 3~5 个，每个一级 2~3 个，剔除禁用词
        if (FunctionMenuNormalizer.isMenuPrompt(customPrompt)) {
            String cleaned = FunctionMenuNormalizer.normalize(generated, null, customPrompt);
            log.info("Function menu normalized: prompt detected, {} -> {} lines",
                    String.valueOf(generated == null ? 0 : generated.split("\\R").length),
                    cleaned.split("\\R").length);
            return cleaned;
        }
        return generated;
    }
}
