package com.company.ruanzhu.generate.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 把 AI 生成的"功能菜单"脏结果规范化为：
 *   恰好 10 行，每行格式 子菜单名称（一级菜单名称）
 *   一级菜单个数 3~5 个之间，每个一级菜单下 2~3 个子菜单（3+3+2+2、3+2+3+2 等排列）
 *   不出现禁用词、不出现带冒号的说明句
 *
 * 即使 AI 返回的是 8 条、带一大段"用户认证与权限管理：..."这类说明文字，
 * 或者返回了 markdown 代码块，也保证输出恰好 10 条合规菜单。
 */
public final class FunctionMenuNormalizer {

    private FunctionMenuNormalizer() {}

    // 用户明确禁用 / 第一张截图里经常被AI自作主张加进来的词
    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
            "用户管理", "用户认证", "登录", "注册", "权限", "角色",
            "系统设置", "系统配置", "软件说明", "使用说明",
            "数据分析", "报表生成", "报表分析", "日志", "监控",
            "接口集成", "工作流引擎", "异常告警", "异常处理",
            "RBAC", "用户中心"
    );

    // 通用子菜单动词（用于补齐不足 10 条时），确保名字是"动词开头可操作"样式，而不是xxx管理功能：
    private static final List<String> GENERIC_SUB_PREFIXES = Arrays.asList(
            "订单录入", "单据查询", "流程审批", "任务分派", "进度跟踪",
            "数据登记", "配置维护", "统计汇总", "异常上报", "审核确认",
            "状态更新", "文件归档", "记录查询", "计划排程", "参数设置",
            "核对校验", "变更登记", "下发执行", "质检登记", "资料导入"
    );

    // 通用一级菜单（无key时兜底），3~5个
    private static final List<String> GENERIC_PRIMARY = Arrays.asList(
            "业务基础管理", "流程协同管理", "业务执行管理",
            "资源与资料管理", "质量与风控管理"
    );

    /**
     * 根据用户提示词判断：是否是"功能菜单"这一类生成。
     *
     * 判定规则（很严格，避免把"系统概述/功能特点"之类的长文误判定成菜单）：
     * 1) 必须同时命中"子菜单名称" + "一级菜单名称" 这两个明确的菜单格式要求标记词
     *    （我们的功能菜单 TAB 里的提示词原文是严格带这两句的）
     * 2) 提示词里绝不能出现"系统概述"、"功能特点"、"概述"、"约200字"、"一段连续文字"等
     *    这些是概述/特点用的特征词，出现就一定不是菜单
     */
    public static boolean isMenuPrompt(String prompt) {
        if (prompt == null) return false;
        // 明确的"不是功能菜单"的标记词（概述/功能特点的特征）——命中一个就直接判否
        List<String> overviewMarkers = Arrays.asList(
                "系统概述", "功能特点", "一段连续文字", "约200字",
                "开篇描述", "主要功能：", "技术特点：", "软著名称"
        );
        for (String marker : overviewMarkers) {
            if (prompt.contains(marker)) return false;
        }
        // 严格判定：必须同时有"子菜单名称"和"一级菜单名称"两处格式要求描述
        return prompt.contains("子菜单名称") && prompt.contains("一级菜单名称");
    }

    /**
     * 规范化。
     *
     * @param rawOutput    AI 原始输出
     * @param softwareName 提示词里的软件名（用于领域识别，优先匹配行业相关的补齐词）
     * @param prompt       用户原始 prompt，用于抽取软件名上下文
     * @return 恰好 10 行，每行格式为 子菜单（一级菜单）
     */
    public static String normalize(String rawOutput, String softwareName, String prompt) {
        String text = rawOutput == null ? "" : rawOutput;
        // 去 markdown 代码块
        text = text.replaceAll("(?s)^\\s*```[\\w]*\\s*\\n?", "").replaceAll("(?s)\\n?```\\s*$", "").trim();

        List<String> lines = extractValidMenuLines(text);
        lines = filterForbidden(lines);
        lines = deduplicate(lines);

        // 如果有效行数不足 10，且文本里还存在"冒号说明行"的垃圾内容（例：用户认证与权限管理：实现...）
        // 再试一次：用启发式从说明行里挖菜单（"实现xxx、yyy、zzz 等功能" 这种枚举）
        if (lines.size() < 10) {
            List<String> more = tryMineFromDescriptionLines(text);
            more = filterForbidden(more);
            for (String m : more) {
                if (lines.size() >= 10) break;
                if (!lines.contains(m)) lines.add(m);
            }
        }

        // 补齐到 10 条（根据软件名方向做行业匹配，否则用通用）
        lines = padToTen(lines, softwareName == null ? detectSoftwareName(prompt) : softwareName);

        // 截断到 10
        if (lines.size() > 10) {
            lines = new ArrayList<>(lines.subList(0, 10));
        }

        return String.join("\n", lines);
    }

    /**
     * 从原始文本提取所有已经符合 「子菜单（一级菜单）」 格式的行。
     */
    private static List<String> extractValidMenuLines(String text) {
        List<String> out = new ArrayList<>();
        String[] rawLines = text.split("\\r?\\n");
        for (String raw : rawLines) {
            String l = raw.trim();
            if (l.isEmpty()) continue;
            // 去前缀序号："1." "1、" "1)" "1）" 等
            l = l.replaceFirst("^\\s*[\\d一二三四五六七八九十]+[\\.、\\)）]\\s*", "");
            // 去项目符号："- " "* "
            l = l.replaceFirst("^[-*·•]+\\s*", "");
            l = l.trim();
            if (l.isEmpty()) continue;
            // 已经是标准格式：子菜单（一级菜单）或 子菜单(一级菜单)
            if (l.matches("^[^（）()\\r\\n]{1,30}[（(][^（）()\\r\\n]{1,20}[)）]\\s*$")) {
                // 统一中文括号
                l = l.replace('(', '（').replace(')', '）');
                // 去除尾部空格
                l = l.replaceAll("（\\s+", "（").replaceAll("\\s+）", "）");
                out.add(l);
            }
        }
        return out;
    }

    /**
     * 从"xxx功能：支持A、B、C，提供D，实现E 等功能"这种垃圾说明行中，
     * 尝试抽取 A/B/C/D/E 作为子菜单名，并把冒号左边的短语归为一级菜单。
     */
    private static List<String> tryMineFromDescriptionLines(String text) {
        List<String> out = new ArrayList<>();
        String[] rawLines = text.split("\\r?\\n");
        for (String raw : rawLines) {
            String l = raw.trim();
            if (l.isEmpty()) continue;
            // 只处理带冒号且含顿号/逗号枚举的行
            if (!l.matches(".*[：:].+")) continue;
            int firstColon = Math.min(
                    l.indexOf('：') >= 0 ? l.indexOf('：') : Integer.MAX_VALUE,
                    l.indexOf(':') >= 0 ? l.indexOf(':') : Integer.MAX_VALUE
            );
            if (firstColon == Integer.MAX_VALUE) continue;
            String left = l.substring(0, firstColon).trim();
            String right = l.substring(firstColon + 1).trim();
            if (left.isEmpty() || right.isEmpty()) continue;
            // left 里不要出现"xxx管理功能"之类末尾词，取第一个名词短语作为一级菜单
            String primary = extractPrimaryFromLabel(left);
            if (primary == null || isForbidden(primary)) continue;
            // 从 right 中按 顿号/逗号/分号 切分候选子菜单
            String[] parts = right.split("[、，,;；]|和|与");
            for (String p : parts) {
                String sub = p
                        .replaceAll("^.*?(支持|提供|实现|包含|含|具备|完成|执行|处理|监控|记录|统计|维护|配置)", "")
                        .replaceAll("等相关功能.*$", "")
                        .replaceAll("等功能.*$", "")
                        .replaceAll("等业务.*$", "")
                        .replaceAll("等.*$", "")
                        .trim();
                if (sub.isEmpty() || sub.length() < 2 || sub.length() > 24) continue;
                if (isForbidden(sub)) continue;
                // 纯标点或数字跳过
                if (sub.matches("^[\\p{Punct}0-9\\s]+$")) continue;
                String candidate = sub + "（" + primary + "）";
                if (!out.contains(candidate)) out.add(candidate);
            }
        }
        return out;
    }

    private static String extractPrimaryFromLabel(String left) {
        String s = left;
        // 去序号前缀
        s = s.replaceFirst("^\\s*[\\d一二三四五六七八九十]+[\\.、\\)）]\\s*", "");
        s = s.replaceAll("管理功能$", "");
        s = s.replaceAll("功能模块$", "");
        s = s.replaceAll("模块$", "");
        s = s.replaceAll("功能$", "");
        s = s.trim();
        if (s.isEmpty()) return null;
        // 结尾如果还不是"管理/管控/追溯/协同/调度/执行"等词，加一个
        if (!s.matches(".*(管理|管控|追溯|协同|调度|执行|维护|处理|配置|运营|审批)$")) {
            s = s + "管理";
        }
        return s;
    }

    private static boolean isForbidden(String text) {
        if (text == null) return true;
        for (String k : FORBIDDEN_KEYWORDS) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private static List<String> filterForbidden(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            if (!isForbidden(l)) out.add(l);
        }
        return out;
    }

    private static List<String> deduplicate(List<String> lines) {
        Set<String> seen = new LinkedHashSet<>();
        for (String l : lines) seen.add(l);
        return new ArrayList<>(seen);
    }

    /**
     * 补齐到恰好 10 条，并强制一级菜单数量分布（3+3+2+2 优先等）。
     */
    private static List<String> padToTen(List<String> lines, String softwareName) {
        // 先统计现有一级菜单分布
        Map<String, List<String>> bucket = new HashMap<>();
        for (String l : lines) {
            String primary = parsePrimary(l);
            bucket.computeIfAbsent(primary == null ? "其他" : primary, k -> new ArrayList<>()).add(l);
        }

        List<String> primaries = new ArrayList<>(bucket.keySet());
        // 如果没有一级菜单，给个默认集合
        if (primaries.isEmpty()) {
            primaries = buildPrimaryPool(softwareName);
        }
        // 如果一级菜单数量 >5，只保留子菜单数最多的前 5 个
        if (primaries.size() > 5) {
            primaries.sort((a, b) -> Integer.compare(bucket.getOrDefault(b, Collections.emptyList()).size(),
                    bucket.getOrDefault(a, Collections.emptyList()).size()));
            primaries = new ArrayList<>(primaries.subList(0, 5));
        }
        // 如果一级菜单 <3，补到 3~4 个
        List<String> poolAll = buildPrimaryPool(softwareName);
        int i = 0;
        while (primaries.size() < 3 && i < poolAll.size()) {
            String cand = poolAll.get(i++);
            if (!primaries.contains(cand)) primaries.add(cand);
        }

        // 决定每个一级菜单需要多少个子菜单（目标总数=10，每个在2~3之间）
        int n = primaries.size();
        int[] counts = distributeCountsTo(n, 10);

        // 生成补齐
        List<String> subPrefixPool = buildSubPrefixPool(softwareName);
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        List<String> result = new ArrayList<>();
        int subCursor = rand.nextInt(subPrefixPool.size()); // 随机起点，避免每次都是同样的

        for (int idx = 0; idx < n; idx++) {
            String primary = primaries.get(idx);
            List<String> existing = bucket.getOrDefault(primary, Collections.emptyList());
            int target = counts[idx];
            int take = Math.min(existing.size(), target);
            for (int k = 0; k < take; k++) {
                result.add(existing.get(k));
            }
            // 不足的补齐
            int need = target - take;
            for (int k = 0; k < need; k++) {
                int tries = 0;
                while (tries < 200) {
                    String sub = subPrefixPool.get(subCursor % subPrefixPool.size());
                    subCursor++;
                    String candidate = sub + "（" + primary + "）";
                    if (!result.contains(candidate)) {
                        result.add(candidate);
                        break;
                    }
                    tries++;
                }
                // 极端兜底：加序号防重
                if (tries >= 200) {
                    String candidate = subPrefixPool.get(subCursor++ % subPrefixPool.size()) + idx + k + "（" + primary + "）";
                    result.add(candidate);
                }
            }
        }
        // 如果由于异常 case（比如 primaries 对应子项没取够 / n 过大），导致 result!=10，
        // 最后做一步：超出截断 + 不足再循环补。
        while (result.size() > 10) result.remove(result.size() - 1);
        int safe = 0;
        while (result.size() < 10 && safe < 100) {
            safe++;
            String primary = primaries.get(safe % primaries.size());
            String sub = subPrefixPool.get((subCursor++) % subPrefixPool.size());
            String cand = sub + "（" + primary + "）";
            if (!result.contains(cand)) result.add(cand);
        }
        return result;
    }

    /**
     * 把 10 个子菜单分配到 n 个一级菜单上，每个为 2 或 3。
     * 比如 n=4 → [3,3,2,2] 或 [2,3,3,2] 等随机排列。
     */
    private static int[] distributeCountsTo(int n, int total) {
        if (n <= 0) return new int[]{total};
        int[] res = new int[n];
        // 下界：每个至少 2
        int remain = total - 2 * n; // 比如 n=4 → remain = 2
        Arrays.fill(res, 2);
        // 把 remain 个 +1 随机分配
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        Collections.shuffle(order, ThreadLocalRandom.current());
        int idx = 0;
        while (remain > 0 && idx < order.size()) {
            res[order.get(idx++)] += 1;
            remain--;
        }
        // 如果还有剩余（n<3 / total 溢出），依次往前加直到满
        idx = 0;
        while (remain > 0) {
            res[idx++ % n] += 1;
            remain--;
        }
        return res;
    }

    // 从"子菜单（一级菜单）"格式里解析一级菜单
    private static String parsePrimary(String line) {
        if (line == null) return null;
        int s = line.indexOf('（');
        int e = line.indexOf('）');
        if (s > 0 && e > s) return line.substring(s + 1, e);
        int s2 = line.indexOf('(');
        int e2 = line.indexOf(')');
        if (s2 > 0 && e2 > s2) return line.substring(s2 + 1, e2);
        return null;
    }

    private static String detectSoftwareName(String prompt) {
        if (prompt == null) return null;
        // 从形如"请根据软件名称'xxx'"或"请根据软件名称"xxx""里提取
        String name = null;
        int k = prompt.indexOf("软件名称");
        if (k >= 0) {
            String tail = prompt.substring(k + 4).trim();
            // 单引号
            int s = tail.indexOf('\'');
            int e = s >= 0 ? tail.indexOf('\'', s + 1) : -1;
            if (s >= 0 && e > s) name = tail.substring(s + 1, e).trim();
            else {
                int s2 = tail.indexOf('"');
                int e2 = s2 >= 0 ? tail.indexOf('"', s2 + 1) : -1;
                if (s2 >= 0 && e2 > s2) name = tail.substring(s2 + 1, e2).trim();
            }
        }
        return name;
    }

    // 根据软件名关键词给一级菜单池（行业相关优先，保证内容"像"这个软件）
    private static List<String> buildPrimaryPool(String softwareName) {
        String n = softwareName == null ? "" : softwareName;
        if (containsAny(n, "模具", "生产", "工序", "制造", "派工", "报工", "BOM", "机加")) {
            return Arrays.asList(
                    "订单与工艺管理", "工序流转执行", "车间物料调度", "模具质量追溯", "生产进度跟踪"
            );
        }
        if (containsAny(n, "建筑", "工程", "施工", "项目", "工地")) {
            return Arrays.asList(
                    "工程项目管理", "施工进度管控", "材料与资源调度", "质量安全巡检", "变更与签证管理"
            );
        }
        if (containsAny(n, "金融", "信贷", "贷款", "风控", "银行", "保险")) {
            return Arrays.asList(
                    "客户档案管理", "业务受理审批", "额度与费率管理", "贷后/保后跟踪", "档案合同归档"
            );
        }
        if (containsAny(n, "医疗", "医院", "HIS", "门诊", "病患", "病历")) {
            return Arrays.asList(
                    "病患档案管理", "门诊诊疗登记", "医嘱处方开立", "检验检查管理", "病历文书归档"
            );
        }
        if (containsAny(n, "教育", "教务", "教学", "学生", "校园", "培训", "课程")) {
            return Arrays.asList(
                    "课程与师资管理", "排课与考勤登记", "作业与成绩录入", "教学资料维护", "培训计划审核"
            );
        }
        if (containsAny(n, "物流", "仓储", "快递", "运输", "配送", "库存")) {
            return Arrays.asList(
                    "订单录入与调度", "入库出库登记", "库存盘点核对", "运输过程跟踪", "回单签收确认"
            );
        }
        if (containsAny(n, "电商", "商城", "店铺", "商品", "营销", "订单", "会员")) {
            return Arrays.asList(
                    "商品档案维护", "订单履约处理", "营销活动配置", "会员权益管理", "售后工单处理"
            );
        }
        if (containsAny(n, "软著", "文档", "著材料", "申请", "生成", "AI")) {
            return Arrays.asList(
                    "项目资料管理", "内容模板配置", "AI智能生成", "文档版本维护", "导出与交付管理"
            );
        }
        // 通用兜底
        return new ArrayList<>(GENERIC_PRIMARY);
    }

    private static List<String> buildSubPrefixPool(String softwareName) {
        List<String> core = new ArrayList<>(GENERIC_SUB_PREFIXES);
        Collections.shuffle(core, ThreadLocalRandom.current());
        return core;
    }

    private static boolean containsAny(String s, String... arr) {
        for (String a : arr) if (s.contains(a)) return true;
        return false;
    }
}
