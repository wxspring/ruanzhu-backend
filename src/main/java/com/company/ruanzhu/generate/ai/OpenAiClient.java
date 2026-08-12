package com.company.ruanzhu.generate.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OpenAI-compatible AI client implementation.
 * Can work with OpenAI API, Azure OpenAI, or compatible services.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiClient implements AiClient {

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4}")
    private String model;

    @Override
    public String generate(String prompt) {
        return generate("You are a helpful assistant.", prompt);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        log.info("OpenAI generate: model={}, prompt length={}", model, userPrompt.length());

        // TODO: Implement actual OpenAI API call
        // For now, return a placeholder response
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key not configured, returning placeholder");
            return generatePlaceholder(userPrompt);
        }

        // Placeholder implementation - replace with actual API call
        // Use Spring AI or direct HTTP client
        return generatePlaceholder(userPrompt);
    }

    @Override
    public String getName() {
        return "openai";
    }

    private String generatePlaceholder(String prompt) {
        // Generate context-aware placeholder based on prompt keywords
        if (prompt.contains("开发目的") || prompt.contains("purpose")) {
            return "本软件旨在为用户提供高效、便捷的解决方案，通过先进的技术架构实现核心业务功能，" +
                    "提升工作效率，降低运营成本。系统采用模块化设计，支持灵活扩展，" +
                    "满足不同场景下的业务需求。";
        }
        if (prompt.contains("面向领域") || prompt.contains("domain")) {
            return "本软件面向企业级应用市场，主要服务于中小型企业的数字化转型需求。" +
                    "适用于软件开发、项目管理、数据分析等领域，帮助用户实现业务流程自动化，" +
                    "提升团队协作效率。";
        }
        if (prompt.contains("主要功能") || prompt.contains("functions")) {
            return "1. 用户管理模块：提供用户注册、登录、权限管理等功能，确保系统安全性。\n" +
                    "2. 数据处理模块：支持数据的增删改查、批量导入导出、数据校验等操作。\n" +
                    "3. 报表统计模块：自动生成各类统计报表，支持数据可视化展示。\n" +
                    "4. 系统配置模块：提供系统参数配置、日志管理、备份恢复等功能。\n" +
                    "5. 接口服务模块：提供标准化的API接口，支持第三方系统集成。\n" +
                    "各模块之间通过统一的数据接口进行交互，确保数据一致性和系统稳定性。";
        }
        if (prompt.contains("技术特点") || prompt.contains("features")) {
            return "本软件采用前后端分离架构，后端基于Spring Boot框架，前端使用React技术栈。" +
                    "数据库采用MySQL，缓存使用Redis，支持高并发访问。系统遵循RESTful API设计规范，" +
                    "采用JWT进行身份认证，确保接口安全性。代码结构清晰，易于维护和扩展。";
        }
        return "【AI生成内容占位符 - 请配置AI API密钥以生成真实内容】";
    }
}
