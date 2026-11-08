package com.company.ruanzhu.file.generator;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 Java AWT 直接绘制系统截图，不依赖浏览器。
 * 作为 SystemScreenshotService（浏览器截图）的兜底方案。
 *
 * 截图标识约定与 SystemScreenshotService 一致：
 * - login        : 登录页
 * - home         : 首页/工作台
 * - list_1, list_2, ...  : 各模块列表页
 * - form_1, form_2, ...  : 各模块表单页
 */
@Slf4j
public class AwtScreenshotService {

    private static final int W = 1280;
    private static final int H = 800;

    private final MockSystemHtmlGenerator htmlGen;
    private final MockSystemHtmlGenerator.IndustryProfile industry;
    private final File outputDir;

    public AwtScreenshotService(String softwareName, String functionMenu, File outputDir) {
        this.outputDir = outputDir;
        if (!outputDir.exists()) outputDir.mkdirs();
        this.htmlGen = new MockSystemHtmlGenerator(softwareName, functionMenu, outputDir);
        this.industry = htmlGen.getIndustry();
    }

    public Map<String, File> captureAll() {
        Map<String, File> result = new LinkedHashMap<>();
        try {
            // 基础页面
            result.put("login", draw("login.png", this::drawLogin));
            result.put("home", draw("home.png", this::drawHome));

            // 架构图
            result.put("arch", draw("arch.png", this::drawArchitecture));

            // 安装章节 Windows 系统截图
            result.put("win_ipconfig", draw("win_ipconfig.png", this::drawWinIpConfig));
            result.put("win_explorer", draw("win_explorer.png", this::drawWinExplorer));
            result.put("win_service", draw("win_service.png", this::drawWinService));

            // 每个模块 6 张截图
            int idx = 1;
            for (Map.Entry<String, List<String>> e : htmlGen.getModules().entrySet()) {
                String mod = e.getKey();
                result.put("list_" + idx, draw("list_" + idx + ".png", g -> drawListWithData(g, mod)));
                result.put("list_empty_" + idx, draw("list_empty_" + idx + ".png", g -> drawListEmpty(g, mod)));
                result.put("list_search_" + idx, draw("list_search_" + idx + ".png", g -> drawListSearch(g, mod)));
                result.put("form_" + idx, draw("form_" + idx + ".png", g -> drawFormNew(g, mod)));
                result.put("form_filled_" + idx, draw("form_filled_" + idx + ".png", g -> drawFormFilled(g, mod)));
                result.put("detail_" + idx, draw("detail_" + idx + ".png", g -> drawDetail(g, mod)));
                idx++;
            }
            log.info("AWT 截图生成完成，共 {} 张", result.size());
        } catch (Exception e) {
            log.error("AWT 截图生成异常", e);
        }
        return result;
    }

    /** 清理工作目录 */
    public void cleanup() {
        try {
            deleteRecursively(outputDir);
        } catch (IOException e) {
            log.warn("清理 AWT 截图工作目录失败", e);
        }
    }

    private static void deleteRecursively(File f) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        java.nio.file.Files.deleteIfExists(f.toPath());
    }

    // ==================== 通用绘制工具 ====================

    private File draw(String name, Painter painter) throws IOException {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        painter.paint(g);
        g.dispose();
        File f = new File(outputDir, name);
        ImageIO.write(img, "png", f);
        return f;
    }

    @FunctionalInterface
    interface Painter {
        void paint(Graphics2D g);
    }

    /** 获取中文字体 */
    private Font font(int size, int style) {
        String[] names = {"Microsoft YaHei", "微软雅黑", "SimHei", "黑体", "SansSerif"};
        for (String n : names) {
            Font f = new Font(n, style, size);
            if (f.getFamily().equals(n) || f.canDisplay('中')) return f;
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    private Color color(String hex) {
        return Color.decode(hex);
    }

    private void roundRect(Graphics2D g, int x, int y, int w, int h, int r, Color c) {
        g.setColor(c);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }

    private void drawText(Graphics2D g, String text, int x, int y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        g.drawString(text, x, y);
    }

    private void drawTextCentered(Graphics2D g, String text, int cx, int y, Font f, Color c) {
        g.setFont(f);
        g.setColor(c);
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        g.drawString(text, x, y);
    }

    // ==================== 登录页 ====================

    private void drawLogin(Graphics2D g) {
        // 背景渐变
        GradientPaint bg = new GradientPaint(0, 0, color("#1a3a5c"), W, H, color("#0d2137"));
        g.setPaint(bg);
        g.fillRect(0, 0, W, H);

        // 装饰圆
        g.setColor(new Color(255, 255, 255, 15));
        g.fillOval(W - 200, -100, 400, 400);
        g.fillOval(-100, H - 200, 300, 300);

        // 登录卡片
        int cardW = 400, cardH = 420;
        int cx = (W - cardW) / 2, cy = (H - cardH) / 2;
        roundRect(g, cx, cy, cardW, cardH, 12, Color.WHITE);

        // 软件名称
        drawTextCentered(g, htmlGen.getSoftwareName(), W / 2, cy + 70, font(20, Font.BOLD), color("#1a3a5c"));
        // 副标题
        drawTextCentered(g, industry.loginSubtitle, W / 2, cy + 100, font(13, Font.PLAIN), color("#8c8c8c"));

        // 用户名输入框
        int inputX = cx + 40, inputY = cy + 140, inputW = cardW - 80, inputH = 42;
        drawText(g, "用户名", inputX, inputY - 8, font(13, Font.PLAIN), color("#595959"));
        roundRect(g, inputX, inputY, inputW, inputH, 6, color("#f5f5f5"));
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(inputX, inputY, inputW, inputH, 6, 6);
        drawText(g, industry.loginUser, inputX + 12, inputY + 27, font(14, Font.PLAIN), color("#262626"));

        // 密码输入框
        inputY += 70;
        drawText(g, "密码", inputX, inputY - 8, font(13, Font.PLAIN), color("#595959"));
        roundRect(g, inputX, inputY, inputW, inputH, 6, color("#f5f5f5"));
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(inputX, inputY, inputW, inputH, 6, 6);
        // 密码用圆点
        g.setColor(color("#262626"));
        for (int i = 0; i < 8; i++) {
            g.fillOval(inputX + 14 + i * 14, inputY + 17, 6, 6);
        }

        // 登录按钮
        int btnY = inputY + 65;
        roundRect(g, inputX, btnY, inputW, 42, 6, color("#1890ff"));
        drawTextCentered(g, "登 录", W / 2, btnY + 27, font(15, Font.BOLD), Color.WHITE);
    }

    // ==================== 首页/工作台 ====================

    private void drawHome(Graphics2D g) {
        drawChrome(g, "工作台");

        int contentX = 240, contentY = 56, contentW = W - 240;
        int pad = 20;

        // 统计卡片（4 个）
        String[] stats = industry.statsData;
        int cardW = (contentW - pad * 5) / 4;
        int cardH = 90;
        String[] statColors = {"#1890ff", "#52c41a", "#faad14", "#f5222d"};
        for (int i = 0; i < 4; i++) {
            int x = contentX + pad + i * (cardW + pad);
            int y = contentY + pad;
            roundRect(g, x, y, cardW, cardH, 6, Color.WHITE);
            g.setColor(color("#f0f0f0"));
            g.drawRoundRect(x, y, cardW, cardH, 6, 6);

            String label = stats[i * 3];
            String value = stats[i * 3 + 1];
            String trend = stats[i * 3 + 2];

            drawText(g, label, x + 16, y + 30, font(13, Font.PLAIN), color("#8c8c8c"));
            drawText(g, value, x + 16, y + 62, font(24, Font.BOLD), color(statColors[i]));
            drawText(g, trend, x + 16, y + 82, font(11, Font.PLAIN),
                    trend.startsWith("-") ? color("#f5222d") : color("#52c41a"));
        }

        // 最近操作记录
        int tableY = contentY + pad + cardH + pad;
        int tableH = H - tableY - pad;
        roundRect(g, contentX + pad, tableY, contentW - pad * 2, tableH, 6, Color.WHITE);
        g.setColor(color("#f0f0f0"));
        g.drawRoundRect(contentX + pad, tableY, contentW - pad * 2, tableH, 6, 6);

        drawText(g, "最近操作记录", contentX + pad + 16, tableY + 30, font(15, Font.BOLD), color("#262626"));

        // 表头
        String[] cols = {"时间", "操作人", "操作内容", "状态"};
        int[] colW = {160, 100, 0, 100};
        int tableX = contentX + pad + 16;
        int tableContentW = contentW - pad * 2 - 32;
        colW[2] = tableContentW - colW[0] - colW[1] - colW[3];
        int thY = tableY + 50;
        g.setColor(color("#fafafa"));
        g.fillRect(tableX, thY, tableContentW, 36);
        g.setColor(color("#f0f0f0"));
        g.drawLine(tableX, thY + 36, tableX + tableContentW, thY + 36);

        int cx = tableX;
        for (int i = 0; i < cols.length; i++) {
            drawText(g, cols[i], cx + 12, thY + 23, font(13, Font.BOLD), color("#595959"));
            cx += colW[i];
        }

        // 数据行
        String[][] ops = industry.recentOps;
        for (int r = 0; r < ops.length && r < 6; r++) {
            int ry = thY + 36 + r * 38;
            if (ry + 38 > tableY + tableH - 10) break;
            cx = tableX;
            for (int c = 0; c < ops[r].length && c < cols.length; c++) {
                Color tc = color("#262626");
                if (c == 3) {
                    String st = ops[r][c];
                    if (st.contains("成功")) tc = color("#52c41a");
                    else if (st.contains("告警") || st.contains("失败")) tc = color("#f5222d");
                    else if (st.contains("审批")) tc = color("#faad14");
                }
                drawText(g, ops[r][c], cx + 12, ry + 24, font(12, Font.PLAIN), tc);
                cx += colW[c];
            }
        }
    }

    // ==================== 列表页（有数据） ====================

    private void drawListWithData(Graphics2D g, String module) {
        drawListBase(g, module, true, false);
    }

    // ==================== 列表页（空数据） ====================

    private void drawListEmpty(Graphics2D g, String module) {
        drawListBase(g, module, false, false);
    }

    // ==================== 列表页（搜索筛选） ====================

    private void drawListSearch(Graphics2D g, String module) {
        drawListBase(g, module, true, true);
    }

    private void drawListBase(Graphics2D g, String module, boolean hasData, boolean isSearch) {
        drawChrome(g, module);

        int contentX = 240, contentY = 56, contentW = W - 240;
        int pad = 20;

        // 面包屑
        drawText(g, "首页 / " + module, contentX + pad, contentY + 30, font(13, Font.PLAIN), color("#8c8c8c"));

        // 操作栏：搜索框 + 新增按钮
        int barY = contentY + 45;
        roundRect(g, contentX + pad, barY, 240, 34, 4, Color.WHITE);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(contentX + pad, barY, 240, 34, 4, 4);
        if (isSearch) {
            drawText(g, "待审核", contentX + pad + 12, barY + 22, font(13, Font.PLAIN), color("#262626"));
            // 搜索按钮
            roundRect(g, contentX + pad + 250, barY, 60, 34, 4, color("#1890ff"));
            drawTextCentered(g, "搜 索", contentX + pad + 250 + 30, barY + 22, font(13, Font.PLAIN), Color.WHITE);
        } else {
            drawText(g, "请输入关键字搜索...", contentX + pad + 12, barY + 22, font(13, Font.PLAIN), color("#bfbfbf"));
        }

        // 新增按钮 + 红色标注框
        int btnX = contentX + pad + (isSearch ? 325 : 255);
        roundRect(g, btnX, barY, 80, 34, 4, color("#1890ff"));
        drawTextCentered(g, "+ 新增", btnX + 40, barY + 22, font(13, Font.BOLD), Color.WHITE);
        // 红色标注框
        g.setColor(color("#f5222d"));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(btnX - 3, barY - 3, 86, 40, 6, 6);
        g.setStroke(new BasicStroke(1));

        // 表格
        String[] cols = industry.listColumnsFor(module);
        String[][] rows = industry.sampleRowsFor(module);

        int tableY = barY + 50;
        int tableX = contentX + pad;
        int tableContentW = contentW - pad * 2;
        int tableH = H - tableY - pad;

        roundRect(g, tableX, tableY, tableContentW, tableH, 6, Color.WHITE);
        g.setColor(color("#f0f0f0"));
        g.drawRoundRect(tableX, tableY, tableContentW, tableH, 6, 6);

        // 表头
        int thY = tableY + 10;
        g.setColor(color("#fafafa"));
        g.fillRect(tableX + 1, thY, tableContentW - 2, 38);
        g.setColor(color("#f0f0f0"));
        g.drawLine(tableX, thY + 38, tableX + tableContentW, thY + 38);

        int colCount = Math.min(cols.length, 8);
        int colW = tableContentW / colCount;
        for (int i = 0; i < colCount; i++) {
            String txt = cols[i];
            if (g.getFontMetrics(font(12, Font.PLAIN)).stringWidth(txt) > colW - 16) {
                txt = txt.substring(0, Math.min(txt.length(), colW / 12 - 1)) + "..";
            }
            drawText(g, txt, tableX + i * colW + 12, thY + 25, font(12, Font.BOLD), color("#595959"));
        }

        if (!hasData) {
            // 空数据提示
            drawTextCentered(g, "暂无数据", tableX + tableContentW / 2, tableY + tableH / 2, font(16, Font.PLAIN), color("#bfbfbf"));
            // 分页不显示
            return;
        }

        // 数据行
        int maxRows = isSearch ? 3 : 8; // 搜索结果少
        for (int r = 0; r < rows.length && r < maxRows; r++) {
            int ry = thY + 38 + r * 38;
            if (ry + 38 > tableY + tableH - 10) break;
            if (r % 2 == 1) {
                g.setColor(new Color(250, 250, 250));
                g.fillRect(tableX + 1, ry, tableContentW - 2, 38);
            }
            g.setColor(color("#f5f5f5"));
            g.drawLine(tableX, ry + 38, tableX + tableContentW, ry + 38);

            for (int c = 0; c < colCount && c < rows[r].length; c++) {
                String txt = rows[r][c];
                Color tc = color("#262626");
                // 状态列着色
                if (c == colCount - 1) {
                    if (txt.contains("合格") || txt.contains("成功") || txt.contains("已完成")) tc = color("#52c41a");
                    else if (txt.contains("待") || txt.contains("审批")) tc = color("#faad14");
                    else if (txt.contains("退回") || txt.contains("失败") || txt.contains("异常")) tc = color("#f5222d");
                }
                if (g.getFontMetrics(font(12, Font.PLAIN)).stringWidth(txt) > colW - 16) {
                    txt = txt.substring(0, Math.min(txt.length(), colW / 12 - 1)) + "..";
                }
                drawText(g, txt, tableX + c * colW + 12, ry + 24, font(12, Font.PLAIN), tc);
            }
        }

        // 分页信息
        int pagerY = tableY + tableH - 30;
        drawText(g, "共 " + industry.recordCount + " 条记录", tableX + 12, pagerY, font(12, Font.PLAIN), color("#8c8c8c"));
        roundRect(g, tableX + tableContentW - 120, pagerY - 18, 60, 26, 4, Color.WHITE);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(tableX + tableContentW - 120, pagerY - 18, 60, 26, 4, 4);
        drawTextCentered(g, "1 / 13", tableX + tableContentW - 90, pagerY, font(12, Font.PLAIN), color("#595959"));
    }

    // ==================== 表单页（新增，空） ====================

    private void drawFormNew(Graphics2D g, String module) {
        drawFormBase(g, module, false);
    }

    // ==================== 表单页（已填写） ====================

    private void drawFormFilled(Graphics2D g, String module) {
        drawFormBase(g, module, true);
    }

    private void drawFormBase(Graphics2D g, String module, boolean filled) {
        drawChrome(g, module);

        int contentX = 240, contentY = 56, contentW = W - 240;
        int pad = 20;

        // 面包屑
        drawText(g, "首页 / " + module + " / 新增", contentX + pad, contentY + 30, font(13, Font.PLAIN), color("#8c8c8c"));

        // 表单卡片
        int formY = contentY + 45;
        int formH = H - formY - pad;
        roundRect(g, contentX + pad, formY, contentW - pad * 2, formH, 6, Color.WHITE);
        g.setColor(color("#f0f0f0"));
        g.drawRoundRect(contentX + pad, formY, contentW - pad * 2, formH, 6, 6);

        // 标题
        drawText(g, industry.primaryAction + module, contentX + pad + 16, formY + 35, font(16, Font.BOLD), color("#262626"));
        g.setColor(color("#f0f0f0"));
        g.drawLine(contentX + pad + 16, formY + 50, contentX + contentW - pad - 16, formY + 50);

        // 表单字段（两列布局）
        String[] fields = industry.formFieldsFor(module);
        int labelW = 110;
        int inputW = (contentW - pad * 2 - 32 - labelW * 2 - 40) / 2;
        int rowH = 60;
        int startX = contentX + pad + 16;
        int startY = formY + 80;

        for (int i = 0; i < fields.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = startX + col * (labelW + inputW + 40);
            int fy = startY + row * rowH;
            if (fy + rowH > formY + formH - 80) break;

            // 标签（必填项加红星）
            boolean required = i < 4;
            String label = fields[i];
            if (required) {
                drawText(g, "*", fx, fy + 16, font(14, Font.PLAIN), color("#f5222d"));
                drawText(g, label, fx + 14, fy + 16, font(13, Font.PLAIN), color("#595959"));
            } else {
                drawText(g, label, fx, fy + 16, font(13, Font.PLAIN), color("#595959"));
            }

            // 输入框
            int ix = fx + labelW;
            int iy = fy - 4;
            roundRect(g, ix, iy, inputW, 32, 4, Color.WHITE);
            g.setColor(color("#d9d9d9"));
            g.drawRoundRect(ix, iy, inputW, 32, 4, 4);

            // 占位符或已填写内容
            if (filled) {
                String[] sampleVals = industry.sampleRowsFor(module)[0];
                int vi = i % 2 == 0 ? 0 : 1;
                String val = (vi < sampleVals.length) ? sampleVals[vi] : "示例数据" + (i + 1);
                drawText(g, val, ix + 10, iy + 21, font(12, Font.PLAIN), color("#262626"));
            } else {
                drawText(g, "请输入" + label, ix + 10, iy + 21, font(12, Font.PLAIN), color("#bfbfbf"));
            }
        }

        // 备注字段（整行）
        int remarkRow = (fields.length + 1) / 2;
        int ry = startY + remarkRow * rowH;
        if (ry + 80 < formY + formH - 80) {
            drawText(g, industry.remarkLabel, startX, ry + 16, font(13, Font.PLAIN), color("#595959"));
            int ix = startX + labelW;
            int iy = ry - 4;
            int iw = contentW - pad * 2 - 32 - labelW;
            roundRect(g, ix, iy, iw, 70, 4, Color.WHITE);
            g.setColor(color("#d9d9d9"));
            g.drawRoundRect(ix, iy, iw, 70, 4, 4);
            if (filled) {
                drawText(g, "已按要求完成" + industry.remarkLabel + "信息录入。", ix + 10, iy + 22, font(12, Font.PLAIN), color("#262626"));
            } else {
                drawText(g, "请输入" + industry.remarkLabel, ix + 10, iy + 22, font(12, Font.PLAIN), color("#bfbfbf"));
            }

            ry = iy + 70 + 20;
        } else {
            ry = formY + formH - 80;
        }

        // 按钮（保存按钮加红色标注框）
        int btnY = formY + formH - 60;
        int btnW = 90, btnH = 36;
        int btnX = contentX + contentW - pad - btnW * 2 - 16;
        roundRect(g, btnX, btnY, btnW, btnH, 4, Color.WHITE);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(btnX, btnY, btnW, btnH, 4, 4);
        drawTextCentered(g, "取 消", btnX + btnW / 2, btnY + 23, font(13, Font.PLAIN), color("#595959"));

        roundRect(g, btnX + btnW + 16, btnY, btnW, btnH, 4, color("#1890ff"));
        drawTextCentered(g, "保 存", btnX + btnW + 16 + btnW / 2, btnY + 23, font(13, Font.BOLD), Color.WHITE);
        // 红色标注框
        g.setColor(color("#f5222d"));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(btnX + btnW + 13, btnY - 3, btnW + 6, btnH + 6, 6, 6);
        g.setStroke(new BasicStroke(1));
    }

    // ==================== 详情页 ====================

    private void drawDetail(Graphics2D g, String module) {
        drawChrome(g, module);

        int contentX = 240, contentY = 56, contentW = W - 240;
        int pad = 20;

        // 面包屑
        drawText(g, "首页 / " + module + " / 详情", contentX + pad, contentY + 30, font(13, Font.PLAIN), color("#8c8c8c"));

        // 详情卡片
        int formY = contentY + 45;
        int formH = H - formY - pad;
        roundRect(g, contentX + pad, formY, contentW - pad * 2, formH, 6, Color.WHITE);
        g.setColor(color("#f0f0f0"));
        g.drawRoundRect(contentX + pad, formY, contentW - pad * 2, formH, 6, 6);

        drawText(g, module + " - 详情信息", contentX + pad + 16, formY + 35, font(16, Font.BOLD), color("#262626"));
        g.setColor(color("#f0f0f0"));
        g.drawLine(contentX + pad + 16, formY + 50, contentX + contentW - pad - 16, formY + 50);

        // 详情字段（只读展示，两列）
        String[] fields = industry.formFieldsFor(module);
        String[] sampleVals = industry.sampleRowsFor(module)[0];
        int labelW = 110;
        int valW = (contentW - pad * 2 - 32 - labelW * 2 - 40) / 2;
        int rowH = 50;
        int startX = contentX + pad + 16;
        int startY = formY + 80;

        for (int i = 0; i < fields.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int fx = startX + col * (labelW + valW + 40);
            int fy = startY + row * rowH;
            if (fy + rowH > formY + formH - 60) break;

            drawText(g, fields[i] + "：", fx, fy + 16, font(13, Font.PLAIN), color("#8c8c8c"));
            String val = (i < sampleVals.length) ? sampleVals[i] : "示例值";
            drawText(g, val, fx + labelW, fy + 16, font(13, Font.BOLD), color("#262626"));
        }

        // 备注行
        int remarkRow = (fields.length + 1) / 2;
        int ry = startY + remarkRow * rowH;
        if (ry + 80 < formY + formH - 60) {
            drawText(g, industry.remarkLabel + "：", startX, ry + 16, font(13, Font.PLAIN), color("#8c8c8c"));
            drawText(g, "已按要求完成相关信息录入，数据校验通过。", startX + labelW, ry + 16, font(13, Font.BOLD), color("#262626"));
        }

        // 底部返回按钮
        int btnY = formY + formH - 60;
        int btnW = 90, btnH = 36;
        roundRect(g, contentX + contentW - pad - btnW, btnY, btnW, btnH, 4, color("#1890ff"));
        drawTextCentered(g, "返 回", contentX + contentW - pad - btnW / 2, btnY + 23, font(13, Font.PLAIN), Color.WHITE);
    }

    // ==================== 系统架构图 ====================

    private void drawArchitecture(Graphics2D g) {
        g.setColor(color("#f5f7fa"));
        g.fillRect(0, 0, W, H);

        drawTextCentered(g, "系统架构图", W / 2, 40, font(20, Font.BOLD), color("#262626"));

        String[] layers = {"展示层（前端）", "应用层（后端服务）", "服务层（中间件）", "数据层（存储）"};
        String[] layerColors = {"#e6f7ff", "#f6ffed", "#fff7e6", "#fff1f0"};
        String[] borderColors = {"#1890ff", "#52c41a", "#faad14", "#f5222d"};

        int layerH = 100;
        int gap = 20;
        int totalH = layers.length * layerH + (layers.length - 1) * gap;
        int startY = 70;

        for (int i = 0; i < layers.length; i++) {
            int y = startY + i * (layerH + gap);
            int x = 80, w = W - 160;
            roundRect(g, x, y, w, layerH, 8, color(layerColors[i]));
            g.setColor(color(borderColors[i]));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x, y, w, layerH, 8, 8);
            g.setStroke(new BasicStroke(1));
            drawText(g, layers[i], x + 20, y + 30, font(15, Font.BOLD), color("#262626"));

            String[] mods = switch (i) {
                case 0 -> new String[]{"登录页", "工作台", "业务录入", "审批处理", "报表分析"};
                case 1 -> new String[]{"认证授权", "业务管理", "流程引擎", "统计分析", "文件导出"};
                case 2 -> new String[]{"Redis 缓存", "MinIO 存储", "异步任务", "JWT 鉴权"};
                default -> new String[]{"MySQL", "Redis", "MinIO"};
            };
            int mx = x + 120;
            for (String m : mods) {
                roundRect(g, mx, y + 40, 100, 36, 4, Color.WHITE);
                g.setColor(color(borderColors[i]));
                g.drawRoundRect(mx, y + 40, 100, 36, 4, 4);
                drawTextCentered(g, m, mx + 50, y + 63, font(12, Font.PLAIN), color("#595959"));
                mx += 115;
            }
        }
    }

    // ==================== Windows IP 配置对话框 ====================

    private void drawWinIpConfig(Graphics2D g) {
        g.setColor(color("#f0f0f0"));
        g.fillRect(0, 0, W, H);

        // 对话框
        int dw = 480, dh = 420;
        int dx = (W - dw) / 2, dy = (H - dh) / 2;
        g.setColor(Color.WHITE);
        g.fillRoundRect(dx, dy, dw, dh, 8, 8);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(dx, dy, dw, dh, 8, 8);

        // 标题栏
        g.setColor(color("#f5f5f5"));
        g.fillRoundRect(dx, dy, dw, 40, 8, 8);
        drawText(g, "Internet 协议版本 4 (TCP/IPv4) 属性", dx + 16, dy + 26, font(13, Font.BOLD), color("#262626"));
        drawText(g, "×", dx + dw - 24, dy + 26, font(14, Font.PLAIN), color("#595959"));

        // 内容
        int cy = dy + 60;
        drawText(g, "如果网络支持此功能，则可以获取自动指派的", dx + 16, cy, font(12, Font.PLAIN), color("#595959"));
        drawText(g, "IP 设置。否则，你需要从网络系统管理员处获得", dx + 16, cy + 18, font(12, Font.PLAIN), color("#595959"));
        drawText(g, "适当的 IP 设置。", dx + 16, cy + 36, font(12, Font.PLAIN), color("#595959"));

        // 单选按钮
        cy += 70;
        g.setColor(color("#1890ff"));
        g.drawOval(dx + 20, cy, 14, 14);
        g.fillOval(dx + 24, cy + 4, 6, 6);
        drawText(g, "使用下面的 IP 地址(S):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#262626"));

        // IP 地址
        cy += 30;
        drawText(g, "IP 地址(I):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#595959"));
        g.setColor(Color.WHITE);
        g.fillRect(dx + 150, cy, 200, 24);
        g.setColor(color("#d9d9d9"));
        g.drawRect(dx + 150, cy, 200, 24);
        drawText(g, "192 . 168 . 1 . 100", dx + 156, cy + 16, font(12, Font.PLAIN), color("#262626"));

        // 子网掩码
        cy += 30;
        drawText(g, "子网掩码(U):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#595959"));
        g.setColor(Color.WHITE);
        g.fillRect(dx + 150, cy, 200, 24);
        g.setColor(color("#d9d9d9"));
        g.drawRect(dx + 150, cy, 200, 24);
        drawText(g, "255 . 255 . 255 . 0", dx + 156, cy + 16, font(12, Font.PLAIN), color("#262626"));

        // 默认网关
        cy += 30;
        drawText(g, "默认网关(D):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#595959"));
        g.setColor(Color.WHITE);
        g.fillRect(dx + 150, cy, 200, 24);
        g.setColor(color("#d9d9d9"));
        g.drawRect(dx + 150, cy, 200, 24);
        drawText(g, "192 . 168 . 1 . 1", dx + 156, cy + 16, font(12, Font.PLAIN), color("#262626"));

        // DNS
        cy += 50;
        g.setColor(color("#1890ff"));
        g.drawOval(dx + 20, cy, 14, 14);
        g.fillOval(dx + 24, cy + 4, 6, 6);
        drawText(g, "使用下面的 DNS 服务器地址(E):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#262626"));

        cy += 30;
        drawText(g, "首选 DNS 服务器(P):", dx + 40, cy + 12, font(12, Font.PLAIN), color("#595959"));
        g.setColor(Color.WHITE);
        g.fillRect(dx + 180, cy, 170, 24);
        g.setColor(color("#d9d9d9"));
        g.drawRect(dx + 180, cy, 170, 24);
        drawText(g, "8 . 8 . 8 . 8", dx + 186, cy + 16, font(12, Font.PLAIN), color("#262626"));

        // 按钮
        int by = dy + dh - 50;
        g.setColor(color("#1890ff"));
        g.fillRoundRect(dx + dw - 200, by, 80, 28, 4, 4);
        drawTextCentered(g, "确定", dx + dw - 160, by + 18, font(13, Font.BOLD), Color.WHITE);
        g.setColor(color("#f5f5f5"));
        g.fillRoundRect(dx + dw - 110, by, 80, 28, 4, 4);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(dx + dw - 110, by, 80, 28, 4, 4);
        drawTextCentered(g, "取消", dx + dw - 70, by + 18, font(13, Font.PLAIN), color("#595959"));
    }

    // ==================== Windows 文件管理器 ====================

    private void drawWinExplorer(Graphics2D g) {
        g.setColor(color("#f0f0f0"));
        g.fillRect(0, 0, W, H);

        // 窗口
        int wx = 50, wy = 30, ww = W - 100, wh = H - 60;
        g.setColor(Color.WHITE);
        g.fillRoundRect(wx, wy, ww, wh, 8, 8);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(wx, wy, ww, wh, 8, 8);

        // 标题栏
        g.setColor(color("#f5f5f5"));
        g.fillRoundRect(wx, wy, ww, 36, 8, 8);
        drawText(g, "C:\\ruanzhu", wx + 16, wy + 24, font(13, Font.PLAIN), color("#262626"));

        // 地址栏
        int ay = wy + 44;
        g.setColor(Color.WHITE);
        g.fillRect(wx + 8, ay, ww - 16, 28);
        g.setColor(color("#d9d9d9"));
        g.drawRect(wx + 8, ay, ww - 16, 28);
        drawText(g, "C:\\ruanzhu", wx + 16, ay + 18, font(12, Font.PLAIN), color("#595959"));

        // 文件列表表头
        int hy = ay + 36;
        g.setColor(color("#fafafa"));
        g.fillRect(wx + 8, hy, ww - 16, 28);
        drawText(g, "名称", wx + 16, hy + 18, font(12, Font.BOLD), color("#595959"));
        drawText(g, "修改日期", wx + 400, hy + 18, font(12, Font.BOLD), color("#595959"));
        drawText(g, "类型", wx + 600, hy + 18, font(12, Font.BOLD), color("#595959"));
        drawText(g, "大小", wx + 750, hy + 18, font(12, Font.BOLD), color("#595959"));

        // 文件列表
        String[][] files = {
                {"backend", "2026/9/15 10:30", "文件文件夹", ""},
                {"frontend", "2026/9/15 10:30", "文件文件夹", ""},
                {"docker-compose.yml", "2026/9/10 14:20", "YML 文件", "5 KB"},
                {".env", "2026/9/10 14:00", "环境变量", "1 KB"},
                {"README.md", "2026/9/10 14:00", "MD 文件", "3 KB"},
                {"Dockerfile", "2026/9/10 14:00", "Docker 文件", "2 KB"}
        };
        for (int i = 0; i < files.length; i++) {
            int ry = hy + 28 + i * 28;
            if (ry > wy + wh - 30) break;
            if (i % 2 == 1) {
                g.setColor(new Color(250, 250, 250));
                g.fillRect(wx + 8, ry, ww - 16, 28);
            }
            drawText(g, files[i][0], wx + 32, ry + 18, font(12, Font.PLAIN), color("#262626"));
            drawText(g, files[i][1], wx + 400, ry + 18, font(12, Font.PLAIN), color("#8c8c8c"));
            drawText(g, files[i][2], wx + 600, ry + 18, font(12, Font.PLAIN), color("#8c8c8c"));
            drawText(g, files[i][3], wx + 750, ry + 18, font(12, Font.PLAIN), color("#8c8c8c"));
            // 文件夹图标
            g.setColor(color("#faad14"));
            g.fillRoundRect(wx + 16, ry + 6, 14, 12, 2, 2);
        }
    }

    // ==================== Windows 服务管理 ====================

    private void drawWinService(Graphics2D g) {
        g.setColor(color("#f0f0f0"));
        g.fillRect(0, 0, W, H);

        int wx = 50, wy = 30, ww = W - 100, wh = H - 60;
        g.setColor(Color.WHITE);
        g.fillRoundRect(wx, wy, ww, wh, 8, 8);
        g.setColor(color("#d9d9d9"));
        g.drawRoundRect(wx, wy, ww, wh, 8, 8);

        g.setColor(color("#f5f5f5"));
        g.fillRoundRect(wx, wy, ww, 36, 8, 8);
        drawText(g, "服务", wx + 16, wy + 24, font(13, Font.PLAIN), color("#262626"));

        int hy = wy + 44;
        g.setColor(color("#fafafa"));
        g.fillRect(wx + 8, hy, ww - 16, 28);
        String[] cols = {"名称", "描述", "状态", "启动类型"};
        int[] colX = {wx + 16, wx + 300, wx + 600, wx + 750};
        for (int i = 0; i < cols.length; i++) {
            drawText(g, cols[i], colX[i], hy + 18, font(12, Font.BOLD), color("#595959"));
        }

        String[][] services = {
                {"MySQL", "MySQL 数据库服务", "正在运行", "自动"},
                {"Redis", "Redis 缓存服务", "正在运行", "自动"},
                {"MinIO", "MinIO 对象存储", "正在运行", "自动"},
                {"ruanzhu", "软著材料 AI 生产系统", "正在运行", "自动"}
        };
        for (int i = 0; i < services.length; i++) {
            int ry = hy + 28 + i * 32;
            if (ry > wy + wh - 30) break;
            if (i % 2 == 1) {
                g.setColor(new Color(250, 250, 250));
                g.fillRect(wx + 8, ry, ww - 16, 32);
            }
            drawText(g, services[i][0], colX[0], ry + 20, font(12, Font.PLAIN), color("#262626"));
            drawText(g, services[i][1], colX[1], ry + 20, font(12, Font.PLAIN), color("#8c8c8c"));
            // 状态着色
            Color sc = services[i][2].equals("正在运行") ? color("#52c41a") : color("#f5222d");
            g.setColor(sc);
            g.fillOval(colX[2] - 16, ry + 14, 8, 8);
            drawText(g, services[i][2], colX[2], ry + 20, font(12, Font.PLAIN), sc);
            drawText(g, services[i][3], colX[3], ry + 20, font(12, Font.PLAIN), color("#8c8c8c"));
        }
    }

    // ==================== 公共框架（顶栏+侧边栏） ====================

    private void drawChrome(Graphics2D g, String activeModule) {
        // 背景
        g.setColor(color("#f0f2f5"));
        g.fillRect(0, 0, W, H);

        // 顶栏
        g.setColor(color("#001529"));
        g.fillRect(0, 0, W, 56);
        drawText(g, htmlGen.getSoftwareName(), 24, 36, font(17, Font.BOLD), Color.WHITE);
        drawText(g, industry.headerRole, W - 130, 36, font(13, Font.PLAIN), new Color(255, 255, 255, 200));

        // 侧边栏
        g.setColor(color("#001529"));
        g.fillRect(0, 56, 220, H - 56);

        int my = 80;
        drawText(g, "工作台", 30, my, font(14, Font.PLAIN),
                activeModule.equals("工作台") ? Color.WHITE : new Color(255, 255, 255, 180));
        if (activeModule.equals("工作台")) {
            g.setColor(color("#1890ff"));
            g.fillRect(0, my - 16, 3, 22);
        }
        my += 44;

        for (Map.Entry<String, List<String>> e : htmlGen.getModules().entrySet()) {
            boolean active = e.getKey().equals(activeModule);
            drawText(g, e.getKey(), 30, my, font(14, Font.PLAIN),
                    active ? Color.WHITE : new Color(255, 255, 255, 180));
            if (active) {
                g.setColor(color("#1890ff"));
                g.fillRect(0, my - 16, 3, 22);
            }
            my += 44;
            // 子菜单
            if (active && e.getValue() != null) {
                g.setColor(new Color(255, 255, 255, 60));
                for (String sub : e.getValue()) {
                    if (my > H - 30) break;
                    String subTxt = sub.length() > 10 ? sub.substring(0, 10) + ".." : sub;
                    drawText(g, subTxt, 50, my, font(12, Font.PLAIN), new Color(255, 255, 255, 150));
                    my += 34;
                }
            }
        }
    }
}
