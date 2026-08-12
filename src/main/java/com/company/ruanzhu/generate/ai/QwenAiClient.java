package com.company.ruanzhu.generate.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Qwen (通义千问) AI client implementation.
 * Uses DashScope API for Alibaba Cloud's Qwen models.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "qwen")
public class QwenAiClient implements AiClient {

    @Value("${ai.qwen.api-key:}")
    private String apiKey;

    @Value("${ai.qwen.model:qwen-plus}")
    private String model;

    @Override
    public String generate(String prompt) {
        return generate("你是一个专业的软件开发文档撰写助手。", prompt);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        log.info("Qwen generate: model={}, prompt length={}", model, userPrompt.length());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Qwen API key not configured, returning placeholder");
            return generatePlaceholder(userPrompt);
        }

        // TODO: Implement actual DashScope API call
        // For now, return placeholder
        return generatePlaceholder(userPrompt);
    }

    @Override
    public String getName() {
        return "qwen";
    }

    private String generatePlaceholder(String prompt) {
        if (prompt.contains("开发目的") || prompt.contains("purpose")) {
            return "本软件旨在为企业用户提供一站式的业务管理解决方案，通过数字化手段优化业务流程，" +
                    "提升运营效率。系统核心关注点包括：数据安全管理、多用户协同、" +
                    "灵活的工作流配置，以及实时的业务数据监控与分析能力。";
        }
        if (prompt.contains("面向领域") || prompt.contains("domain")) {
            return "本软件主要面向企业信息化管理领域，适用于各类中小型企业的日常运营管理。" +
                    "目标用户群体包括：企业管理人员、项目团队成员、数据分析人员等。" +
                    "系统可广泛应用于项目管理、客户关系管理、进销存管理等业务场景。";
        }
        if (prompt.contains("主要功能") || prompt.contains("functions")) {
            return "1. 用户认证与权限管理：实现用户注册、登录、角色分配、权限控制，保障系统安全访问。\n" +
                    "2. 数据管理功能：支持业务数据的录入、修改、查询、删除，提供批量导入导出能力。\n" +
                    "3. 工作流引擎：自定义审批流程，支持多级审核、条件分支、自动流转等功能。\n" +
                    "4. 报表与分析：提供多维度数据统计、图表展示、Excel导出等功能。\n" +
                    "5. 系统监控：实时监控系统运行状态、用户操作日志、异常告警等功能。\n" +
                    "6. 接口集成：提供标准RESTful API，支持与第三方系统对接集成。\n" +
                    "以上功能模块通过统一的业务中台进行数据交换，确保信息流通畅、业务衔接紧密。";
        }
        if (prompt.contains("技术特点") || prompt.contains("features")) {
            return "本软件采用微服务架构设计，基于Spring Cloud生态构建，支持服务注册发现、" +
                    "配置中心、熔断降级等能力。前端采用Vue3框架，结合Element Plus组件库，" +
                    "提供响应式用户界面。数据存储使用MySQL主从架构，Redis集群提供缓存加速。" +
                    "系统支持容器化部署，可通过Docker Compose或Kubernetes进行编排管理。";
        }
        return "【AI生成内容占位符 - 请配置通义千问API密钥以生成真实内容】";
    }
}
