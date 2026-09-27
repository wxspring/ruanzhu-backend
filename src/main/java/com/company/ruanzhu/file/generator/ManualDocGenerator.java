package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;

/**
 * 说明文档：严格 60 页 = 前 30 页 + 后 30 页，每页 30 行。
 * <p>
 * 不足 1800 行空行补齐；超 1800 行取前 900 + 末尾 900。
 * 分页符：前 29 + 中间 1 + 后 29 = 59 个 = 60 页整。
 */
@Slf4j
public class ManualDocGenerator implements DocumentGenerator {

    static final int LPP = 30;
    static final int FP  = 30;
    static final int BP  = 30;
    static final int FL  = LPP * FP;   // 900
    static final int BL  = LPP * BP;   // 900
    static final int TOTAL_OUT_LINES = FL + BL; // 1800

    private static final String FONT_CN = "宋体";
    private static final String FONT_H  = "黑体";
    private static final int FS_BODY = 10;
    private static final int FS_H1  = 14;
    private static final int FS_H2  = 12;
    private static final int FS_TITLE = 18;
    private static final int LSP = 320; // 16pt 固定行距 twips

    private final ProjectService projectService;
    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;
    private final Long projectId;

    public ManualDocGenerator(ProjectService projectService,
                              FileRecordRepository fileRecordRepository,
                              StorageClient storageClient,
                              Long projectId) {
        this.projectService = projectService;
        this.fileRecordRepository = fileRecordRepository;
        this.storageClient = storageClient;
        this.projectId = projectId;
    }

    @Override public byte[] generateWord() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();

        try (XWPFDocument doc = new XWPFDocument()) {
            page(doc);
            hf(doc, project.getName(), summary != null ? summary.getVersion() : "V1.0");

            // 收集所有逻辑行（扉页标题、版本、目录、章节标题、正文段等每个都算 1 行，统一计数才符合"每页30行"硬规格）
            List<LineItem> all = collectAllLines(project, summary);
            int N = all.size();
            List<LineItem> front = sliceFront(all);   // 固定 900
            List<LineItem> back  = sliceBack(all);    // 固定 900

            writeBlock(doc, front, true);     // 前 900 行，页 30 末也 break → 30 个 = 跳到新页 31
            writeBlock(doc, back, false);      // 后 900 行，末尾不 break → 29 个
            // 总段数 = 900 + 900 = 1800；总 break = 30 + 29 = 59 → 正好 60 页 ✅

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            log.info("说明文档：原总行数={} 输出=60页(前30+后30)x{}行 中间省略={}",
                    N, LPP, Math.max(0, N - TOTAL_OUT_LINES));
            return out.toByteArray();
        } catch (Exception e) {
            log.error("generateWord failed", e);
            throw new RuntimeException("生成说明文档失败", e);
        }
    }

    @Override public String getFileName() {
        return projectService.getProjectById(projectId).getName() + "说明.docx";
    }

    /**
     * 用 iText 直接生成 PDF。A4，每页 30 行，中文用系统宋体/黑体。
     */
    @Override public byte[] generatePdf() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();
        List<LineItem> allLines = collectAllLines(project, summary);

        List<LineItem> front = sliceFront(allLines);
        List<LineItem> back  = sliceBack(allLines);

        com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4, 28.3f, 28.3f, 22.7f, 22.7f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont song = BaseFont.createFont("C:\\Windows\\Fonts\\simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont hei  = BaseFont.createFont("C:\\Windows\\Fonts\\simhei.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            float leading = 16f; // 与 DOCX 一致

            List<LineItem> all = new ArrayList<>(front);
            all.addAll(back);

            int lineNo = 0;
            for (LineItem item : all) {
                lineNo++;
                if (item.kind == Kind.IMAGE && item.imageData != null) {
                    try {
                        com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(item.imageData);
                        img.scaleToFit(420, 252);
                        img.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                        doc.add(img);
                    } catch (Exception e) {
                        log.warn("PDF 嵌入图片失败", e);
                    }
                } else {
                    float fs;
                    BaseFont bf;
                    int style = Font.NORMAL;
                    switch (item.kind) {
                        case TITLE: fs = FS_TITLE; bf = hei; break;
                        case H1:    fs = FS_H1;    bf = hei; break;
                        case H2:    fs = FS_H2;    bf = hei; break;
                        default:    fs = FS_BODY;  bf = song; break;
                    }
                    Font font = new Font(bf, fs, style, BaseColor.BLACK);
                    Paragraph p = new Paragraph(item.text == null ? "" : item.text, font);
                    p.setLeading(leading);
                    p.setSpacingAfter(0);
                    p.setSpacingBefore(0);
                    doc.add(p);
                }
                if (lineNo % LPP == 0 && lineNo < all.size()) {
                    doc.newPage();
                }
            }
            doc.close();
            log.info("说明文档 PDF 生成：{} 行，每页 {} 行", all.size(), LPP);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("generatePdf failed", e);
            throw new RuntimeException("生成说明文档 PDF 失败", e);
        }
    }

    // --- 切片：900 / 900，内容不足时循环拼接填满，不用空行 ---

    private List<LineItem> sliceFront(List<LineItem> src) {
        List<LineItem> expanded = ensureAtLeast(src, TOTAL_OUT_LINES);
        return new ArrayList<>(expanded.subList(0, FL));
    }

    private List<LineItem> sliceBack(List<LineItem> src) {
        List<LineItem> expanded = ensureAtLeast(src, TOTAL_OUT_LINES);
        return new ArrayList<>(expanded.subList(expanded.size() - BL, expanded.size()));
    }

    // --- 行模型 ---

    enum Kind { TITLE, VERSION, TOC, H1, H2, BODY, BLANK, IMAGE }

    static class LineItem {
        Kind kind; String text; boolean bold; byte[] imageData; String imageFormat;
        LineItem(Kind k, String t){ this(k,t,false); }
        LineItem(Kind k, String t, boolean b){ kind=k; text=t==null?"":t; bold=b; }
        LineItem(byte[] img, String fmt){ kind=Kind.IMAGE; text=""; imageData=img; imageFormat=fmt; }
    }

    private List<LineItem> collectAllLines(ProjectVO project, SoftwareSummaryVO summary) {
        List<LineItem> r = new ArrayList<>();
        String name = project.getName();
        String ver = summary != null && summary.getVersion() != null ? summary.getVersion() : "V1.0";
        String functionMenu = summary != null ? summary.getFunctionMenu() : null;

        // 生成系统截图（登录页、首页、各模块列表/表单页）
        Map<String, byte[]> screenshots = captureScreenshots(name, functionMenu);
        // 解析功能菜单：模块 -> 功能列表
        Map<String, List<String>> modules = parseFunctionMenu(functionMenu);

        // 扉页信息（参与30行计数）
        for (int i=0;i<3;i++) r.add(new LineItem(Kind.BLANK,""));
        r.add(new LineItem(Kind.TITLE, name + " 操作说明书", true));
        r.add(new LineItem(Kind.BLANK,""));
        r.add(new LineItem(Kind.VERSION, "版本：" + ver));
        r.add(new LineItem(Kind.BLANK,""));
        r.add(new LineItem(Kind.BODY, "著作权人：" + name));
        r.add(new LineItem(Kind.BODY, "开发完成日期：-"));
        for (int i=0;i<2;i++) r.add(new LineItem(Kind.BLANK,""));
        // 目录占位
        r.add(new LineItem(Kind.TOC, "目录（Word 中右键更新域以生成）"));
        r.add(new LineItem(Kind.BLANK,""));
        String[] ch = new String[]{"第一章 软件概述","第二章 安装与配置","第三章 功能说明","第四章 操作指南","第五章 维护与常见问题"};
        for (String s : ch) r.add(new LineItem(Kind.BODY, s));
        for (int i=0;i<2;i++) r.add(new LineItem(Kind.BLANK,""));

        // 正文：优先读用户 MANUAL HTML，否则按 summary 字段拼 5 章
        List<LineItem> body = new ArrayList<>();
        for (String s : collectBodyPlain(summary)) {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) { body.add(new LineItem(Kind.BLANK, "")); continue; }
            if (t.matches("^第.+[章节].*")) body.add(new LineItem(Kind.H1, t, true));
            else if (t.matches("^\\d+(\\.\\d+)*\\s+.+") || t.matches("^\\d+[、.．]\\s*.+")) body.add(new LineItem(Kind.H2, t, true));
            else body.add(new LineItem(Kind.BODY, t));
        }
        r.addAll(body);

        // 在"1.4 系统架构"段落后插入架构图
        byte[] archImg = generateArchitectureDiagram();
        if (archImg != null) {
            int insertIdx = -1;
            for (int i = 0; i < r.size(); i++) {
                if (r.get(i).text.contains("1.4 系统架构")) { insertIdx = i; break; }
            }
            if (insertIdx >= 0) {
                int endIdx = r.size();
                for (int i = insertIdx + 1; i < r.size(); i++) {
                    Kind k = r.get(i).kind;
                    if (k == Kind.H1 || k == Kind.H2) { endIdx = i; break; }
                }
                r.add(endIdx, new LineItem(archImg, "png"));
                r.add(endIdx + 1, new LineItem(Kind.BODY, "图1-1 系统架构图", false));
            }
        }

        // 在操作指南章节插入系统截图
        insertOperationScreenshots(r, modules, screenshots);

        // 不到 1 页（30行）补齐空行，保证第一页也不会只有少量内容
        while (r.size() < LPP) r.add(new LineItem(Kind.BLANK, ""));
        return r;
    }

    /** 解析功能菜单：每行 "功能名：模块名" */
    private Map<String, List<String>> parseFunctionMenu(String functionMenu) {
        Map<String, List<String>> modules = new LinkedHashMap<>();
        if (functionMenu == null || functionMenu.trim().isEmpty()) return modules;
        for (String raw : functionMenu.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("[:：/\\\\]", 2);
            String func, module;
            if (parts.length == 2) { func = parts[0].trim(); module = parts[1].trim(); }
            else { func = line; module = "系统功能"; }
            if (func.isEmpty()) continue;
            modules.computeIfAbsent(module, k -> new ArrayList<>()).add(func);
        }
        return modules;
    }

    /** 调用浏览器生成系统截图，返回 标识 -> PNG字节 */
    private Map<String, byte[]> captureScreenshots(String softwareName, String functionMenu) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try {
            File workDir = new File(System.getProperty("java.io.tmpdir"), "ruanzhu_shots_" + projectId);
            SystemScreenshotService svc = new SystemScreenshotService(softwareName, functionMenu, workDir);
            Map<String, File> files = svc.captureAll();
            for (Map.Entry<String, File> e : files.entrySet()) {
                result.put(e.getKey(), Files.readAllBytes(e.getValue().toPath()));
            }
            // 截图嵌入后清理临时目录
            svc.cleanup();
        } catch (Exception e) {
            log.warn("生成系统截图失败，将跳过截图嵌入", e);
        }
        return result;
    }

    /** 在操作指南章节插入系统截图 */
    private void insertOperationScreenshots(List<LineItem> r, Map<String, List<String>> modules, Map<String, byte[]> shots) {
        if (shots.isEmpty()) return;
        // 1) 登录截图：找到"4.1 登录系统"段落后插入
        insertImageAfterHeading(r, "4.1 登录系统", shots.get("login"), "图4-1 系统登录页面");
        // 2) 首页截图：找到登录截图后或"4.2"前插入
        insertImageAfterHeading(r, "工作台", shots.get("home"), "图4-2 系统首页（工作台）");
        // 3) 各模块截图：按顺序插入到对应操作步骤后
        int modIdx = 1;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            String key = "list_" + modIdx;
            String fkey = "form_" + modIdx;
            String figNo = String.format("图4-%d", modIdx + 2);
            String figNo2 = String.format("图4-%d", modIdx + 2 + modules.size());
            // 找到该模块对应的 H2 标题（如 "4.2 来料质检登记"）后插入列表页截图
            // 这里用模块名模糊匹配
            String heading = findHeadingByModule(r, e.getKey());
            if (heading != null) {
                insertImageAfterHeading(r, heading, shots.get(key), figNo + " " + e.getKey() + "列表页面");
                insertImageAfterHeading(r, heading, shots.get(fkey), figNo2 + " " + e.getKey() + "录入页面");
            }
            modIdx++;
        }
    }

    /** 在正文中查找包含模块名的 H2 标题文本 */
    private String findHeadingByModule(List<LineItem> r, String module) {
        for (LineItem it : r) {
            if (it.kind == Kind.H2 && it.text.contains(module)) return it.text;
        }
        return null;
    }

    /** 在指定标题段落后插入图片+图注 */
    private void insertImageAfterHeading(List<LineItem> r, String headingText, byte[] img, String caption) {
        if (img == null) return;
        int idx = -1;
        for (int i = 0; i < r.size(); i++) {
            if (r.get(i).kind == Kind.H2 && r.get(i).text.contains(headingText)) { idx = i; break; }
        }
        if (idx < 0) {
            // H2 没找到，试试 BODY
            for (int i = 0; i < r.size(); i++) {
                if (r.get(i).text.contains(headingText)) { idx = i; break; }
            }
        }
        if (idx < 0) return;
        // 找到该段落下一个 H1/H2 的位置
        int endIdx = r.size();
        for (int i = idx + 1; i < r.size(); i++) {
            Kind k = r.get(i).kind;
            if (k == Kind.H1 || k == Kind.H2) { endIdx = i; break; }
        }
        r.add(endIdx, new LineItem(img, "png"));
        r.add(endIdx + 1, new LineItem(Kind.BODY, caption, false));
    }

    /** 内容不足 1800 行时循环拼接，保证前后 30 页都有内容 */
    private static List<LineItem> ensureAtLeast(List<LineItem> src, int minLen) {
        if (src.size() >= minLen) return src;
        List<LineItem> out = new ArrayList<>(minLen);
        while (out.size() < minLen) out.addAll(src);
        return out;
    }

    private List<String> collectBodyPlain(SoftwareSummaryVO summary) {
        List<FileRecord> saved = fileRecordRepository.findByProjectIdAndFileType(projectId, "MANUAL");
        if (!saved.isEmpty()) {
            FileRecord latest = saved.stream()
                    .max(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 0))
                    .get();
            byte[] data = storageClient.download(latest.getStoragePath());
            String html = new String(data, StandardCharsets.UTF_8);
            List<String> lines = htmlToPlainLines(html);
            log.info("说明文档：用保存的 MANUAL name={}, size={}B, lines={}",
                    latest.getFileName(), data.length, lines.size());
            return lines;
        }
        // 无保存 MANUAL 时，按 summary 字段 + 功能菜单动态拼接 5 章
        Map<String, List<String>> modules = parseFunctionMenu(summary != null ? summary.getFunctionMenu() : null);
        List<String> out = new ArrayList<>();
        ch1(out, summary); ch2(out, summary); ch3(out, summary, modules); ch4(out, summary, modules); ch5(out, summary);
        log.info("说明文档：MANUAL 缺失，回退章节拼接 lines={} modules={}", out.size(), modules.size());
        return out;
    }

    private void ch1(List<String> out, SoftwareSummaryVO s) {
        out.add("第一章 软件概述");
        out.add("1.1 软件简介");
        multi(out, s!=null?s.getPurpose():null,
                "本软件是一套面向汽车零配件质检环节的数据研判平台，旨在为质检管理人员提供" +
                "从未料质检登记、缺陷研判、不合格品处置到质检任务调度的全流程数字化管理能力。" +
                "平台通过融合工业扫码与量具数据自动采集技术，实现质检数据的实时录入与精准追溯，" +
                "支持缺陷分布的智能研判与高风险批次的自动标记，帮助质检团队及时掌握来料质量状况" +
                "并快速响应不合格品处置需求。");
        out.add("系统采用前后端分离架构，后端基于 Spring Boot 框架开发，前端基于 React 框架构建，" +
                "数据存储采用 MySQL 关系型数据库，文件存储采用 MinIO 对象存储。系统支持多角色分级" +
                "权限管理，涵盖运营管理、工艺管理、化验管理、库管等多种岗位角色，各角色可根据职责" +
                "访问相应功能模块。");
        out.add("1.2 应用领域");
        multi(out, s!=null?s.getTargetDomain():null,
                "本平台主要应用于汽车整车及零部件制造企业的质检部门，适用于以下场景：");
        out.add("（1）汽车零配件来料质检登记与结果追溯；");
        out.add("（2）零配件缺陷类型判定与严重等级划分；");
        out.add("（3）缺陷分布趋势分析与高风险批次识别；");
        out.add("（4）不合格品处置申请与让步接收审批；");
        out.add("（5）质检任务下发与执行状态跟踪；");
        out.add("（6）质检数据统计分析与报表导出。");
        out.add("适用对象包括：质检主管、质检工程师、来料检验员、不合格品处置专员、生产调度人员等。");
        out.add("1.3 技术特点");
        multi(out, s!=null?s.getTechFeatures():null,
                "本平台具备以下技术特点：");
        out.add("（1）前后端分离架构，后端 Spring Boot + 前端 React，支持独立部署与水平扩展；");
        out.add("（2）工业扫码与量具数据自动采集，减少人工录入错误，提高数据准确性；");
        out.add("（3）缺陷研判规则引擎，支持自定义缺陷类型与判定条件，灵活适配不同品类零配件；");
        out.add("（4）高风险批次自动标记，基于缺陷率阈值实时预警；");
        out.add("（5）多级权限控制，基于角色的访问控制（RBAC），确保数据安全；");
        out.add("（6）数据可视化展示，支持缺陷分布图表、趋势曲线、批次对比等多种分析视图；");
        out.add("（7）操作留痕与审计日志，所有关键操作均记录日志，支持追溯；");
        out.add("（8）支持移动端与触控一体机，适配车间现场使用环境。");
        out.add("1.4 系统架构");
        out.add("本平台采用分层架构设计，自下而上分为数据层、服务层、应用层与展示层：");
        out.add("（1）数据层：包括 MySQL 数据库（存储业务数据）和 MinIO 对象存储（存储质检报告、" +
                "附件等文件），同时支持 Redis 缓存用于提升高频访问数据的响应速度；");
        out.add("（2）服务层：基于 Spring Boot 构建，提供用户认证、权限管理、质检数据管理、" +
                "缺陷研判、不合格品处置、任务调度、统计分析等核心业务服务；");
        out.add("（3）应用层：通过 RESTful API 对外提供服务，支持前端 Web 应用、移动端及第三方系统集成；");
        out.add("（4）展示层：基于 React 构建的 Web 前端，提供项目看板、质检登记、缺陷研判、" +
                "报表分析等功能界面，同时支持响应式布局适配不同设备。");
    }
    private void ch2(List<String> out, SoftwareSummaryVO s) {
        out.add("第二章 安装与配置");
        out.add("2.1 运行环境");
        out.add("2.1.1 硬件环境");
        out.add("服务器端最低配置要求：");
        out.add("（1）CPU：4核及以上，推荐 8核；");
        out.add("（2）内存：8GB 以上，推荐 16GB；");
        out.add("（3）硬盘：50GB 可用空间，推荐使用 SSD；");
        out.add("（4）网络：千兆以太网。");
        out.add("客户端配置要求：");
        out.add("（1）PC：CPU 双核 2GHz 以上，内存 4GB 以上；");
        out.add("（2）移动终端：Android 8.0+ 或 iOS 12.0+；");
        out.add("（3）车间触控一体机：15寸以上触摸屏，Windows 10 IoT。");
        multi(out, s!=null?s.getRunHardware():null, null);
        out.add("2.1.2 软件环境");
        out.add("服务器端：");
        out.add("（1）操作系统：Windows Server 2019+ 或 CentOS 7+/Ubuntu 20.04+；");
        out.add("（2）运行时：JDK 17+；");
        out.add("（3）数据库：MySQL 8.0+；");
        out.add("（4）对象存储：MinIO（可选，默认本地文件系统）；");
        out.add("（5）缓存：Redis 6.0+（可选，用于会话与热点数据缓存）。");
        out.add("客户端：");
        out.add("（1）浏览器：Chrome 90+ / Edge 90+ / Firefox 88+；");
        out.add("（2）分辨率：建议 1366×768 及以上。");
        multi(out, s!=null?s.getRunPlatform():null, null);
        out.add("2.2 安装步骤");
        out.add("2.2.1 数据库准备");
        out.add("（1）安装 MySQL 8.0 并启动服务；");
        out.add("（2）创建数据库：CREATE DATABASE ruanzhu DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;");
        out.add("（3）创建数据库用户并授权：CREATE USER 'ruanzhu'@'%' IDENTIFIED BY 'your_password'; " +
                "GRANT ALL PRIVILEGES ON ruanzhu.* TO 'ruanzhu'@'%'; FLUSH PRIVILEGES;");
        out.add("（4）执行数据库初始化脚本：mysql -u ruanzhu -p ruanzhu < schema.sql。");
        out.add("2.2.2 后端部署");
        out.add("（1）从发布包获取后端可执行文件 ruanzhu.jar；");
        out.add("（2）将 ruanzhu.jar 上传至服务器指定目录，如 /opt/ruanzhu/；");
        out.add("（3）创建配置文件 application.yml，设置数据库连接、端口、文件存储路径等参数；");
        out.add("（4）启动后端服务：java -jar ruanzhu.jar --spring.config.location=application.yml；");
        out.add("（5）验证服务启动：访问 http://服务器IP:8080/api/health，返回 success 表示启动成功。");
        out.add("2.2.3 前端部署");
        out.add("（1）从发布包获取前端构建产物 dist 目录；");
        out.add("（2）方式一：将 dist 目录部署至 Nginx，配置反向代理指向后端 API；");
        out.add("（3）方式二：将 dist 目录放置于后端 static 目录，由后端直接提供静态资源服务；");
        out.add("（4）配置 Nginx 示例：server { listen 80; root /usr/share/nginx/html; " +
                "location /api/ { proxy_pass http://localhost:8080/api/; } }。");
        out.add("2.2.4 服务启停");
        out.add("（1）启动后端：nohup java -jar ruanzhu.jar --spring.config.location=application.yml > app.log 2>&1 &");
        out.add("（2）停止后端：kill $(pgrep -f ruanzhu.jar)");
        out.add("（3）查看日志：tail -f app.log");
        out.add("2.3 初始配置");
        out.add("2.3.1 系统参数配置");
        out.add("登录系统后进入 系统管理-参数配置，依次配置：");
        out.add("（1）组织信息：机构名称、软件版本、著作权人、开发完成日期；");
        out.add("（2）系统参数：缺陷率阈值、高风险标记规则、质检批次默认抽样比例；");
        out.add("（3）存储配置：文件存储路径、最大上传文件大小、允许的文件类型。");
        out.add("2.3.2 用户与角色配置");
        out.add("（1）进入 系统管理-用户管理，创建各岗位用户账号；");
        out.add("（2）进入 系统管理-角色管理，配置角色权限，包括质检主管、质检工程师、" +
                "来料检验员、不合格品处置专员、生产调度员等；");
        out.add("（3）为用户分配相应角色，用户登录后即可使用对应功能。");
        out.add("2.3.3 基础数据配置");
        out.add("（1）零配件品类维护：录入本企业涉及的零配件品类及对应的检验指标；");
        out.add("（2）缺陷类型维护：定义划痕、裂纹、变形、尺寸超差、镀层脱落等缺陷类型" +
                "及对应的严重等级（轻微、一般、严重、致命）；");
        out.add("（3）供应商维护：录入供应商信息及对应的零配件供应范围。");
    }
    private void ch3(List<String> out, SoftwareSummaryVO s, Map<String, List<String>> modules) {
        out.add("第三章 功能说明");
        String mf = s != null ? s.getMainFunctions() : null;
        if (mf != null && !mf.trim().isEmpty()) {
            multi(out, mf, null);
        } else {
            StringBuilder sb = new StringBuilder("本系统围绕核心业务流程展开，主要功能模块包括：");
            int i = 0;
            for (String mod : modules.keySet()) {
                if (i++ > 0) sb.append("、");
                sb.append(mod);
            }
            sb.append("等。各模块数据互通，形成完整的业务管理链条。");
            out.add(sb.toString());
        }
        int idx = 1;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            out.add("3." + idx + " " + e.getKey());
            out.add(e.getKey() + "模块主要功能包括：");
            int n = 1;
            for (String f : e.getValue()) {
                out.add("（" + cnNum(n) + "）" + f + "：支持对" + f + "相关信息进行录入、查询、修改与管理；");
                n++;
            }
            idx++;
        }
        int reportIdx = idx;
        out.add("3." + reportIdx + " 统计报表");
        out.add("统计报表模块提供多维度的数据分析报表，主要功能包括：");
        out.add("（1）业务数据统计报表：按时间、类型等维度统计业务数据；");
        out.add("（2）趋势分析报表：展示关键指标的变化趋势；");
        out.add("（3）报表导出：支持将报表导出为 Excel、PDF 格式。");
        out.add("3." + (reportIdx + 1) + " 系统管理");
        out.add("系统管理模块提供系统级配置与管理功能，主要功能包括：");
        out.add("（1）用户管理：创建、修改、禁用用户账号，重置密码；");
        out.add("（2）角色管理：配置角色及其对应的菜单权限与数据权限；");
        out.add("（3）菜单管理：维护系统菜单结构与权限标识；");
        out.add("（4）参数配置：配置系统运行参数；");
        out.add("（5）日志审计：查看系统操作日志与登录日志。");
    }

    private void ch4(List<String> out, SoftwareSummaryVO s, Map<String, List<String>> modules) {
        out.add("第四章 操作指南");
        out.add("4.1 登录系统");
        out.add("（1）打开浏览器，在地址栏输入系统访问地址；");
        out.add("（2）在登录页面输入用户名和密码；");
        out.add("（3）首次登录系统会强制要求修改初始密码，请按提示设置新密码；");
        out.add("（4）密码修改成功后，使用新密码重新登录即可进入系统首页（工作台）。");
        out.add("4.2 工作台首页");
        out.add("登录成功后进入系统工作台首页，首页展示内容包括：");
        out.add("（1）统计卡片：展示今日记录数、本月记录数、异常告警数、在线用户数等关键指标；");
        out.add("（2）功能模块入口：以卡片形式展示各功能模块，点击可快速进入对应模块；");
        out.add("（3）最近操作记录：展示系统最近的操作日志，便于了解系统运行状态。");
        int idx = 3;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            out.add("4." + idx + " " + e.getKey());
            out.add("（1）在左侧菜单栏点击 " + e.getKey() + "，展开该模块下的功能菜单；");
            out.add("（2）点击需要操作的功能项，进入对应的列表页面，页面展示已录入的数据记录；");
            out.add("（3）在列表页面可通过顶部搜索框输入关键词进行查询，点击 查询 按钮筛选数据，点击 重置 按钮清空查询条件；");
            out.add("（4）点击 新增 按钮，打开信息录入表单；");
            out.add("（5）在表单中填写各项信息，带 * 号的为必填项；");
            out.add("（6）填写完成后点击 保存 按钮提交数据，系统校验通过后保存成功；");
            out.add("（7）在列表页面可对已有记录进行 查看、编辑 操作，操作完成后系统自动更新数据。");
            int n = 1;
            for (String f : e.getValue()) {
                out.add("（" + cnNum(n + 7) + "）" + f + "：在左侧菜单点击 " + f + "，按上述流程进行操作。");
                n++;
            }
            idx++;
        }
        out.add("4." + idx + " 统计报表");
        idx++;
        out.add("（1）进入 统计报表 菜单，选择需要查看的报表类型；");
        out.add("（2）设置查询条件（如时间段等），点击 查询；");
        out.add("（3）系统展示报表数据与可视化图表；");
        out.add("（4）点击 导出，选择导出格式，下载报表文件。");
        out.add("4." + idx + " 用户与权限管理");
        idx++;
        out.add("（1）用户管理：进入 系统管理-用户管理 页面，点击 新增用户，" +
                "填写用户名、姓名、所属部门、角色，点击 保存；");
        out.add("（2）角色管理：进入 系统管理-角色管理 页面，点击 新增角色，" +
                "配置角色名称、菜单权限、数据权限，点击 保存；");
        out.add("（3）密码重置：在用户列表中选择用户，点击 重置密码，" +
                "系统生成临时密码并通知用户首次登录后修改。");
        out.add("4." + idx + " 系统日志查看");
        out.add("（1）进入 系统管理-操作日志 页面，查看系统操作记录；");
        out.add("（2）可按操作人、操作类型、时间段筛选日志；");
        out.add("（3）进入 系统管理-登录日志 页面，查看用户登录记录。");
    }

    private static String cnNum(int n) {
        String[] cn = {"一","二","三","四","五","六","七","八","九","十"};
        return n >= 1 && n <= 10 ? cn[n-1] : String.valueOf(n);
    }

    private void ch5(List<String> out, SoftwareSummaryVO s) {
        out.add("第五章 维护与常见问题");
        out.add("5.1 系统维护");
        out.add("5.1.1 日常维护");
        out.add("（1）每日检查数据库备份是否正常完成，确认备份文件完整可用；");
        out.add("（2）每日检查服务器磁盘空间，确保可用空间不低于 20%；");
        out.add("（3）每日检查系统服务运行状态，确认后端服务、数据库服务、Nginx 服务正常运行；");
        out.add("（4）关注系统告警信息，及时处理异常告警。");
        out.add("5.1.2 定期维护");
        out.add("（1）每周一次：清理系统日志，检查数据一致性，验证数据库备份可恢复性；");
        out.add("（2）每月一次：安装操作系统与数据库安全补丁，更新密码策略，检查用户权限合理性；");
        out.add("（3）每季度一次：进行系统性能评估，优化慢查询，清理历史归档数据；");
        out.add("（4）每年一次：进行全面系统审计，更新灾难恢复预案，进行应急演练。");
        out.add("5.1.3 数据备份与恢复");
        out.add("（1）数据库备份：使用 mysqldump 工具每日凌晨自动备份，保留最近 30 天备份；");
        out.add("（2）文件备份：MinIO 或本地文件存储每日增量备份，保留最近 90 天；");
        out.add("（3）恢复演练：每季度进行一次备份恢复演练，确保备份可用；");
        out.add("（4）恢复步骤：停止应用服务 → 恢复数据库 → 恢复文件 → 启动应用服务 → 验证数据完整性。");
        out.add("5.2 常见问题");
        out.add("Q1：无法登录系统？");
        out.add("A：请检查以下内容：");
        out.add("（1）账号是否被锁定（连续 5 次输错密码会锁定账号，需管理员解锁）；");
        out.add("（2）密码是否过期（系统默认 90 天密码过期，需修改密码）；");
        out.add("（3）数据库连接是否正常（检查数据库服务状态与连接配置）；");
        out.add("（4）网络是否正常（检查客户端与服务器之间的网络连通性）。");
        out.add("Q2：扫码录入零配件编号无响应？");
        out.add("A：请检查扫码枪是否正常连接，是否已正确安装驱动，扫码枪输入模式是否为" +
                "键盘仿真模式。");
        out.add("Q3：量具数据无法自动采集？");
        out.add("A：请检查量具是否通过 USB 或串口正确连接，量具通信参数（波特率、数据位、" +
                "停止位、校验位）是否与系统配置一致，量具驱动是否已安装。");
        out.add("Q4：报表导出为空？");
        out.add("A：请确认所选时间范围内是否存在数据，当前用户权限是否覆盖对应数据范围，" +
                "查询条件是否设置正确。");
        out.add("Q5：高风险批次标记不准确？");
        out.add("A：请检查 系统管理-参数配置 中的缺陷率阈值设置是否合理，可根据实际业务" +
                "需要调整阈值。");
        out.add("Q6：AI 生成内容失败？");
        out.add("A：请联系管理员检查 AI Key 是否有效、网络是否可访问 AI 服务、账户余额是否充足。");
        out.add("5.3 技术支持");
        out.add("如遇本手册未涵盖的问题，请通过以下方式联系技术支持：");
        out.add("（1）电话：400-XXX-XXXX");
        out.add("（2）邮箱：support@example.com");
        out.add("（3）工作时间：周一至周五 9:00-18:00");
        out.add("联系时请提供软件版本号、问题截图及操作步骤，以便快速定位与解决问题。");
    }

    /**
     * 生成系统架构图 PNG，返回字节数组。
     * 用 Java 2D 绘制四层架构（数据层/服务层/应用层/展示层）。
     */
    private byte[] generateArchitectureDiagram() {
        try {
            int W = 800, H = 480;
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, W, H);

            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 16));
            g.setColor(new Color(30, 60, 120));
            g.drawString("系统架构图", W/2 - 50, 30);

            String[] layers = {"展示层 (React Web / 移动端)", "应用层 (RESTful API)",
                    "服务层 (Spring Boot 业务服务)", "数据层 (MySQL / MinIO / Redis)"};
            Color[] colors = {new Color(180, 210, 255), new Color(150, 200, 230),
                    new Color(120, 180, 210), new Color(90, 150, 190)};

            int y = 60;
            int boxH = 70;
            int gap = 20;
            for (int i = 0; i < layers.length; i++) {
                g.setColor(colors[i]);
                g.fillRoundRect(60, y, W - 120, boxH, 15, 15);
                g.setColor(new Color(30, 60, 120));
                g.drawRoundRect(60, y, W - 120, boxH, 15, 15);
                g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
                g.drawString(layers[i], 80, y + 35);

                // 内部模块标签
                g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
                g.setColor(new Color(60, 60, 60));
                String[] mods;
                switch (i) {
                    case 0: mods = new String[]{"项目看板", "质检登记", "缺陷研判", "报表分析"}; break;
                    case 1: mods = new String[]{"认证授权", "质检管理", "研判分析", "处置审批"}; break;
                    case 2: mods = new String[]{"用户服务", "质检服务", "研判服务", "任务调度"}; break;
                    default: mods = new String[]{"MySQL", "MinIO", "Redis"}; break;
                }
                int mx = 80;
                for (String m : mods) {
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillRoundRect(mx, y + 42, 90, 22, 6, 6);
                    g.setColor(new Color(30, 60, 120));
                    g.drawString(m, mx + 6, y + 57);
                    mx += 100;
                }

                // 箭头
                if (i < layers.length - 1) {
                    g.setColor(new Color(100, 100, 100));
                    int ay = y + boxH + 4;
                    g.drawLine(W/2, ay, W/2, ay + gap - 8);
                    g.fillPolygon(new int[]{W/2, W/2-6, W/2+6}, new int[]{ay+gap-8, ay+gap-16, ay+gap-16}, 3);
                }
                y += boxH + gap;
            }

            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("生成架构图失败", e);
            return null;
        }
    }

    private static void multi(List<String> out, String val, String fallback) {
        String v = (val != null && !val.trim().isEmpty()) ? val : fallback;
        if (v == null) return;
        for (String p : v.split("\r?\n")) {
            String s = p.replace("\t", "    ").trim();
            out.add(s);
        }
    }

    private static List<String> htmlToPlainLines(String html) {
        if (html == null || html.isEmpty()) return java.util.Collections.emptyList();
        String s = html;
        s = s.replaceAll("(?is)<script.*?>.*?</script>", " ");
        s = s.replaceAll("(?is)<style.*?>.*?</style>", " ");
        s = s.replaceAll("(?i)<(br\\s*/?|/p|/div|/h[1-6]|/li|/tr|/blockquote)>", "\n");
        s = s.replaceAll("(?i)<(p|div|h[1-6]|li|tr|blockquote)(\\s[^>]*)?>", "\n");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"");
        List<String> result = new ArrayList<>();
        for (String raw : s.split("\r?\n")) {
            String t = raw.replaceAll("[ \\t\\u00A0]+", " ").trim();
            result.add(t);
        }
        return result;
    }

    // --- 页面、页眉页脚 ---

    private static void page(XWPFDocument doc) {
        CTSectPr sect = doc.getDocument().getBody().isSetSectPr()
                ? doc.getDocument().getBody().getSectPr()
                : doc.getDocument().getBody().addNewSectPr();
        CTPageSz sz = sect.isSetPgSz() ? sect.getPgSz() : sect.addNewPgSz();
        sz.setW(BigInteger.valueOf(11906)); sz.setH(BigInteger.valueOf(16838));
        CTPageMar m = sect.isSetPgMar() ? sect.getPgMar() : sect.addNewPgMar();
        m.setTop(BigInteger.valueOf(mm2t(15))); m.setBottom(BigInteger.valueOf(mm2t(15)));
        m.setLeft(BigInteger.valueOf(mm2t(20))); m.setRight(BigInteger.valueOf(mm2t(20)));
        m.setHeader(BigInteger.valueOf(mm2t(10))); m.setFooter(BigInteger.valueOf(mm2t(10)));
        CTDocGrid g = sect.isSetDocGrid() ? sect.getDocGrid() : sect.addNewDocGrid();
        g.setType(STDocGrid.LINES); g.setLinePitch(BigInteger.valueOf(LSP));
    }
    private static int mm2t(int mm){ return (int) Math.round(mm * 1440.0 / 25.4); }

    private static void hf(XWPFDocument doc, String name, String version) {
        XWPFHeader h = doc.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph hp = h.getParagraphs().isEmpty() ? h.createParagraph() : h.getParagraphs().get(0);
        hp.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = hp.createRun();
        r.setText(name + "  操作说明书   版本：" + version);
        r.setFontSize(8); r.setFontFamily(FONT_CN); r.setColor("666666");

        XWPFFooter f = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph fp = f.getParagraphs().isEmpty() ? f.createParagraph() : f.getParagraphs().get(0);
        fp.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        fr = fp.createRun();
        fr.getCTR().addNewInstrText().setStringValue(" PAGE ");
        fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        fr = fp.createRun(); fr.setText("1");
        fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
        fr = fp.createRun(); fr.setText(" / ");
        fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        fr = fp.createRun();
        fr.getCTR().addNewInstrText().setStringValue(" NUMPAGES ");
        fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        fr = fp.createRun(); fr.setText("1");
        fr = fp.createRun();
        fr.getCTR().addNewFldChar().setFldCharType(STFldCharType.END);
        fr.setFontSize(8); fr.setFontFamily(FONT_CN);
    }

    // --- 写块：30 行一页写满分页 ---

    private static void writeBlock(XWPFDocument doc, List<LineItem> items, boolean pageBreakAtLastLine) {
        int onPage = 0;
        for (int i = 0; i < items.size(); i++) {
            LineItem it = items.get(i);
            onPage++;
            final boolean lastLine = (i == items.size() - 1);
            XWPFParagraph para = doc.createParagraph();
            stylePara(para, it.kind);
            spacing(para);

            if (it.kind == Kind.IMAGE && it.imageData != null) {
                // 嵌入图片
                try {
                    XWPFRun run = para.createRun();
                    int picType = "png".equalsIgnoreCase(it.imageFormat)
                            ? XWPFDocument.PICTURE_TYPE_PNG
                            : XWPFDocument.PICTURE_TYPE_JPEG;
                    // 图片宽度约 14cm（适配 A4 正文宽度），按比例
                    run.addPicture(new java.io.ByteArrayInputStream(it.imageData),
                            picType, "diagram.png",
                            Units.toEMU(420), Units.toEMU(252));
                } catch (Exception e) {
                    log.warn("DOCX 嵌入图片失败", e);
                }
            } else {
                int size; String font;
                switch (it.kind) {
                    case TITLE:   size = FS_TITLE; font = FONT_H;  break;
                    case VERSION: size = FS_H2;    font = FONT_CN; break;
                    case TOC:     size = FS_BODY;  font = FONT_CN; break;
                    case H1:      size = FS_H1;    font = FONT_H;  break;
                    case H2:      size = FS_H2;    font = FONT_H;  break;
                    default:      size = FS_BODY;  font = FONT_CN; break;
                }
                XWPFRun run = para.createRun();
                run.setText(it.text);
                run.setFontSize(size);
                run.setFontFamily(font);
                run.setBold(it.bold || it.kind == Kind.TITLE || it.kind == Kind.H1 || it.kind == Kind.H2);
            }

            if (onPage == LPP) {
                onPage = 0;
                boolean need = !lastLine || pageBreakAtLastLine;
                if (need) {
                    XWPFRun pb = para.createRun();
                    pb.addBreak(BreakType.PAGE);
                }
            }
        }
    }

    private static void addPageBreakOnly(XWPFDocument doc) {
        XWPFParagraph para = doc.createParagraph();
        spacing(para);
        para.createRun().addBreak(BreakType.PAGE);
    }

    private static void stylePara(XWPFParagraph para, Kind k) {
        switch (k) {
            case TITLE:
            case VERSION:
                para.setAlignment(ParagraphAlignment.CENTER);
                break;
            case TOC:
                para.setSpacingAfter(200);
                break;
            case H1:
                para.setSpacingBefore(60);
                break;
            case H2:
                para.setSpacingBefore(40);
                break;
            case BODY:
                para.setIndentationFirstLine(420);
                break;
            default:
                break;
        }
    }

    private static void spacing(XWPFParagraph para) {
        CTPPr ppr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing sp = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        sp.setBefore(BigInteger.ZERO);
        sp.setAfter(BigInteger.ZERO);
        sp.setLine(BigInteger.valueOf(LSP));
        sp.setLineRule(STLineSpacingRule.EXACT);
    }
}
