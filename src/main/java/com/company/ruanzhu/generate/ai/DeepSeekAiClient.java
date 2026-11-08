package com.company.ruanzhu.generate.ai;

import com.company.ruanzhu.common.util.LanguageEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DeepSeek (https://api.deepseek.com) AI client implementation.
 * Uses the official OpenAI-compatible /v1/chat/completions endpoint.
 * Config: ai.provider=deepseek; keys: ai.deepseek.{api-key,base-url,model}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "deepseek")
public class DeepSeekAiClient implements AiClient {

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.model:deepseek-v4-flash}")
    private String model;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    public DeepSeekAiClient() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.setErrorHandler(new DetailedResponseErrorHandler(objectMapper));

        // ╔══════════════════════════════════════════════════════════════════════╗
        // ║ 【关键修正】强制读取本项目 .env 文件并覆盖 @Value 注入值。              ║
        // ║ 原因：Spring 环境优先级"IDEA 运行配置 Environment variables"永远最高，  ║
        // ║       IDEA 里如果留了旧 key / 脏 base-url，用户改 .env 100 次都没用。  ║
        // ║       我们直接读文件本身，拿到的就是你写到磁盘上的真实值。              ║
        // ╚══════════════════════════════════════════════════════════════════════╝
        DotEnvOverride.Result env = DotEnvOverride.loadDotEnv();
        String fromFileKey = env.get("DEEPSEEK_API_KEY", "AI_DEEPSEEK_API_KEY");
        String fromFileBase = env.get("DEEPSEEK_BASE_URL", "AI_DEEPSEEK_BASE_URL");
        String fromFileModel = env.get("DEEPSEEK_MODEL", "AI_DEEPSEEK_MODEL");
        if (fromFileKey != null) {
            if (!fromFileKey.equals(apiKey)) {
                log.info("[.env OVERRIDE] DEEPSEEK_API_KEY 由文件 {} 覆盖（已忽略 IDEA 运行配置的脏值），新 key-tail=***{}",
                        env.sourcePath(), DotEnvOverride.tail4(fromFileKey));
            }
            this.apiKey = fromFileKey;
        }
        if (fromFileBase != null) this.baseUrl = fromFileBase;
        if (fromFileModel != null) this.model = fromFileModel;
        log.info("DeepSeekAiClient 初始化完成：.env-source={}, model={}, base-url(raw)={}, key-tail=***{}",
                env.sourcePath(), model, baseUrl, DotEnvOverride.tail4(apiKey));
    }

    @Override
    public String generate(String prompt) {
        return generate("你是一名资深的软件行业文案撰稿人，严格遵守用户在提示词中给出的步骤、违禁词与格式约束；严禁输出违禁套话。", prompt);
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String normalizedBase = normalizeBaseUrl(baseUrl);
        log.info("DeepSeek generate: model={}, baseUrl={} (raw={}), key-tail=***{}, prompt length={}",
                model, normalizedBase, baseUrl, DotEnvOverride.tail4(apiKey), userPrompt == null ? 0 : userPrompt.length());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("================================= AI 未调用 ==================================");
            log.warn("DEEPSEEK_API_KEY 未配置（application.yml 中 ai.deepseek.api-key 为空）");
            log.warn("  本次返回的内容为本地占位文案（非真实大模型生成）。");
            log.warn("  如需启用真实 AI，请在 .env 中写入 DEEPSEEK_API_KEY=sk-xxx，或在 application-dev.yml 中明文配置。");
            log.warn("============================================================================");
            return generatePlaceholder(userPrompt);
        }

        try {
            String result = callChatCompletions(normalizedBase, systemPrompt, userPrompt);
            if (result != null && !result.isBlank()) {
                log.info("DeepSeek generate success, output length={}", result.length());
                return result;
            }
            log.warn("DeepSeek generate returned empty content, falling back to local placeholder");
        } catch (Exception e) {
            log.error("DeepSeek generate HTTP call failed, falling back to local placeholder", e);
        }
        log.warn("================================= AI 未调用 ==================================");
        log.warn("  真实 AI 调用失败或返回空，本次返回的内容为本地占位文案（非真实大模型生成）。");
        log.warn("============================================================================");
        return generatePlaceholder(userPrompt);
    }

    /** 规范化 base-url：只清洗脏值，不做协议推断。
     *  - 剥尾逗号、空白、引号、反引号
     *  - 把写错的子路径（/anthropic /claude /bedrock /models /chat）剥离（这些是 Anthropic SDK 端点或模型列表，我们用的是 chat/completions）
     *  - 剥末尾多余的斜杠
     *  注意：我们只清洗，不主动添加 /v1，因为 DeepSeek 官方 base_url 就是 https://api.deepseek.com（没有 /v1），
     *  而 OpenAI 官方 base_url 是 https://api.openai.com/v1，DashScope 官方 base_url 是 https://dashscope.aliyuncs.com/compatible-mode/v1。
     *  各 provider 的 @Value 默认值已经写正确，这里只负责把用户手滑写错的脏值洗干净。
     */
    static String normalizeBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) return "https://api.deepseek.com";
        String s = raw.trim();
        // 去尾噪：逗号、分号、反引号、引号、空白、问号等
        s = s.replaceAll("[,;`\"'\\s]+$", "");
        while (s.endsWith("?")) s = s.substring(0, s.length() - 1);
        // 把明显不对的子路径干掉（/anthropic 是 Anthropic SDK 的端点，/models 是模型列表端点，都不是 chat/completions）
        s = s.replaceAll("(?i)/(anthropic|claude|bedrock|models|chat)(/.*)?$", "");
        // 剥末尾所有连续 /
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String callChatCompletions(String normalizedBase, String systemPrompt, String userPrompt) throws Exception {
        String url = normalizedBase + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", 0.7);
        body.put("max_tokens", 4096);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new LinkedHashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);
        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode content = root.at("/choices/0/message/content");
            if (!content.isMissingNode() && !content.isNull()) {
                return content.asText("").trim();
            }
            JsonNode errMsg = root.at("/error/message");
            if (!errMsg.isMissingNode()) {
                log.warn("DeepSeek 2xx 但含 error: code={} msg={}", root.at("/error/code").asText(""), errMsg.asText(""));
            }
        }
        log.warn("DeepSeek unexpected response status={}, body={}", resp.getStatusCode(), resp.getBody());
        return null;
    }

    @Override
    public String getName() {
        return "deepseek";
    }

    // ============ 下面与 QwenAiClient 完全相同（DashScope/DeepSeek 占位兜底逻辑一致，便于无 API Key 时输出海水淡化相关内容） ============

    private record PromptContext(String softwareName, String category, String domain,
                                String language, String purpose, String functions,
                                String version) {}

    private PromptContext extractContext(String prompt) {
        String name = extractField(prompt, "软件名称", "软著名称");
        String category = extractField(prompt, "软件分类");
        String domain = extractField(prompt, "面向领域");
        String language = extractField(prompt, "编程语言");
        String purpose = extractField(prompt, "开发目的");
        String functions = extractField(prompt, "主要功能");
        String version = extractField(prompt, "版本号");
        return new PromptContext(name, category, domain, language, purpose, functions, version);
    }

    private String extractField(String prompt, String key, String... aliases) {
        if (prompt == null) return null;
        String[] keys = new String[aliases.length + 1];
        keys[0] = key;
        System.arraycopy(aliases, 0, keys, 1, aliases.length);
        for (String k : keys) {
            Pattern p = Pattern.compile(k + "\\s*[：:]\\s*(.+?)(?:\\n|$)");
            Matcher m = p.matcher(prompt);
            if (m.find()) {
                String v = m.group(1).trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private boolean matchesDomain(PromptContext ctx, String... keywords) {
        if (ctx == null) return false;
        String[] pool = new String[]{ctx.category(), ctx.domain(), ctx.softwareName()};
        for (String s : pool) {
            if (s == null) continue;
            for (String k : keywords) {
                if (s.contains(k)) return true;
            }
        }
        return false;
    }

    private boolean matchesDomainByContextOrHint(PromptContext ctx, String hint, String... keywords) {
        if (matchesDomain(ctx, keywords)) return true;
        if (hint == null) return false;
        for (String k : keywords) {
            if (hint.contains(k)) return true;
        }
        return false;
    }

    private boolean isCodeGenerationPrompt(String prompt) {
        if (prompt == null) return false;
        // 先用代码生成链路独有的特征词做正向判定（优先级最高）。
        // 代码生成提示词里同样会出现"开发目的""面向领域"等字段名，
        // 所以不能先用这些词做排除，否则会把代码生成误判成文书生成。
        if (prompt.contains("可编译运行的源代码") || prompt.contains("【代码结构强制要求")
                || prompt.contains("补充更多代码") || prompt.contains("直接输出代码")) {
            return true;
        }
        // 文书类提示词（功能特点/系统概述等）的字段清单里会出现"源程序量"等字样，
        // 绝不能据此判定为代码生成，否则会把代码骨架写进功能特点文本框。
        if (prompt.contains("功能特点") || prompt.contains("系统概述")
                || prompt.contains("技术特点选项")) {
            return false;
        }
        return prompt.contains("源代码") || prompt.contains("源码")
                || prompt.contains("代码生成") || prompt.contains("完整代码");
    }

    private String generatePlaceholder(String prompt) {
        if (prompt == null) prompt = "";
        PromptContext ctx = extractContext(prompt);

        if (isCodeGenerationPrompt(prompt)) {
            return generatePlaceholderCode(ctx);
        }

        if (prompt.contains("系统概述")) {
            return generateOverviewPlaceholder(ctx, prompt);
        }

        if (prompt.contains("功能特点") || prompt.contains("功能特点内容管检测")
                || (prompt.contains("软著名称") && prompt.contains("软件分类")
                && prompt.contains("版本号") && (prompt.contains("技术特点选项")
                || prompt.contains("主要功能")))) {
            return generateFunctionalPlaceholder(ctx, prompt);
        }

        if (prompt.contains("开发目的")) return generatePurposePlaceholder(ctx);
        if (prompt.contains("面向领域")) return generateDomainPlaceholder(ctx);
        if (prompt.contains("主要功能")) return generateFunctionsPlaceholder(ctx);
        if (prompt.contains("技术特点")) return generateTechFeaturesPlaceholder(ctx);
        if (prompt.contains("操作手册") || prompt.contains("manual") || prompt.contains("HTML")) return generateManualPlaceholder(ctx);
        if (prompt.contains("功能菜单")
                || (prompt.contains("子菜单名称") && prompt.contains("一级菜单名称"))) {
            return generateFunctionalMenuPlaceholder(ctx, prompt);
        }
        return "【AI生成内容占位符 - 请配置对应 provider 的 API Key 以生成真实内容】";
    }

    private String generatePlaceholderCode(PromptContext ctx) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "Soft";
        String domain = ctx.domain() != null ? ctx.domain() : "";
        String language = ctx.language() != null && !ctx.language().isBlank() ? ctx.language() : "Java";
        // 按声明语言生成对应语法的代码骨架，禁止再硬编码 com.highwater 海水淡化 Java 模板
        return MultiLanguageCodeTemplate.build(language, name, domain);
    }

    // ---------- 功能菜单 ----------
    private String generateFunctionalMenuPlaceholder(PromptContext ctx, String prompt) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "业务软件";
        String hint = name + " " + (prompt != null ? prompt : "");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        boolean water = matchesDomainByContextOrHint(ctx, hint,
                "海水淡化", "反渗透", "污水处理", "污水站", "净水厂",
                "MBR", "RO膜", "超滤", "纳滤", "加药", "药剂", "COD", "氨氮",
                "排水", "水务", "水质", "水厂", "污泥", "中水回用", "苦咸水",
                "中水", "水体净化", "脱盐", "浓水");
        boolean mold = !water && matchesDomainByContextOrHint(ctx, hint, "模具", "工序", "生产", "制造", "车间", "排产", "MES", "零部件", "机加工", "压铸", "注塑");
        boolean construct = !water && !mold && matchesDomainByContextOrHint(ctx, hint, "建筑", "施工", "工地", "安全", "土建", "巡检", "隐患", "工程", "监理");
        boolean finance = !water && !mold && !construct && matchesDomainByContextOrHint(ctx, hint, "金融", "资本", "投资", "监管", "银行", "证券", "基金", "股东", "风控");
        boolean medical = !water && !mold && !construct && !finance && matchesDomainByContextOrHint(ctx, hint, "医疗", "医院", "健康", "患者", "诊疗", "病历", "医生", "医药", "门诊", "住院");
        boolean edu = !water && !mold && !construct && !finance && !medical
                && matchesDomainByContextOrHint(ctx, hint, "教育", "教学", "学生", "课程", "学校", "考试", "教师", "教务", "班级", "作业");
        boolean logistics = !water && !mold && !construct && !finance && !medical && !edu
                && matchesDomainByContextOrHint(ctx, hint, "物流", "仓储", "库存", "运输", "配送", "仓库", "拣货", "订单履约", "快递", "货运");
        boolean ecommerce = !water && !mold && !construct && !finance && !medical && !edu && !logistics
                && matchesDomainByContextOrHint(ctx, hint, "电商", "零售", "商品", "门店", "会员", "营销", "购物", "店铺", "GMV", "订单", "促销");

        Map<String, List<String>> primaryMenus;
        if (water) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("报表与决策分析", Arrays.asList("运行数据统计台账汇总", "工艺趋势多维度图表分析", "水质日报Excel报表导出", "生产报表定时邮件分发"));
            primaryMenus.put("流程审批协同", Arrays.asList("运行办件查询检索", "工艺状态变更登记", "水质异常多级审批流转", "超标整改工单闭环"));
            primaryMenus.put("业务基础管理", Arrays.asList("巡检任务分派", "药剂耗量变更登记", "设备异常上报", "工艺参数维护登记"));
            primaryMenus.put("设备与药剂协同", Arrays.asList("加药泵状态登记", "RO膜污堵巡检", "药剂库存台账维护", "污泥处置记录登记"));
        } else if (mold) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("订单与工艺管理", Arrays.asList("生产订单录入", "工艺路线配置", "BOM版本维护", "图纸与文档关联"));
            primaryMenus.put("派工与现场报工", Arrays.asList("班组与工位管理", "工序派工下发", "现场作业报工", "工时统计核算"));
            primaryMenus.put("质量检验追溯", Arrays.asList("首件检验录入", "过程巡检登记", "不合格品评审", "返工闭环跟踪"));
            primaryMenus.put("设备与物料协同", Arrays.asList("设备台账保养", "点检与故障报修", "物料齐套检查", "领料退料登记"));
        } else if (construct) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("项目与计划管理", Arrays.asList("在建项目台账", "参建单位维护", "里程碑计划配置", "形象进度填报"));
            primaryMenus.put("现场安全管理", Arrays.asList("隐患巡检录入", "整改任务派发", "复查验收确认", "风险分级预警"));
            primaryMenus.put("质量与验收管理", Arrays.asList("工序报验登记", "隐蔽工程验收", "分部分项评定", "材料进场检验"));
            primaryMenus.put("施工协同辅助", Arrays.asList("施工班组管理", "签证变更登记", "施工日志记录", "影像资料归档"));
        } else if (finance) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("机构与股东管理", Arrays.asList("机构信息台账", "股东档案维护", "持股比例管理", "变更事项审批"));
            primaryMenus.put("资本登记流转", Arrays.asList("资本实缴登记", "资本账户划转", "分红入账处理", "冻结解冻办理"));
            primaryMenus.put("风险监测预警", Arrays.asList("监测指标配置", "大额交易识别", "关联交易分析", "预警工单派发"));
            primaryMenus.put("监管报表与分析", Arrays.asList("报表口径配置", "监管报表汇总", "异常数据校验", "一键导出上报"));
        } else if (medical) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("患者与就诊管理", Arrays.asList("患者档案建档", "门诊挂号登记", "住院入出院办理", "就诊历史查询"));
            primaryMenus.put("临床诊疗协同", Arrays.asList("医生工作站", "电子病历书写", "医嘱处方开立", "检验检查申请"));
            primaryMenus.put("药事与耗材管理", Arrays.asList("药品出入库登记", "批次效期追溯", "处方合理用药审核", "高值耗材登记"));
            primaryMenus.put("质控与运营分析", Arrays.asList("病历质控检查", "临床路径执行", "门诊量统计分析", "平均住院日分析"));
        } else if (edu) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("教学组织管理", Arrays.asList("班级档案维护", "课程表编排", "教师课时配置", "教材版本管理"));
            primaryMenus.put("课堂与作业管理", Arrays.asList("备课资源共享", "课堂考勤登记", "在线作业布置", "作业批改统计"));
            primaryMenus.put("考试与成绩分析", Arrays.asList("试卷组卷管理", "在线考试组织", "成绩批量录入", "学情多维度分析"));
            primaryMenus.put("学生发展评价", Arrays.asList("综合素质评价", "成长档案记录", "品德表现登记", "学期汇总归档"));
        } else if (logistics) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("仓储基础管理", Arrays.asList("商品档案维护", "仓库库区货位", "库存余额核算", "批次与效期管理"));
            primaryMenus.put("入库与上架作业", Arrays.asList("入库预约登记", "收货验收核对", "上架货位分配", "入库单据打印"));
            primaryMenus.put("拣货与出库作业", Arrays.asList("波次拣货分配", "复核打包确认", "出库签收登记", "退货入库处理"));
            primaryMenus.put("运输与费用结算", Arrays.asList("车辆与路线配置", "运输订单派车", "在途跟踪登记", "费用结算对账"));
        } else if (ecommerce) {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("商品与库存管理", Arrays.asList("商品类目维护", "多规格SKU管理", "多仓库存核算", "价格策略配置"));
            primaryMenus.put("订单履约处理", Arrays.asList("多渠道订单接入", "智能分仓分配", "发货履约登记", "售后退换处理"));
            primaryMenus.put("营销与活动运营", Arrays.asList("优惠券发放", "满减满赠配置", "拼团活动管理", "活动效果分析"));
            primaryMenus.put("会员与精准触达", Arrays.asList("会员等级管理", "积分发放核销", "人群标签画像", "营销消息推送"));
        } else {
            primaryMenus = new LinkedHashMap<>();
            primaryMenus.put("业务基础管理", Arrays.asList("业务档案登记", "组织与岗位维护", "业务流程配置", "附件归档管理"));
            primaryMenus.put("流程审批协同", Arrays.asList("业务申请录入", "多级审批流转", "状态变更登记", "办件查询检索"));
            primaryMenus.put("报表与决策分析", Arrays.asList("统计台账汇总", "多维度图表分析", "Excel报表导出", "定时邮件分发"));
            primaryMenus.put("协同办公支撑", Arrays.asList("待办消息中心", "工作日志记录", "审批时限提醒", "移动端作业"));
        }

        List<String> primaries = new ArrayList<>(primaryMenus.keySet());
        Collections.shuffle(primaries, rnd);
        int primaryCount = Math.min(primaries.size(), 2 + rnd.nextInt(2));
        List<String> pickedPrimary = primaries.subList(0, primaryCount);

        List<String> resultLines = new ArrayList<>();
        int[] perPrimary = new int[primaryCount];
        int allocated = 0;
        for (int i = 0; i < primaryCount; i++) {
            int take = 2 + rnd.nextInt(2);
            perPrimary[i] = take;
            allocated += take;
        }
        while (allocated < 10) {
            for (int i = 0; i < primaryCount && allocated < 10; i++) {
                if (perPrimary[i] < primaryMenus.get(pickedPrimary.get(i)).size()) {
                    perPrimary[i]++;
                    allocated++;
                }
            }
            boolean anyRoom = false;
            for (int i = 0; i < primaryCount; i++) {
                if (perPrimary[i] < primaryMenus.get(pickedPrimary.get(i)).size()) anyRoom = true;
            }
            if (!anyRoom) break;
        }

        for (int i = 0; i < primaryCount; i++) {
            String p = pickedPrimary.get(i);
            List<String> subs = new ArrayList<>(primaryMenus.get(p));
            Collections.shuffle(subs, rnd);
            int take = Math.min(perPrimary[i], subs.size());
            for (int j = 0; j < take; j++) {
                resultLines.add(subs.get(j) + "（" + p + "）");
            }
        }
        int guard = 0;
        while (resultLines.size() < 10 && guard < 100) {
            guard++;
            for (String p : pickedPrimary) {
                if (resultLines.size() >= 10) break;
                List<String> subs = primaryMenus.get(p);
                String s = subs.get(rnd.nextInt(subs.size()));
                String line = s + "（" + p + "）";
                if (!resultLines.contains(line)) resultLines.add(line);
            }
        }
        if (resultLines.size() > 10) resultLines = new ArrayList<>(resultLines.subList(0, 10));
        Collections.shuffle(resultLines, rnd);

        return String.join("\n", resultLines);
    }

    // ---------- 系统概述 ----------
    private String generateOverviewPlaceholder(PromptContext ctx, String prompt) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "本软件";
        String hint = name + " " + (prompt != null ? prompt : "");
        StringBuilder sb = new StringBuilder();

        if (matchesDomainByContextOrHint(ctx, hint,
                "海水淡化", "反渗透", "污水处理", "污水站", "净水厂",
                "MBR", "RO膜", "超滤", "纳滤", "加药", "药剂", "COD", "氨氮",
                "排水", "水务", "水质", "水厂", "污泥", "中水回用", "苦咸水",
                "中水", "水体净化", "脱盐", "浓水")) {
            sb.append("面向海水淡化厂运营主管与污水处理站运行班组，围绕RO膜组件、加药泵及MBR池的实时工况、进水COD与氨氮指标进行管控，旨在解决人工抄表误差大、药剂投加依赖经验导致成本波动、膜污堵预警滞后等问题。系统以各工艺段产水量、脱盐率、污泥浓度及加药流量为业务对象，在业务基础管理中落实巡检任务分派、设备参数变更登记与异常环节上报，再由流程审批协同完成状态变更登记、多级审批流转及办件查询检索，最后在报表与决策分析环节输出统计台账、多维度图表分析结果并支持Excel报表导出与定时邮件分发，形成从现场数据采集到审批处置、再到分析反馈的闭环。该系统可有效降低水处理电耗与药剂成本，提升出水水质达标率，并为设备运维与合规留痕提供确切的数据支撑。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "建筑", "施工", "工地")) {
            sb.append(name + "面向建筑施工企业多项目并行管理场景，围绕现场作业、人材机调度、风险管控三个核心维度，构建覆盖项目立项、进度跟踪、成本核算、质量巡检、安全监督的一体化数字化作业平台。系统通过标准化业务流程与实时数据采集，解决传统模式下信息传递滞后、决策依据缺失、跨部门协同低效等问题，帮助管理层实现对项目全生命周期的可视化、可量化、可追溯管理，提升整体交付效率与合规水平。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "金融", "资本", "投资", "银行")) {
            sb.append(name + "面向金融行业资本动态监管与机构经营分析场景，构建覆盖资本登记、股东变更、资金流转、风险监测、统计上报的一体化管理体系。系统通过多源数据汇聚与规则化校验，解决传统监管中数据碎片化、口径不一致、预警滞后等问题，为监管部门和机构提供实时透明的资本视图与智能化分析能力，支撑合规管理与稳健经营。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "医疗", "健康", "医院", "患者")) {
            sb.append(name + "面向医疗机构日常运营与诊疗协作场景，围绕患者服务、诊疗流程、资源调度、质量管理四个主线，构建一体化业务协同平台。系统通过标准化流程与结构化数据采集，解决传统模式下信息孤岛、环节衔接不畅、管理追溯困难等问题，帮助医疗机构提升诊疗效率、保障医疗安全、优化服务体验，推动医院管理向精细化、智能化方向发展。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "教育", "教学", "学生", "学校")) {
            sb.append(name + "面向各类教育机构教学与管理一体化场景，围绕学生成长、课程实施、师资管理、家校协同四大主线，构建统一的数字化支撑平台。系统通过标准化流程与过程化数据沉淀，解决传统模式下信息分散、协同不畅、评价依据不足等问题，助力学校提升教学组织效率、优化资源配置、完善学习服务，推动教育管理向精细化与个性化方向持续演进。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "物流", "仓储", "库存", "运输")) {
            sb.append(name + "面向物流仓储企业供应链一体化运营场景，围绕入库、存储、出库、运输、结算五大环节，构建端到端的数字化作业与调度平台。系统通过条码识别、路径优化、库存预警等手段，解决传统模式下账物不符、响应滞后、成本核算粗放等问题，帮助企业实现仓储作业透明化、运输调度智能化、经营数据可视化，持续提升供应链整体运行效率。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "电商", "零售", "商品", "门店")) {
            sb.append(name + "面向零售企业全渠道经营与会员服务场景，围绕商品管理、订单履约、营销运营、会员服务、财务结算五大核心，构建统一的业务中台。系统通过多端数据打通与规则化运营，解决传统模式下单渠道割裂、库存不准、营销无法精准触达等问题，帮助企业实现全链路可视化管理与数据驱动决策，提升经营效率与顾客复购体验。");
        } else if (matchesDomainByContextOrHint(ctx, hint, "模具", "工序", "生产", "制造", "MES", "车间")) {
            sb.append(name + "面向模具与零部件制造企业生产现场管控场景，围绕订单派工、工序流转、设备状态、质量检验、物料齐套五大核心环节，构建一体化作业协同平台。系统通过工艺路线标准化、现场数据实时采集、异常快速响应与进度可视化，解决传统管理中工序衔接不清、交付延期、质量追溯困难等痛点，帮助企业实现排产更优、在制更清、交付更稳，持续提升模具与零部件的整体交付能力与制造管理水平。");
        } else {
            sb.append(name + "面向行业用户的专业化业务管理与协同作业场景，围绕核心业务全流程，构建涵盖数据录入、流程审批、状态跟踪、统计分析、决策支撑的一体化数字化管理平台。系统通过标准化业务规则、结构化数据沉淀和多角色协同机制，解决传统模式下信息分散、流转低效、追溯困难、决策依据不足等问题，帮助用户实现业务全过程的可视化、可量化、可追溯管理，持续提升整体运营效率和管理水平。");
        }
        String overview = sb.toString();
        if (overview.length() > 260) {
            int idx = overview.lastIndexOf("。", 220);
            if (idx > 40) overview = overview.substring(0, idx + 1);
        }
        return overview;
    }

    // ---------- 功能特点（输出 16 行） ----------
    private String generateFunctionalPlaceholder(PromptContext ctx, String prompt) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "软件著作权软件";
        String version = ctx.version() != null && !ctx.version().isBlank() ? ctx.version() : "V1.0";
        String category = ctx.category() != null && !ctx.category().isBlank() ? ctx.category() : "应用软件";
        String language = ctx.language() != null && !ctx.language().isBlank() ? ctx.language() : "java";
        int codeLines = ThreadLocalRandom.current().nextInt(8000, 20001);

        String devHardware = "Intel Core i7-12700/16GB 内存/512GB SSD 以上配置的台式计算机及相关外设。";
        String runHardware = "Intel Xeon E5-2680 v4/32GB 内存/1TB SSD 以上配置的服务器主机及千兆网络设备。";
        String devOs = "Windows 10/11 64位专业版操作系统，支持本地虚拟化与容器开发环境。";
        String devTools = LanguageEnvironment.devTools(language);
        String runPlatform = LanguageEnvironment.runPlatform(language);
        String runSupport = LanguageEnvironment.supportStack(language);
        String purpose;
        String domain;
        String mainFunctions;
        String techFeatures;
        String techOptions;

        String hintText = name + " " + (prompt != null ? prompt : "");
        boolean water = matchesDomainByContextOrHint(ctx, hintText,
                "海水淡化", "反渗透", "污水处理", "污水站", "净水厂",
                "MBR", "RO膜", "超滤", "纳滤", "加药", "药剂", "COD", "氨氮",
                "排水", "水务", "水质", "水厂", "污泥", "中水回用", "苦咸水",
                "中水", "水体净化", "脱盐", "浓水");
        boolean mold = !water && matchesDomainByContextOrHint(ctx, hintText, "模具", "工序", "生产", "制造", "车间", "排产", "MES", "零部件", "机加工", "压铸");
        boolean construct = !water && !mold && matchesDomainByContextOrHint(ctx, hintText, "建筑", "施工", "工地", "安全", "土建", "巡检", "隐患", "工程", "监理");
        boolean finance = !water && !mold && !construct && matchesDomainByContextOrHint(ctx, hintText, "金融", "资本", "投资", "监管", "银行", "证券", "基金", "股东", "风控");
        boolean medical = !water && !mold && !construct && !finance && matchesDomainByContextOrHint(ctx, hintText, "医疗", "医院", "健康", "患者", "诊疗", "病历", "医生", "医药", "门诊", "住院");
        boolean edu = !water && !mold && !construct && !finance && !medical
                && matchesDomainByContextOrHint(ctx, hintText, "教育", "教学", "学生", "课程", "学校", "考试", "教师", "教务", "班级", "作业");
        boolean logistics = !water && !mold && !construct && !finance && !medical && !edu
                && matchesDomainByContextOrHint(ctx, hintText, "物流", "仓储", "库存", "运输", "配送", "仓库", "拣货", "订单履约", "快递", "货运");
        boolean ecommerce = !water && !mold && !construct && !finance && !medical && !edu && !logistics
                && matchesDomainByContextOrHint(ctx, hintText, "电商", "零售", "商品", "门店", "会员", "营销", "购物", "店铺", "GMV", "订单", "促销");

        if (water) {
            purpose = "为海水淡化厂与污水处理站提供从工艺运行、水质指标管控到报表输出的一体化数字化能力。";
            domain = "海水淡化运营管理、污水处理厂工艺管控与水质合规监测领域。";
            devHardware = "工业级触摸屏工控机、在线多参数水质分析仪、加药泵控制器及 4G DTU 采集网关等配套设备。";
            runHardware = "中控室工控机、巡检手持平板、PLC 控制柜与 Modbus 仪表接入设备。";
            mainFunctions = "系统面向海水淡化及污水处理全工艺链，围绕RO膜组件、加药泵、MBR池、沉淀池等核心设备建立主数据，支持设备台账、工况登记、维保计划与备件管理；通过在线采集接口与人工补录双通道汇聚进水COD、氨氮、浊度、产水量、脱盐率、回收率、加药流量、污泥浓度等关键指标，形成逐时逐刻的水质与工艺运行快照；依托巡检任务分派、工艺参数变更登记与异常上报模块，把巡检点、巡检人、发现问题、整改建议与处置结果在线登记，保证每项运行动作责任到人、留痕可查；针对超标、泵组异常停机、膜压差攀升等情况支持状态变更登记、多级审批流转与办件查询检索，实现从发现到处置的闭环审批；最终通过统计台账汇总、多维度图表分析、Excel报表导出、定时邮件分发把产水、药耗、能耗、合格率向运营主管与管理层定期呈现，为工艺调优、成本控制、合规检查与应急处置提供依据，整体提升海水淡化与污水处理运行的稳定性、经济性与可追溯性。";
            techFeatures = "系统集成在线水质采集与 PLC 通信能力，支持多工艺段指标阈值预警与缺失数据自动补录，实现从现场到管理的闭环联动。";
            techOptions = "物联网软件";
        } else if (mold) {
            purpose = "为模具及零部件制造企业提供从订单到交付的全程工序数字化管控能力。";
            domain = "离散制造业、模具加工车间与生产过程执行管理领域。";
            mainFunctions = "系统围绕模具生产订单建立完整的工序流程主数据，支持订单下发、工艺路线配置、派工到岗、作业报工、工时统计与进度回写，满足多型号模具并行生产的精细化管理需要；通过设备台账、点检保养、状态监控与故障报修模块，实现关键加工设备全生命周期管理，降低非计划停机对交期的影响；依据图纸与工艺要求建立检验标准，支持首检、巡检、末检在线录入与不合格品评审、返工闭环，保证每道工序质量可追溯；同时集成物料齐套、领料退料与在制品流转功能，实时掌握车间在制数量、工序瓶颈与缺料情况，为排产调度提供依据；结合数据看板与多维度统计分析，面向班组长、工艺人员与管理人员提供订单进度、设备OEE、质量合格率、人员绩效等可视化指标，辅助快速决策与持续改进，整体提升模具交付准时率与车间运行效率。";
            techFeatures = "系统采用前后端分离架构，后端与数据访问层解耦，前端基于组件化设计，支持响应式看板与实时数据刷新，集成消息队列保障报工事件可靠处理。";
            techOptions = "物联网软件";
        } else if (construct) {
            purpose = "为建筑施工企业打造现场安全、进度与质量一体化的数字化管理工具。";
            domain = "建筑工程施工管理、工地安全监督与现场协同作业领域。";
            mainFunctions = "系统建立工程项目主数据，支持多项目并行管理，在线录入项目基本信息、参建单位、关键节点计划与里程碑，实时掌握各在建项目整体概况；围绕现场施工过程，提供进度填报、形象进度上传、工序报验与隐蔽验收功能，关键节点支持移动端现场拍照确认并同步至PC端台账；通过安全隐患巡检、整改、复查的闭环流程，结合整改时限提醒与超时预警，有效降低现场违规风险；集成质量检查、分部分项评定与问题整改追踪功能，做到每项质量问题责任到人、过程留痕、结果可查；同时提供合同付款、签证变更、材料进场与领用登记等辅助模块，支撑项目成本分析与资金计划；最终通过综合驾驶舱展示项目进度、安全、质量、成本多维指标，满足管理层决策和总部监管需要。";
            techFeatures = "系统采用B/S架构，后端集成统一身份鉴权，前端使用响应式布局与移动端H5双端适配，集成对象存储保存现场影像，支持离线缓存与数据补录。";
            techOptions = "智慧城市软件";
        } else if (finance) {
            purpose = "为金融监管与机构经营提供资本登记、流转监测与合规分析一体化支撑。";
            domain = "金融行业资本监管、股东管理与合规经营分析领域。";
            mainFunctions = "系统建立机构、股东、资本账户统一主数据，支持资本实缴登记、股东持股比例维护、变更事项线上审批与留痕，保证资本数据真实可信；围绕资金流转全链路，提供入账、划转、分红、冻结、解冻等业务办理，每笔业务支持附件上传、复核与审批，满足监管对交易可追溯要求；通过指标建模与规则引擎实现风险监测，对资本不足、异常大额、关联交易集中等情况自动生成预警工单并派发处置；面向多维度监管报表提供自动化汇总、校验与一键导出能力，减少人工汇总与口径不一致问题；同时提供数据权限分级、操作日志审计、关键数据加密等安全机制，确保敏感信息可管可控；最终通过监管驾驶舱向管理人员展示机构资本概况、风险分布、处置进度与趋势预测，支撑精准施策。";
            techFeatures = "系统采用微服务可扩展设计，注册发现与配置中心组件化，数据库采用主从复制保障高可用，集成Redis与消息队列提升高频查询与任务调度性能。";
            techOptions = "金融软件";
        } else if (medical) {
            purpose = "为医疗机构提供以患者为中心的诊疗协同与质量持续改进平台。";
            domain = "医院信息一体化、临床业务协同与医疗质量管理领域。";
            mainFunctions = "系统建立患者档案主索引，支持门诊与住院场景统一身份识别，汇聚历次就诊、诊断、用药、检查检验等信息形成360度视图，便于临床快速获取完整病史；围绕诊疗主线提供医生工作站、电子病历书写、医嘱开立、处方审核、检查检验申请与结果回传闭环，关键环节内置规则校验与合理用药提醒；依托临床路径与质控规则对病历时限、诊断完整性、医嘱依从性进行自动核查，实时生成问题清单与整改任务；集成排班、床位、手术、输血、高值耗材等辅助模块，保障资源调度顺畅与使用可追溯；同时支持院内多系统集成对接与HL7标准消息交换，减少重复录入；最终通过综合运营看板面向管理者展示门诊量、住院占床、检查阳性率、平均住院日等关键指标，辅助精细化运营决策。";
            techFeatures = "系统采用前后端分离架构，后端集成统一安全框架保障患者数据合规，数据库结合PostgreSQL与脱敏策略，前端支持多科室差异化交互模板。";
            techOptions = "医疗软件";
        } else if (edu) {
            purpose = "为教育机构提供教学、教务与家校协同一体化的数字化管理支撑。";
            domain = "中小学及职业院校教学管理、学生发展评价与家校协同领域。";
            mainFunctions = "系统建立学生、班级、教师、课程、教材等基础主数据，支持学期学年切换与批量导入维护，保证全校教学组织关系清晰统一；围绕教学主线提供课程表编排、备课资源共享、在线作业布置与自动批改、课堂考勤与互动记录，满足日常授课与分层教学需要；依托考试与成绩模块实现组卷、在线考试、成绩录入与多维度质量分析，可按班级、学科、知识点输出学情画像，辅助教师改进教学策略；提供学生综合素质评价与成长档案记录，对品德、学业、身心、艺术、实践进行过程化采集与学期汇总；集成家校通知、请假审批、家长端作业查看与沟通留言功能，打通学校与家庭的协同渠道；同时提供校园 OA、资产、招生报名等辅助模块，支撑校务一站式办理；最终通过校领导驾驶舱展示教学运行、学生发展、师资配置等全局指标。";
            techFeatures = "系统采用B/S架构与响应式前端适配，后端集成分布式缓存提升高并发访问能力，集成在线文档与媒体资源服务支持教学素材高效流转。";
            techOptions = "教育软件";
        } else if (logistics) {
            purpose = "为物流仓储企业打造库存、订单与运输一体化的供应链作业协同平台。";
            domain = "仓储物流运营、订单履约与运输调度优化领域。";
            mainFunctions = "系统建立商品、仓库、库区、货位、承运商等基础主数据，支持多仓多货主统一管理与库存余额实时核算；围绕仓储作业提供入库预约、收货验收、上架分配、波次拣货、复核打包、出库签收全流程条码化操作，异常情况可在线登记并触发复核；支持多种盘点方式与库存调整流程，结合效期预警、批次追溯与先进先出策略降低积压与损耗；面向订单履约提供多渠道订单接入、自动拆单合单、库存锁定、取消退款等能力，保证订单处理快速准确；集成运输管理模块实现路线规划、车辆派车、在途跟踪、签收回单与费用结算，结合路径优化减少空驶与超时；同时提供可视化监控大屏展示当日进出库、订单时效、车辆在途、库容占比等指标；支持多角色权限与操作审计，确保关键流程可控可追溯。";
            techFeatures = "系统采用微服务化与事件驱动设计，集成条码识别、拣货路径优化与消息推送能力，数据库分库分表支持海量订单高并发写入与查询。";
            techOptions = "云计算软件";
        } else if (ecommerce) {
            purpose = "为零售企业打造商品、订单、营销、会员一体化的全渠道经营中台。";
            domain = "全渠道零售经营、会员营销与电商运营管理领域。";
            mainFunctions = "系统建立商品、类目、品牌、多规格SKU与多仓库库存统一主数据，支持多平台店铺商品上架、价格同步与库存共享，避免超卖与重复维护；围绕订单提供多渠道接入、智能分仓、自动拆合单、异常拦截、发货履约、售后退换全链路闭环管理，结合发货时限监控提升履约准时率；内置营销引擎支持满减、满赠、优惠券、组合套餐、拼团、秒杀等多种玩法，可按人群、时段、商品维度灵活配置并提供活动效果分析；通过会员体系实现等级、积分、储值、标签与画像管理，结合消费行为开展精准触达与复购运营；集成财务管理模块完成订单对账、开票、成本核算与渠道结算，确保经营数据准确可信；同时提供可视化经营驾驶舱展示GMV、订单量、转化率、客单价、复购率、营销ROI等核心指标，辅助运营团队持续优化策略。";
            techFeatures = "系统采用前后端分离与多租户扩展架构，集成缓存、搜索引擎与实时计算能力，保障大促期间高并发访问稳定与数据秒级汇总。";
            techOptions = "大数据软件";
        } else {
            purpose = "为行业用户提供核心业务全流程数字化与协同办公的一体化支撑能力。";
            domain = "行业信息化建设、业务流程数字化与企业协同管理领域。";
            mainFunctions = "系统建立统一的业务主数据与组织权限体系，支持多部门、多角色按职责分工协作，关键业务对象具备完整的属性档案与版本管理，保障基础信息一致可信；围绕核心业务流程提供在线录入、多级审批、状态流转、附件归档与查询检索功能，每个环节支持条件分支、时限提醒与超时升级，确保流程规范高效；依据业务规则自动生成统计台账与报表，支持多维度筛选、图表展示、Excel导出与定时分发，为管理决策提供可靠依据；集成消息通知、待办中心、工作日志与协作留言等能力，结合移动端使用场景支持异地办公与现场作业；通过操作日志、数据权限与审计追踪保障业务过程安全合规；最终形成业务办理、流程管控、数据分析、协同支撑一体化的综合管理能力，持续提升企业运营效率与精细化管理水平。";
            techFeatures = "系统采用前后端分离的模块化架构，后端集成统一权限控制，前端使用组件库快速构建表单与图表，内置工作流与规则引擎提升业务灵活度。";
            techOptions = "信息安全软件";
        }

        int mfCleanLen = mainFunctions.replaceAll("[\\pP\\s]", "").length();
        if (mfCleanLen < 620) {
            String tail = "同时系统提供灵活可配置的业务规则引擎与表单扩展能力，业务管理人员无需依赖开发人员即可根据组织变化快速调整流程节点、数据字段与统计口径，并能通过标准开放接口与周边ERP、MES、OA、财务等系统实现数据对接与双向同步，确保信息架构统一、口径一致、数据可信，为后续业务迭代与数字化深化奠定稳定基础。";
            mainFunctions = mainFunctions + tail;
            mfCleanLen = mainFunctions.replaceAll("[\\pP\\s]", "").length();
        }
        if (mfCleanLen > 820) {
            int targetClean = 780;
            int clean = 0;
            int cut = -1;
            for (int i = 0; i < mainFunctions.length(); i++) {
                char c = mainFunctions.charAt(i);
                boolean isPunctOrSpace = Character.isWhitespace(c) || Character.getType(c) == Character.START_PUNCTUATION
                        || Character.getType(c) == Character.END_PUNCTUATION
                        || Character.getType(c) == Character.OTHER_PUNCTUATION
                        || Character.getType(c) == Character.CONNECTOR_PUNCTUATION
                        || Character.getType(c) == Character.DASH_PUNCTUATION
                        || Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION
                        || Character.getType(c) == Character.INITIAL_QUOTE_PUNCTUATION
                        || c == ';' || c == '；' || c == ',' || c == '，' || c == ':' || c == '：' || c == '。' || c == '!';
                if (!isPunctOrSpace) clean++;
                if (clean >= targetClean && mainFunctions.charAt(i) == '。') {
                    cut = i;
                    break;
                }
            }
            if (cut > 20) mainFunctions = mainFunctions.substring(0, cut + 1);
        }

        if (!techFeatures.endsWith("。")) techFeatures = techFeatures + "。";
        if (techFeatures.length() > 110) {
            int cut = techFeatures.lastIndexOf("。", 96);
            if (cut > 10) techFeatures = techFeatures.substring(0, cut + 1);
        }

        return "软著名称：" + name + "\n"
                + "版本号：" + version + "\n"
                + "软件分类：" + category + "\n"
                + "开发的硬件环境：" + devHardware + "\n"
                + "运行的硬件环境：" + runHardware + "\n"
                + "开发该软件的操作系统：" + devOs + "\n"
                + "软件开发环境/开发工具：" + devTools + "\n"
                + "该软件的运行平台/操作系统：" + runPlatform + "\n"
                + "软件运行支撑环境/支持软件：" + runSupport + "\n"
                + "编程语言：" + language + "\n"
                + "源程序量：" + codeLines + "\n"
                + "开发目的：" + purpose + "\n"
                + "面向领域：" + domain + "\n"
                + "主要功能：" + mainFunctions + "\n"
                + "技术特点：" + techFeatures + "\n"
                + "软件的技术特点选项：" + techOptions;
    }

    private String generatePurposePlaceholder(PromptContext ctx) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "本软件";
        if (matchesDomain(ctx, "建筑", "施工")) {
            return name + "旨在为建筑施工企业提供一站式的安全隐患巡检管控解决方案，通过移动化巡检、实时数据采集和智能预警机制，实现施工现场安全管理的数字化、精细化管控。系统核心关注点包括：安全隐患实时监控、巡检流程标准化、整改跟踪闭环管理，以及多项目协同指挥调度能力。";
        }
        if (matchesDomain(ctx, "金融", "资本", "投资")) {
            return name + "旨在为金融监管部门和企业提供资本动态监控解决方案，通过实时数据采集和智能分析，实现企业实缴资本的透明化、可追溯管理。系统核心关注点包括：资本数据实时监控、股东权益管理、风险预警机制，以及多维度统计分析能力。";
        }
        if (matchesDomain(ctx, "医疗", "健康")) {
            return name + "旨在为医疗机构提供高效的医疗管理解决方案，通过信息化手段优化诊疗流程，提升医疗服务质量。系统核心关注点包括：患者信息管理、诊疗流程优化、医疗数据安全存储，以及多科室协同工作能力。";
        }
        if (matchesDomain(ctx, "教育", "教学")) {
            return name + "旨在为教育机构提供现代化的教学管理解决方案，通过数字化平台提升教学效率和学习效果。系统核心关注点包括：课程管理、学生信息管理、在线教学支持，以及教学数据分析能力。";
        }
        if (matchesDomain(ctx, "物流", "仓储")) {
            return name + "旨在为物流仓储企业提供高效的供应链管理解决方案，通过自动化数据采集和智能调度优化物流运营流程。系统核心关注点包括：库存实时监控、订单跟踪管理、仓储作业优化，以及运输路径规划能力。";
        }
        if (matchesDomain(ctx, "电商", "零售")) {
            return name + "旨在为零售企业提供全渠道电商管理解决方案，通过统一平台管理多端业务，提升运营效率。系统核心关注点包括：商品管理、订单处理、客户关系维护，以及营销数据分析能力。";
        }
        return name + "旨在为行业用户提供专业化的软件解决方案，通过先进的技术架构和模块化设计实现核心业务流程的数字化管理。系统核心关注点包括：数据安全管理、多用户协同、灵活的工作流配置，以及实时的业务数据监控与分析能力。";
    }

    private String generateDomainPlaceholder(PromptContext ctx) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "本软件";
        if (matchesDomain(ctx, "建筑", "施工")) {
            return name + "主要面向建筑施工行业的安全管理领域，适用于各类建设工程项目的日常巡检管控。目标用户群体包括：项目经理、安全员、监理工程师、施工班组长等。系统可广泛应用于施工现场安全巡检、隐患整改跟踪、施工人员管理等业务场景。";
        }
        if (matchesDomain(ctx, "金融", "资本", "投资")) {
            return name + "主要面向金融监管和企业资本管理领域，适用于各类企业的资本动态监控。目标用户群体包括：监管人员、企业财务人员、股东、审计人员等。系统可广泛应用于资本实缴监控、股东权益管理、风险预警分析等业务场景。";
        }
        if (matchesDomain(ctx, "医疗", "健康")) {
            return name + "主要面向医疗卫生领域，适用于各类医疗机构的日常运营管理。目标用户群体包括：医生、护士、患者、医院管理人员等。系统可广泛应用于患者管理、诊疗服务、药品管理等业务场景。";
        }
        if (matchesDomain(ctx, "教育", "教学")) {
            return name + "主要面向教育教学领域，适用于各类学校和教育机构的教学管理。目标用户群体包括：教师、学生、家长、教务管理人员等。系统可广泛应用于课程管理、在线教学、成绩管理等业务场景。";
        }
        if (matchesDomain(ctx, "物流", "仓储")) {
            return name + "主要面向物流仓储领域，适用于各类仓储物流企业的日常运营。目标用户群体包括：仓库管理员、物流调度员、配送人员、管理人员等。系统可广泛应用于库存管理、订单处理、运输跟踪等业务场景。";
        }
        return name + "面向企业级应用市场，主要服务于行业用户的数字化转型需求。目标用户群体包括：企业管理人员、业务操作人员、系统管理员等。系统可广泛应用于业务流程管理、数据采集与分析、协同办公等业务场景。";
    }

    private String generateFunctionsPlaceholder(PromptContext ctx) {
        return generateDomainPlaceholder(ctx) + "主要功能已在功能特点模块中详细展开，涵盖业务主数据、流程审批、报表分析、协同办公等核心能力。";
    }

    private String generateTechFeaturesPlaceholder(PromptContext ctx) {
        return "系统采用前后端分离的模块化架构，后端基于 Spring Boot 与 MyBatis-Plus，前端采用 React + TypeScript 组件化开发，集成 Redis 缓存、对象存储与 JWT 鉴权，支持高并发访问与横向扩展。";
    }

    private String generateManualPlaceholder(PromptContext ctx) {
        String name = ctx.softwareName() != null ? ctx.softwareName() : "本软件";
        return "<h2>" + name + " 操作手册（占位）</h2>\n"
                + "<p>一、登录：打开浏览器访问系统地址，输入账号/密码并完成图形校验。</p>\n"
                + "<p>二、功能入口：登录后进入首页，左侧导航栏点击对应业务菜单进入操作界面。</p>\n"
                + "<p>三、录入与查询：在业务页面点击新增/查询按钮，完成表单填写与条件筛选后提交。</p>\n"
                + "<p>四、审批与导出：在待办或流程页面点击审批、导出等操作按钮完成处理，Excel 导出与邮件分发由报表菜单统一管理。</p>\n"
                + "<p>五、注意事项：请勿在公共终端保留登录凭证，敏感操作将记录操作日志。</p>\n"
                + "<p>【以上为占位文案——配置真实 AI Key 后可生成完整的图文操作手册 HTML。】</p>";
    }

    // =========================================================================
    // 自定义错误处理器：先把远端响应 body 读出来，解析 JSON 里的 error.message 再抛
    // 这样日志里能看到服务端到底说了什么，不会再出现 "401 Authorization Required: [no body]"
    // =========================================================================
    static class DetailedResponseErrorHandler extends DefaultResponseErrorHandler {
        private final ObjectMapper objectMapper;

        DetailedResponseErrorHandler(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatusCode code = response.getStatusCode();
            byte[] body = response.getBody().readAllBytes();
            String text = body != null && body.length > 0 ? new String(body) : "";
            String msg = text;
            try {
                if (!text.isBlank()) {
                    JsonNode root = objectMapper.readTree(text);
                    JsonNode errMsg = root.at("/error/message");
                    JsonNode errCode = root.at("/error/code");
                    JsonNode reqId = root.at("/request_id");
                    JsonNode plainMsg = root.at("/message");
                    StringBuilder hint = new StringBuilder();
                    if (!errMsg.isMissingNode() && !errMsg.asText("").isBlank()) {
                        hint.append(" error.message=").append(errMsg.asText(""));
                    } else if (!plainMsg.isMissingNode() && !plainMsg.asText("").isBlank()) {
                        hint.append(" message=").append(plainMsg.asText(""));
                    }
                    if (!errCode.isMissingNode() && !errCode.asText("").isBlank()) {
                        hint.append(" error.code=").append(errCode.asText(""));
                    }
                    if (!reqId.isMissingNode() && !reqId.asText("").isBlank()) {
                        hint.append(" request_id=").append(reqId.asText(""));
                    }
                    if (hint.length() > 0) msg = hint.toString();
                }
            } catch (Exception ignore) {
                // JSON 解析失败就用原始 body
            }
            throw new RuntimeException(
                    "AI 服务端返回 " + code + " ，远端响应：" + msg,
                    new org.springframework.web.client.HttpClientErrorException(code, text.isEmpty() ? "(empty body)" : text));
        }
    }
}
