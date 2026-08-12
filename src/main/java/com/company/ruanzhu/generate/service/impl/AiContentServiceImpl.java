package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.generate.ai.AiClient;
import com.company.ruanzhu.generate.service.AiContentService;
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

        return vo;
    }
}
