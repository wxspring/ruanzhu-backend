package com.company.ruanzhu.generate.ai;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class AiClientPlaceholderDetailedTest {

    private static final String FEATURES_PROMPT =
            "下面我会给出软著名称\"模具生产工序流程管控软件\"，请你根据名称分别给出下面的内容，固定的内容不变，不要加开头和结尾句：\n" +
            "软著名称：\n" +
            "版本号：V1.0\n" +
            "软件分类：应用软件\n" +
            "开发的硬件环境：（随机,45个汉字以内）\n" +
            "运行的硬件环境：（随机,45个汉字以内）\n" +
            "开发该软件的操作系统：（随机,45个汉字以内）\n" +
            "软件开发环境/开发工具：（随机,45个汉字以内）\n" +
            "该软件的运行平台/操作系统：（随机,45个汉字以内）\n" +
            "软件运行支撑环境/支持软件：（随机,45个汉字以内）\n" +
            "编程语言：java\n" +
            "源程序量：（要求：8000-20000之间随机数值，不要重复,只需要回复数字）\n" +
            "开发目的：（大于8个字 少于50个字）\n" +
            "面向领域：（大于4个字 少于50个字）\n" +
            "主要功能：（主要功能根据软件的特点，是web软件，必须大于550汉字，必须小于800汉字，不要带序号,结尾带句号）\n" +
            "技术特点：（根据软件名方向随机生成，不要出现广告语、宣传语，字数必须不低于30个字符，必须在100字符以内,结尾带句号）\n" +
            "软件的技术特点选项： （信息安全软件，游戏软件，大数据软件，教育软件，人工智能软件，金融软件，VR软件，医疗软件，地理信息软件，云计算软件，物联网软件，智慧城市软件 根据软件名称和内容，判断属于哪个上面括号中的哪个，并只输出一个在\"软件的技术特点选项：\"后）";

    private static final String OVERVIEW_PROMPT =
            "帮我根据'模具生产工序流程管控软件'，写一段系统概述，要求200字数，不要涉及具体功能菜单，不要加开头和结尾句，笼统的连贯概括";

    private static final String[] EXPECTED_PREFIXES = {
            "软著名称：", "版本号：", "软件分类：",
            "开发的硬件环境：", "运行的硬件环境：", "开发该软件的操作系统：",
            "软件开发环境/开发工具：", "该软件的运行平台/操作系统：",
            "软件运行支撑环境/支持软件：", "编程语言：", "源程序量：",
            "开发目的：", "面向领域：", "主要功能：", "技术特点：",
            "软件的技术特点选项："
    };

    private static final String[] TECH_OPTIONS_POOL = {
            "信息安全软件", "游戏软件", "大数据软件", "教育软件",
            "人工智能软件", "金融软件", "VR软件", "医疗软件",
            "地理信息软件", "云计算软件", "物联网软件", "智慧城市软件"
    };

    private void doTestFeatures(String label, Object client) throws Exception {
        System.out.println("\n========== " + label + " - FEATURES ==========");
        Method m = client.getClass().getDeclaredMethod("generatePlaceholder", String.class);
        m.setAccessible(true);
        String out = (String) m.invoke(client, FEATURES_PROMPT);
        assertNotNull(out);
        out = out.trim();
        String[] lines = out.split("\\r?\\n");

        System.out.println("Total lines: " + lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int len = line.length();
            if (len > 100) {
                System.out.printf("%2d [%d]: %s...%n", i+1, len, line.substring(0, 100));
            } else {
                System.out.printf("%2d [%d]: %s%n", i+1, len, line);
            }
        }

        // Assertions
        assertEquals(16, lines.length, "必须返回16行，实际：" + lines.length);
        for (int i = 0; i < EXPECTED_PREFIXES.length; i++) {
            assertTrue(lines[i].startsWith(EXPECTED_PREFIXES[i]),
                    "第" + (i+1) + "行应以[" + EXPECTED_PREFIXES[i] + "]开头");
        }

        // Extract main functions pure char length
        Pattern mfP = Pattern.compile("主要功能：(.*)", Pattern.DOTALL);
        Matcher mfM = mfP.matcher(out);
        assertTrue(mfM.find(), "应包含主要功能");
        String mf = mfM.group(1).split("\\n技术特点：")[0];
        int mfClean = mf.replaceAll("[\\pP\\s]", "").length();
        System.out.println("主要功能纯字数: " + mfClean);
        assertTrue(mfClean >= 420 && mfClean <= 900, "主要功能纯字数范围 420-900，实际：" + mfClean);
        assertTrue(mf.trim().endsWith("。"), "主要功能结尾必须是句号");

        // Tech features
        String line14 = lines[14];
        String tf = line14.substring("技术特点：".length());
        System.out.println("技术特点字数: " + tf.length());
        assertTrue(tf.length() >= 25 && tf.length() <= 110, "技术特点长度 25-110，实际：" + tf.length());
        assertTrue(tf.endsWith("。"), "技术特点结尾必须是句号");

        // Tech options
        String line15 = lines[15];
        String opts = line15.substring("软件的技术特点选项：".length()).trim();
        System.out.println("技术特点选项: [" + opts + "]");
        boolean matched = false;
        for (String p : TECH_OPTIONS_POOL) if (p.equals(opts)) { matched = true; break; }
        assertTrue(matched, "技术特点选项必须为12项之一，实际：[" + opts + "]");

        // Code lines
        int codeLines = Integer.parseInt(lines[10].substring("源程序量：".length()).trim());
        System.out.println("源程序量: " + codeLines);
        assertTrue(codeLines >= 8000 && codeLines <= 20000, "源程序量 8000-20000，实际：" + codeLines);

        System.out.println("========== " + label + " - FEATURES PASSED ==========");
    }

    private void doTestOverview(String label, Object client) throws Exception {
        System.out.println("\n========== " + label + " - OVERVIEW ==========");
        Method m = client.getClass().getDeclaredMethod("generatePlaceholder", String.class);
        m.setAccessible(true);
        String out = (String) m.invoke(client, OVERVIEW_PROMPT);
        assertNotNull(out);
        out = out.trim();
        int len = out.length();
        int clean = out.replaceAll("[\\pP\\s]", "").length();
        System.out.println("总字符数: " + len + "，纯字数: " + clean);
        if (len > 300) {
            System.out.println(out.substring(0, 300) + "...");
        } else {
            System.out.println(out);
        }
        assertFalse(out.contains("\n"), "系统概述不应包含换行");
        assertTrue(clean >= 120 && clean <= 280, "系统概述纯字数 120-280，实际：" + clean);
        System.out.println("========== " + label + " - OVERVIEW PASSED ==========");
    }

    @Test
    void openAiClient_features() throws Exception {
        doTestFeatures("OpenAiClient", new OpenAiClient());
    }

    @Test
    void openAiClient_overview() throws Exception {
        doTestOverview("OpenAiClient", new OpenAiClient());
    }

    @Test
    void qwenAiClient_features() throws Exception {
        doTestFeatures("QwenAiClient", new QwenAiClient());
    }

    @Test
    void qwenAiClient_overview() throws Exception {
        doTestOverview("QwenAiClient", new QwenAiClient());
    }
}
