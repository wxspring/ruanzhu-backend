package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.common.util.LanguageEnvironment;
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
    public String generateMainFunctions(String softwareName, String category, String codeAnalysis, String functionMenu) {
        String systemPrompt = """
                你是一个专业的软件著作权申请材料撰写专家。请根据提供的软件信息，撰写"主要功能"部分。

                【输出格式】
                按功能菜单中的一级菜单模块顺序，逐个描述每个模块，使用"模块名称模块负责/支持/用于..."的格式，每个模块之间用分号；分隔。模块名称必须与功能菜单中的一级菜单名称完全一致。

                每个模块的描述必须包含以下三方面内容（用逗号连接）：
                1. 该模块的核心业务职责（一句话概括）
                2. 列表页展示的主要字段（如编号、名称、状态、创建时间等）
                3. 录入页需填写的关键字段，以及支持的操作（新增、编辑、查询、删除、导入导出、审批等）

                所有模块描述完后，另起一段说明各模块之间的业务衔接关系。

                【示例】
                数据核验审批模块负责发起市政基础设施数据核验并跟踪整改情况，列表页展示核验编号、项目名称、数据类型、核验状态、提交人、提交时间等字段，录入页需填写项目名称、数据类型、核验内容、附件说明，支持新增、编辑、查询、提交审批、导出等操作；数字化标准配置模块支持配置BIM模型构件编码规则并下发验收标准，列表页展示标准编号、标准名称、适用范围、生效日期、状态等字段，录入页需填写标准名称、适用范围、验收规则、阈值参数，支持新增、编辑、查询、启用、停用等操作；工程数据归集模块用于录入项目基础数据并登记参建单位信息，列表页展示项目编号、项目名称、参建单位、数据类型、录入时间等字段，录入页需填写项目名称、参建单位、数据类型、基础数据说明，支持新增、编辑、查询、导入、导出等操作。
                各模块之间业务衔接紧密，数据核验审批的结果会回写至工程数据归集的项目记录中，数字化标准配置下发的编码规则被各模块引用，项目数据申报的数据来源于工程数据归集的归集结果。

                【禁止事项】
                - 不要使用"待补充""XXX""相关信息"等占位符
                - 不要省略列表字段和录入字段的具体内容
                - 模块名称必须与功能菜单完全一致，不要自行修改
                """;

        String userPrompt = String.format("""
                软件名称：%s
                软件分类：%s
                代码分析结果：%s
                功能菜单（一级菜单为模块名，子菜单为功能点）：
                %s

                请按照上述格式，逐个描述功能菜单中的每个一级菜单模块。
                """, softwareName, category, codeAnalysis,
                functionMenu != null ? functionMenu : "（功能菜单未提供）");

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
                                                String frameworks, String codeAnalysis, String functionMenu) {
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
            vo.setMainFunctions(generateMainFunctions(softwareName, category, codeAnalysis, functionMenu));
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
        sb.append("软件开发环境/开发工具：").append(LanguageEnvironment.devToolsOr(language, vo.getDevTools())).append("\n");
        sb.append("该软件的运行平台/操作系统：").append(LanguageEnvironment.runPlatformOr(language, vo.getRunPlatform())).append("\n");
        sb.append("软件运行支撑环境/支持软件：").append(LanguageEnvironment.supportStackOr(language, vo.getRunSupport())).append("\n");
        sb.append("编程语言：").append(language != null ? language : "按项目实际编程语言填写").append("\n");
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
