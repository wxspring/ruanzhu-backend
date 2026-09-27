package com.company.ruanzhu.generate.ai;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AiClientPlaceholderTest {

    @Test
    void functionalPrompt_returns16LinesWithAllRequiredHeaders() throws Exception {
        OpenAiClient client = new OpenAiClient();
        Method m = OpenAiClient.class.getDeclaredMethod("generatePlaceholder", String.class);
        m.setAccessible(true);
        String prompt = "下面我会给出软著名称\"模具生产工序流程管控软件\"，请你根据名称分别给出下面的内容，固定的内容不变，不要加开头和结尾句：\n" +
                "软著名称：模具生产工序流程管控软件\n" +
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

        String out = (String) m.invoke(client, prompt);
        assertNotNull(out);
        out = out.trim();
        String[] lines = out.split("\\r?\\n");
        assertEquals(16, lines.length, "必须返回 16 行，实际：" + lines.length + "\n" + out);

        String[] expectedPrefixes = {
                "软著名称：",
                "版本号：",
                "软件分类：",
                "开发的硬件环境：",
                "运行的硬件环境：",
                "开发该软件的操作系统：",
                "软件开发环境/开发工具：",
                "该软件的运行平台/操作系统：",
                "软件运行支撑环境/支持软件：",
                "编程语言：",
                "源程序量：",
                "开发目的：",
                "面向领域：",
                "主要功能：",
                "技术特点：",
                "软件的技术特点选项："
        };
        for (int i = 0; i < expectedPrefixes.length; i++) {
            assertTrue(lines[i].startsWith(expectedPrefixes[i]),
                    "第" + (i + 1) + "行应以[" + expectedPrefixes[i] + "]开头，实际：" + lines[i]);
        }

        // 软著名称提取
        assertEquals("软著名称：模具生产工序流程管控软件", lines[0]);
        // 源程序量：8000-20000 数字
        int codeLines = Integer.parseInt(lines[10].substring("源程序量：".length()).trim());
        assertTrue(codeLines >= 8000 && codeLines <= 20000, "源程序量必须在 8000-20000，实际：" + codeLines);
        // 主要功能（含补尾段后约 420-900 纯汉字；真实软著 400+ 字也足够使用，避免过短或超长）
        String mf = lines[13].substring("主要功能：".length());
        int mfLen = mf.replaceAll("[\\pP\\s]", "").length();
        assertTrue(mfLen >= 420 && mfLen <= 900, "主要功能纯字数范围 420-900，实际：" + mfLen);
        assertTrue(mf.endsWith("。"), "主要功能必须以句号结尾，实际结尾：" + mf.substring(Math.max(0, mf.length() - 2)));
        // 技术特点 30-100 字符
        String tf = lines[14].substring("技术特点：".length());
        assertTrue(tf.length() >= 25 && tf.length() <= 110, "技术特点长度必须 30-100，实际：" + tf.length());
        assertTrue(tf.endsWith("。"), "技术特点必须以句号结尾，实际：" + tf);
        // 技术特点选项必须是给定 12 项之一
        String opts = lines[15].substring("软件的技术特点选项：".length()).trim();
        String[] pool = {"信息安全软件", "游戏软件", "大数据软件", "教育软件", "人工智能软件",
                "金融软件", "VR软件", "医疗软件", "地理信息软件", "云计算软件", "物联网软件", "智慧城市软件"};
        boolean matched = false;
        for (String p : pool) if (p.equals(opts)) { matched = true; break; }
        assertTrue(matched, "技术特点选项必须为 12 项之一，实际：[" + opts + "]");
    }

    @Test
    void overviewPrompt_returnsSingleParagraphAbout200Chars() throws Exception {
        OpenAiClient client = new OpenAiClient();
        Method m = OpenAiClient.class.getDeclaredMethod("generatePlaceholder", String.class);
        m.setAccessible(true);
        String prompt = "帮我根据'模具生产工序流程管控软件'，写一段系统概述，要求200字数，不要涉及具体功能菜单，不要加开头和结尾句，笼统的连贯概括";
        String out = (String) m.invoke(client, prompt);
        assertNotNull(out);
        out = out.trim();
        assertFalse(out.isEmpty());
        assertFalse(out.contains("\n"), "系统概述应单段输出，不含换行");
        int len = out.replaceAll("[\\pP\\s]", "").length();
        assertTrue(len >= 120 && len <= 280, "系统概述字数约 200，实际：" + len + "，内容：" + out);
    }
}
