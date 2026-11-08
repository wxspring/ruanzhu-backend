package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.common.util.LanguageEnvironment;
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

import java.io.ByteArrayInputStream;
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

    // 图片按"逻辑行"折算：权重由实际渲染高度推导，使 DOCX 与 PDF 分页一致
    // 渲染规则：宽度 420pt（适配 A4 正文宽度），高度按原始宽高比等比缩放，最高 320pt
    static final int IMAGE_LINE_WEIGHT = 10;
    static final float IMG_WIDTH_PT = 420f;
    static final float IMG_MAX_HEIGHT_PT = 320f;
    static final float LINE_PT = 16f;

    private static final String FONT_CN = "宋体";
    private static final String FONT_H  = "黑体";
    private static final int FS_BODY = 10;
    private static final int FS_H1  = 14;
    private static final int FS_H2  = 12;
    private static final int FS_TITLE = 18;
    private static final int LSP = 320; // 16pt 固定行距 twips

    // ===== 章节标题常量：目录与正文统一引用，避免标题不一致、小节缺失或跳号 =====
    private static final String CH1 = "第一章 软件概述";
    private static final String CH2 = "第二章 安装与配置";
    private static final String CH3 = "第三章 功能介绍";
    private static final String CH4 = "第四章 操作指南";
    private static final String CH5 = "第五章 维护与常见问题";
    private static final String S1_1 = "1.1 软件简介";
    private static final String S1_2 = "1.2 应用领域";
    private static final String S1_3 = "1.3 技术特点";
    private static final String S1_4 = "1.4 系统架构";
    private static final String S2_1 = "2.1 运行环境";
    private static final String S2_2 = "2.2 安装步骤";
    private static final String S2_3 = "2.3 初始配置";
    private static final String S3_1 = "3.1 登录系统";
    private static final String S3_2 = "3.2 工作台首页";
    private static final String S4_1 = "4.1 登录系统";
    private static final String S4_2 = "4.2 工作台首页";
    private static final String S5_1 = "5.1 系统维护";
    private static final String S5_2 = "5.2 常见问题";
    private static final String S5_3 = "5.3 技术支持";

    private final ProjectService projectService;
    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;
    private final Long projectId;

    // 缓存 collectAllLines 结果，避免 generateWord / generatePdf 两次调用时重复生成截图（截图耗时~25s）
    private List<LineItem> cachedAllLines;

    /** 截图缓存：同一项目、同一功能菜单重复导出时复用，最多保留 4 份 */
    private static final Map<String, Map<String, byte[]>> SHOT_CACHE =
            java.util.Collections.synchronizedMap(new LinkedHashMap<>() {
                @Override protected boolean removeEldestEntry(Map.Entry<String, Map<String, byte[]>> e) {
                    return size() > 4;
                }
            });

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

            // 收集所有逻辑行（扉页标题、版本、目录、章节标题、正文段等每个都算 1 行）
            List<LineItem> all = collectAllLines(project, summary);
            int N = all.size();
            // 补齐到整页（30 逻辑行的整数倍），避免末页不足
            padToPage(all);
            int pages = totalWeight(all) / LPP;

            writeBlock(doc, all, false);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            log.info("说明文档：原总行数={} 输出={}页x{}行", N, pages, LPP);
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
     * 分页逻辑与 DOCX 一致：按 lineWeight 累加，到 30 时 newPage。
     */
    @Override public byte[] generatePdf() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();
        List<LineItem> all = new ArrayList<>(collectAllLines(project, summary));
        // 补齐到整页
        padToPage(all);
        int pages = totalWeight(all) / LPP;

        // 与 DOCX 一致：左右 20mm(56.7pt)、上下 15mm(42.5pt)
        com.itextpdf.text.Document doc = new com.itextpdf.text.Document(PageSize.A4, 56.7f, 56.7f, 42.5f, 42.5f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            // 字体按候选顺序回退，避免部署环境缺少某一字体时直接失败
            BaseFont song = loadCjkFont("simsun.ttc,0", "msyh.ttc,0", "simhei.ttf");
            BaseFont hei  = loadCjkFont("simhei.ttf", "msyhbd.ttc,0", "simsun.ttc,0");
            final Font pageFont = new Font(song, 9);

            // PDF 页脚页码，与 DOCX 的 PAGE 域保持一致
            writer.setPageEvent(new com.itextpdf.text.pdf.PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w, com.itextpdf.text.Document d) {
                    try {
                        com.itextpdf.text.pdf.ColumnText.showTextAligned(
                                w.getDirectContent(), com.itextpdf.text.Element.ALIGN_CENTER,
                                new com.itextpdf.text.Phrase("第 " + w.getPageNumber() + " 页", pageFont),
                                (d.left() + d.right()) / 2, d.bottom() - 20, 0);
                    } catch (Exception e) {
                        log.warn("PDF 页脚页码写入失败", e);
                    }
                }
            });
            doc.open();

            // 图片渲染尺寸由 LineItem 按原始宽高比计算，DOCX/PDF 共用

            int onPage = 0;
            int idx = 0;
            for (LineItem item : all) {
                idx++;
                onPage += item.lineWeight;
                if (item.kind == Kind.IMAGE && item.imageData != null) {
                    try {
                        com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(item.imageData);
                        // 按原始宽高比等比缩放（宽度 420pt，最高 320pt），避免截图被拉伸变形
                        img.scaleAbsolute(item.imageWidthPt, item.imageHeightPt);
                        img.setAlignment(com.itextpdf.text.Image.ALIGN_CENTER);
                        doc.add(img);
                    } catch (Exception e) {
                        log.warn("PDF 嵌入图片失败", e);
                    }
                } else {
                    float fs;
                    BaseFont bf;
                    int style = Font.NORMAL;
                    boolean center = false;
                    switch (item.kind) {
                        case TITLE:          fs = FS_TITLE; bf = hei;  center = true; break;
                        case VERSION:        fs = FS_H2;    bf = song; center = true; break;
                        case H1:             fs = FS_H1;    bf = hei;  break;
                        case H2:             fs = FS_H2;    bf = hei;  break;
                        case FIGURE_CAPTION: fs = 9;         bf = song; center = true; break;
                        default:             fs = FS_BODY;  bf = song; break;
                    }
                    Font font = new Font(bf, fs, style, BaseColor.BLACK);
                    Paragraph p = new Paragraph(item.text == null ? "" : item.text, font);
                    p.setLeading(item.lineWeight * 16f);
                    p.setSpacingAfter(0);
                    p.setSpacingBefore(0);
                    if (center) p.setAlignment(Paragraph.ALIGN_CENTER);
                    if (item.kind == Kind.BODY) p.setFirstLineIndent(21f);
                    doc.add(p);
                }
                // 累加 lineWeight 超过或达到本页容量时 newPage
                if (onPage >= LPP && idx < all.size()) {
                    onPage = onPage - LPP;
                    doc.newPage();
                }
            }
            doc.close();
            log.info("说明文档 PDF 生成：{} 行，{} 页，每页 {} 行", all.size(), pages, LPP);
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

    enum Kind { TITLE, VERSION, TOC, H1, H2, BODY, BLANK, IMAGE, FIGURE_CAPTION }

    static class LineItem {
        Kind kind; String text; boolean bold; byte[] imageData; String imageFormat;
        /** 图片渲染尺寸（按原始宽高比等比缩放），pt */
        float imageWidthPt = IMG_WIDTH_PT;
        float imageHeightPt = IMAGE_LINE_WEIGHT * LINE_PT;
        /** 该项占多少"逻辑行"，影响每页 30 行的分页计数；图片按实际渲染高度折算 */
        int lineWeight = 1;
        LineItem(Kind k, String t){ this(k,t,false); }
        LineItem(Kind k, String t, boolean b){ kind=k; text=t==null?"":t; bold=b;
            // TITLE 18pt 超 16pt 单倍行距，占 2 逻辑行避免裁切，且与分页计数一致
            if (k == Kind.TITLE) this.lineWeight = 2;
        }
        LineItem(byte[] img, String fmt){
            kind = Kind.IMAGE; text=""; imageData=img; imageFormat=fmt;
            float w = 0, h = 0;
            try (ByteArrayInputStream in = new ByteArrayInputStream(img)) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) { w = bi.getWidth(); h = bi.getHeight(); }
            } catch (Exception ignore) {
                // 尺寸读取失败时退回默认高度，保证不阻断文档生成
            }
            if (w > 0 && h > 0) {
                imageHeightPt = Math.min(IMG_WIDTH_PT * h / w, IMG_MAX_HEIGHT_PT);
                imageWidthPt = imageHeightPt * w / h;
            }
            lineWeight = Math.max(3, (int) Math.ceil(imageHeightPt / LINE_PT));
        }
    }

    private List<LineItem> collectAllLines(ProjectVO project, SoftwareSummaryVO summary) {
        if (cachedAllLines != null) return cachedAllLines;
        List<LineItem> r = new ArrayList<>();
        String name = project.getName();
        String ver = summary != null && summary.getVersion() != null ? summary.getVersion() : "V1.0";
        String functionMenu = summary != null ? summary.getFunctionMenu() : null;

        // 优先读取用户在编辑器保存的 MANUAL 手册；缺失时回退到本地章节拼接
        List<String> savedManual = loadSavedManualLines();
        // 保存的 MANUAL 为纯文本手册，无需截图；否则生成系统截图（登录页、首页、各模块列表/表单页）
        Map<String, byte[]> screenshots = savedManual != null
                ? java.util.Collections.<String, byte[]>emptyMap()
                : captureScreenshots(name, functionMenu);
        // 解析功能菜单：模块 -> 功能列表
        Map<String, List<String>> modules = parseFunctionMenu(functionMenu);

        // ========== 正文：先构建正文，再由正文标题生成目录，确保目录与正文完全一致 ==========
        List<LineItem> body = new ArrayList<>();
        if (savedManual != null) {
            // 用户保存的手册：按原样分类（第X章→H1、X.Y→H2），目录由这些标题派生
            appendChapterLines(body, savedManual, null, null);
        } else {
        // 第一章 软件介绍（来自 ch1，内容丰富）
        List<String> ch1Lines = new ArrayList<>();
        ch1(ch1Lines, summary);
        // 优先用 AI 生成的 systemOverview 替换 1.1 内容
        String aiOverview = (summary != null && summary.getSystemOverview() != null
                && !summary.getSystemOverview().trim().isEmpty())
                ? summary.getSystemOverview() : null;
        appendChapterLines(body, ch1Lines, aiOverview, "1.1");

        // 第二章 运行环境与安装（来自 ch2）
        List<String> ch2Lines = new ArrayList<>();
        ch2(ch2Lines, summary);
        appendChapterLines(body, ch2Lines, null, null);

        // 第二章截图：Windows 系统配置截图（图号按实际嵌入顺序连续递增）
        int fig2 = 0;
        byte[] ipConfigShot = screenshots.get("win_ipconfig");
        if (ipConfigShot != null) {
            body.add(new LineItem(Kind.BODY, "配置服务器 IP 地址：打开网络适配器属性，选择 Internet 协议版本 4 (TCP/IPv4)，" +
                    "设置静态 IP 地址、子网掩码、默认网关和 DNS 服务器。"));
            body.add(new LineItem(ipConfigShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图2-" + (++fig2) + " 服务器 IP 地址配置", false));
        }
        byte[] explorerShot = screenshots.get("win_explorer");
        if (explorerShot != null) {
            body.add(new LineItem(Kind.BODY, "部署文件目录结构：将系统部署包解压至服务器指定目录，" +
                    "目录中包含后端服务、前端静态资源、Docker 配置等文件。"));
            body.add(new LineItem(explorerShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图2-" + (++fig2) + " 部署文件目录结构", false));
        }
        byte[] serviceShot = screenshots.get("win_service");
        if (serviceShot != null) {
            body.add(new LineItem(Kind.BODY, "服务启动状态：打开 Windows 服务管理器，确认 MySQL、Redis、MinIO 和" +
                    "应用服务均已正常启动。"));
            body.add(new LineItem(serviceShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图2-" + (++fig2) + " Windows 服务管理器", false));
        }

        // ========== 第三章 功能介绍（截图驱动 + AI 描述） ==========
        body.add(new LineItem(Kind.H1, CH3, true));

        // 解析 AI 生成的主要功能，提取各模块描述
        Map<String, String> moduleDescs = parseModuleDescriptions(
                summary != null ? summary.getMainFunctions() : null, modules);
        log.info("result ai module desc : {}", moduleDescs);
        // 图号计数器：按实际嵌入顺序递增，避免缺图导致跳号
        int fig3 = 0;

        // 3.1 登录 + 登录截图
        body.add(new LineItem(Kind.H2, S3_1, true));
        body.add(new LineItem(Kind.BODY, "打开浏览器，在地址栏输入系统访问地址，进入系统登录页面。" +
                "在登录页面输入用户名和密码，点击 登录 按钮即可进入系统。首次登录系统会强制要求" +
                "修改初始密码，请按提示设置新密码，密码修改成功后使用新密码重新登录即可。"));
        byte[] loginShot = screenshots.get("login");
        if (loginShot != null) {
            body.add(new LineItem(loginShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图3-" + (++fig3) + " 系统登录页面", false));
        }

        // 3.2 首页/工作台截图
        body.add(new LineItem(Kind.H2, S3_2, true));
        body.add(new LineItem(Kind.BODY, "登录成功后进入系统工作台首页。首页以可视化方式展示关键业务指标，" +
                "包括今日记录数、本月记录数、异常告警数、在线用户数等统计卡片；同时以卡片形式展示各" +
                "功能模块入口，点击可快速进入对应模块；页面下方展示最近操作记录，便于了解系统运行状态。"));
        byte[] homeShot = screenshots.get("home");
        if (homeShot != null) {
            body.add(new LineItem(homeShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图3-" + (++fig3) + " 系统工作台首页", false));
        }

        // 架构图
        byte[] archShot = screenshots.get("arch");
        if (archShot != null) {
            body.add(new LineItem(Kind.BODY, "系统采用分层架构设计，整体架构如下图所示："));
            body.add(new LineItem(archShot, "png"));
            body.add(new LineItem(Kind.FIGURE_CAPTION, "图3-" + (++fig3) + " 系统架构图", false));
        }

        // 各业务模块：标题 + AI描述 + 功能列表 + 6张截图
        int modIdx = 1;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            String moduleName = e.getKey();
            List<String> funcs = e.getValue();
            String sectionNo = String.format("3.%d", modIdx + 2);

            body.add(new LineItem(Kind.H2, sectionNo + " " + moduleName, true));

            // 模块描述：仅用 AI 生成的内容，未生成则标注原因
            String aiDesc = moduleDescs.get(moduleName);
            if (aiDesc != null && !aiDesc.trim().isEmpty()) {
                for (String line : aiDesc.split("\\r?\\n")) {
                    String t = line.trim();
                    if (!t.isEmpty()) body.add(new LineItem(Kind.BODY, t));
                }
            } else {
                body.add(new LineItem(Kind.BODY, "【" + moduleName + "模块描述 - AI未生成。原因：AI输出中未匹配到此模块的描述，可能是模块名不匹配或AI输出格式异常。】"));
            }

            // 功能点列表：直接列功能菜单中的功能名，不编造描述
            if (funcs != null && !funcs.isEmpty()) {
                body.add(new LineItem(Kind.BODY, moduleName + "模块包含以下功能点："));
                int n = 1;
                for (String f : funcs) {
                    body.add(new LineItem(Kind.BODY, "（" + cnNum(n) + "）" + f));
                    n++;
                }
            }

            // 截图1：列表页（有数据）
            byte[] listShot = screenshots.get("list_" + modIdx);
            if (listShot != null) {
                body.add(new LineItem(Kind.BODY, "进入" + moduleName + "模块后，系统展示数据列表页面，" +
                        "已录入的数据以表格形式展示，支持按关键字段进行查询筛选。"));
                body.add(new LineItem(listShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s列表页面", ++fig3, moduleName), false));
            }

            // 截图2：列表页（空数据）
            byte[] listEmptyShot = screenshots.get("list_empty_" + modIdx);
            if (listEmptyShot != null) {
                body.add(new LineItem(Kind.BODY, "当模块中暂无数据时，列表页面显示暂无数据提示。"));
                body.add(new LineItem(listEmptyShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s列表页面（空数据状态）", ++fig3, moduleName), false));
            }

            // 截图3：列表页（搜索筛选）
            byte[] listSearchShot = screenshots.get("list_search_" + modIdx);
            if (listSearchShot != null) {
                body.add(new LineItem(Kind.BODY, "在搜索框中输入关键字并点击 搜索 按钮，系统筛选出符合条件的数据。"));
                body.add(new LineItem(listSearchShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s列表页面（搜索筛选）", ++fig3, moduleName), false));
            }

            // 截图4：新增表单（空）
            byte[] formShot = screenshots.get("form_" + modIdx);
            if (formShot != null) {
                body.add(new LineItem(Kind.BODY, "点击列表页面的 新增 按钮（红色标注处），打开信息录入表单。" +
                        "在表单中填写各项业务信息，带 * 号的为必填项。"));
                body.add(new LineItem(formShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s新增录入页面", ++fig3, moduleName), false));
            }

            // 截图5：表单（已填写）
            byte[] formFilledShot = screenshots.get("form_filled_" + modIdx);
            if (formFilledShot != null) {
                body.add(new LineItem(Kind.BODY, "填写完成各项信息后，点击 保存 按钮（红色标注处）提交数据，" +
                        "系统校验通过后保存成功并返回列表页面。"));
                body.add(new LineItem(formFilledShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s录入页面（已填写）", ++fig3, moduleName), false));
            }

            // 截图6：详情页
            byte[] detailShot = screenshots.get("detail_" + modIdx);
            if (detailShot != null) {
                body.add(new LineItem(Kind.BODY, "在列表页面点击某条记录可进入详情页面，查看该记录的完整信息。"));
                body.add(new LineItem(detailShot, "png"));
                body.add(new LineItem(Kind.FIGURE_CAPTION,
                        String.format("图3-%d %s详情页面", ++fig3, moduleName), false));
            }

            modIdx++;
        }

        // ========== 第四章 操作指南（来自 ch4，含登录/首页/模块操作截图） ==========
        ch4(body, summary, modules, screenshots);

        // ========== 第五章 维护与常见问题（来自 ch5） ==========
        List<String> ch5Lines = new ArrayList<>();
        ch5(ch5Lines, summary);
        appendChapterLines(body, ch5Lines, null, null);
        }

        // 图片不跨页：本页剩余空间不足时用空行补齐到页末，保证 DOCX/PDF 分页一致
        avoidImageSplit(body);

        // ========== 扉页 ==========
        for (int i=0;i<3;i++) r.add(new LineItem(Kind.BLANK,""));
        r.add(new LineItem(Kind.TITLE, name + " 说明书", true));
        r.add(new LineItem(Kind.VERSION, "版本：" + ver));
        for (int i=0;i<4;i++) r.add(new LineItem(Kind.BLANK,""));
        padToPage(r);

        // ========== 目录：由正文的章/节标题自动生成，页码按最终分页计算 ==========
        r.add(new LineItem(Kind.H1, "目  录", true));
        List<LineItem> tocEntries = new ArrayList<>();
        List<LineItem> headings = new ArrayList<>();
        for (LineItem it : body) {
            boolean isChapter = it.kind == Kind.H1;
            boolean isSection = it.kind == Kind.H2 && it.text.matches("^\\d+\\.\\d+\\s.*");
            if (isChapter || isSection) {
                LineItem toc = new LineItem(Kind.TOC, "");
                tocEntries.add(toc);
                headings.add(it);
                r.add(toc);
            }
        }
        padToPage(r);
        // 正文起始页 = 扉页与目录占用的页数 + 1
        int bodyStartPage = totalWeight(r) / LPP + 1;
        int acc = 0;
        int hi = 0;
        for (LineItem it : body) {
            if (hi < headings.size() && it == headings.get(hi)) {
                String label = it.kind == Kind.H1 ? it.text : "　　" + it.text;
                tocEntries.get(hi).text = tocLine(label, bodyStartPage + acc / LPP);
                hi++;
            }
            acc += it.lineWeight;
        }

        r.addAll(body);
        cachedAllLines = r;
        return r;
    }

    /** 用空行把列表补齐到 30 逻辑行的整数倍（按 lineWeight 计算，图片与标题按权重折算） */
    private static void padToPage(List<LineItem> list) {
        int weight = totalWeight(list);
        while (weight % LPP != 0) {
            list.add(new LineItem(Kind.BLANK, ""));
            weight++;
        }
    }

    /** 列表占用的逻辑行总数 */
    private static int totalWeight(List<LineItem> list) {
        int weight = 0;
        for (LineItem it : list) weight += it.lineWeight;
        return weight;
    }

    /** 图片不跨页：本页剩余逻辑行不足以容纳图片时，用空行补齐到页末 */
    private static void avoidImageSplit(List<LineItem> list) {
        int weight = 0;
        for (int i = 0; i < list.size(); i++) {
            LineItem it = list.get(i);
            if (it.kind == Kind.IMAGE) {
                int remain = LPP - (weight % LPP);
                if (remain < it.lineWeight) {
                    for (int k = 0; k < remain; k++) {
                        list.add(i, new LineItem(Kind.BLANK, ""));
                        i++;
                    }
                    weight += remain;
                }
            }
            weight += it.lineWeight;
        }
    }

    /** 目录条目：标题 + 点线引导 + 页码 */
    private static String tocLine(String label, int page) {
        int width = 0;
        for (char c : label.toCharArray()) width += (c > 0x2E80) ? 2 : 1;
        int dots = Math.max(3, 62 - width - String.valueOf(page).length());
        return label + " " + ".".repeat(dots) + " " + page;
    }

    /** 按候选顺序加载中文字体，全部不可用时抛出明确异常，避免生成损坏的 PDF */
    private static BaseFont loadCjkFont(String... candidates) {
        String winDir = System.getenv().getOrDefault("WINDIR", "C:\\Windows");
        for (String c : candidates) {
            File f = new File(winDir + "\\Fonts\\" + c);
            if (f.exists()) {
                try {
                    return BaseFont.createFont(f.getAbsolutePath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception e) {
                    log.warn("加载字体失败: {}", f.getAbsolutePath(), e);
                }
            }
        }
        try {
            // 退到 iText 内置 CJK 字体（无需系统字体文件，需 itext-asian）
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("未找到可用中文字体，请安装宋体/微软雅黑后重试", e);
        }
    }

    /**
     * 将章节文本行转换为 LineItem 并追加到结果中。
     * 行分类规则：
     *   - 以"第X章"开头 → H1
     *   - 匹配 "X.Y" 或 "X.Y.Z" 开头 → H2
     *   - 其余 → BODY
     * 若 aiReplaceSection 不为空，则该小节的内容用 aiContent 替换。
     */
    private void appendChapterLines(List<LineItem> r, List<String> lines,
                                    String aiContent, String aiReplaceSection) {
        boolean inAiSection = false;
        boolean aiWritten = false;
        for (String raw : lines) {
            if (raw == null) continue;
            String line = raw.trim();
            if (line.isEmpty()) continue;

            // 判断是否是章节标题
            boolean isChapter = line.matches("^第[一二三四五六七八九十]+章.*");
            boolean isSection = line.matches("^\\d+\\.\\d+(\\.\\d+)?\\s.*");
            String sectionNo = isSection ? line.split("\\s")[0] : null;

            // 处理 AI 内容替换：遇到目标小节标题时，用 AI 内容替换后续正文直到下一个标题
            if (aiReplaceSection != null && aiContent != null && sectionNo != null
                    && sectionNo.startsWith(aiReplaceSection)) {
                inAiSection = true;
                aiWritten = false;
            } else if (isSection || isChapter) {
                inAiSection = false;
            }

            if (inAiSection && !aiWritten) {
                // 先写标题
                if (isChapter) r.add(new LineItem(Kind.H1, line, true));
                else if (isSection) r.add(new LineItem(Kind.H2, line, true));
                // 再写 AI 内容
                for (String al : aiContent.split("\\r?\\n")) {
                    String t = al.trim();
                    if (!t.isEmpty()) r.add(new LineItem(Kind.BODY, t));
                }
                aiWritten = true;
                continue;
            }
            if (inAiSection && aiWritten) continue; // 跳过被替换的原文

            if (isChapter) {
                r.add(new LineItem(Kind.H1, line, true));
            } else if (isSection) {
                r.add(new LineItem(Kind.H2, line, true));
            } else {
                r.add(new LineItem(Kind.BODY, line));
            }
        }
    }

    /**
     * 从 AI 生成的"主要功能"文本中提取各模块描述。
     * mainFunctions 格式通常为 "1. 模块名：描述...2. 模块名：描述..."
     * 按功能菜单中的模块名做模糊匹配，返回 模块名 -> 描述 的映射。
     */
    private static Map<String, String> parseModuleDescriptions(String mainFunctions, Map<String, List<String>> modules) {
        Map<String, String> result = new java.util.HashMap<>();
        if (mainFunctions == null || mainFunctions.trim().isEmpty() || modules.isEmpty()) {
            log.warn("parseModuleDescriptions: mainFunctions empty or modules empty, skip");
            return result;
        }

        // 打印 AI 输出前 800 字，便于排查格式问题
        String preview = mainFunctions.length() > 800 ? mainFunctions.substring(0, 800) : mainFunctions;
        log.info("parseModuleDescriptions: AI 输出预览=\n{}", preview);

        // 策略一：优先用功能菜单中的模块名直接定位，截取模块之间的文本
        // （AI 常输出 "数据核验审批模块负责...；在数字化标准配置环节..." 这类连贯段落或"在{模块名}环节"句式）
        result = parseByModuleName(mainFunctions, modules);

        // 策略二：模块名定位不足时，用 "1. 模块名：" 等序号格式补充
        if (result.size() < modules.size()) {
            Map<String, String> byName = parseByNumberedFormat(mainFunctions, modules);
            for (Map.Entry<String, String> e : byName.entrySet()) {
                result.putIfAbsent(e.getKey(), e.getValue());
            }
        }

        log.info("parseModuleDescriptions: matched {}/{} modules", result.size(), modules.size());
        return result;
    }

    /** 策略一：按 "1. 模块名：" 等序号格式切分 */
    private static Map<String, String> parseByNumberedFormat(String text, Map<String, List<String>> modules) {
        Map<String, String> result = new java.util.HashMap<>();
        java.util.regex.Pattern[] patterns = new java.util.regex.Pattern[]{
                java.util.regex.Pattern.compile("(?:^|\\n)\\s*#{0,6}\\s*(\\d+)[、.．]\\s*\\*?\\*?([^：:\\n*#]{2,30})\\*?\\*?\\s*[：:]", java.util.regex.Pattern.MULTILINE),
                java.util.regex.Pattern.compile("(?:^|\\n)\\s*#{0,6}\\s*\\*?\\*?(\\d+)[、.．]\\s*([^：:\\n*#]{2,30})\\*?\\*?\\s*[：:]", java.util.regex.Pattern.MULTILINE),
                java.util.regex.Pattern.compile("(?:^|\\n)\\s*#{1,6}\\s+\\*?\\*?([^：:\\n*#]{2,30})\\*?\\*?", java.util.regex.Pattern.MULTILINE),
                java.util.regex.Pattern.compile("(?:^|\\n)\\s*\\*?\\*?([^：:\\n*#]{2,30})\\*?\\*?\\s*[：:]", java.util.regex.Pattern.MULTILINE)
        };

        java.util.List<int[]> spans = new java.util.ArrayList<>();
        java.util.List<String> names = new java.util.ArrayList<>();

        for (java.util.regex.Pattern p : patterns) {
            java.util.regex.Matcher m = p.matcher(text);
            java.util.List<int[]> trySpans = new java.util.ArrayList<>();
            java.util.List<String> tryNames = new java.util.ArrayList<>();
            while (m.find()) {
                String name = m.group(m.groupCount()).trim();
                if (name.length() < 2 || name.length() > 20) continue;
                if (name.startsWith("各") || name.startsWith("综") || name.startsWith("总") || name.startsWith("以")
                        || name.contains("衔接") || name.contains("关系") || name.contains("说明")
                        || name.contains("如下") || name.contains("功能") || name.contains("模块")
                        || name.contains("主要") || name.contains("系统")) continue;
                trySpans.add(new int[]{m.start(), m.end()});
                tryNames.add(name);
            }
            if (tryNames.size() >= 2) {
                spans = trySpans;
                names = tryNames;
                break;
            }
            if (names.isEmpty() && !tryNames.isEmpty()) {
                spans = trySpans;
                names = tryNames;
            }
        }

        log.info("parseModuleDescriptions[策略一]: parsed {} sections", names.size());

        for (int i = 0; i < names.size(); i++) {
            int start = spans.get(i)[1];
            int end = (i + 1 < spans.size()) ? spans.get(i + 1)[0] : text.length();
            String desc = text.substring(start, end).trim();
            desc = desc.replaceAll("^\\s*\\d+[、.．]\\s*", "");
            desc = stripSummary(desc);
            if (!desc.isEmpty()) {
                String matched = matchModuleName(names.get(i), modules);
                if (matched != null) {
                    result.putIfAbsent(matched, desc);
                    log.info("parseModuleDescriptions[策略一]: AI[{}] -> 模块[{}]", names.get(i), matched);
                }
            }
        }
        return result;
    }

    /**
     * 策略一：用已知模块名直接在文本中定位，截取模块到下一个模块之间的文本作为描述。
     * 支持的句式（与 generateMainFunctions 提示词句式对齐）：
     *   - "模块名模块负责/支持/用于..."
     *   - "在模块名环节输出..."
     *   - "在模块名中落实..."
     *   - "模块名支持..."（模块名后直接跟动词）
     */
    private static Map<String, String> parseByModuleName(String text, Map<String, List<String>> modules) {
        Map<String, String> result = new java.util.HashMap<>();
        java.util.List<String> moduleNames = new java.util.ArrayList<>(modules.keySet());
        // 长模块名优先匹配，避免短模块名命中长模块名的片段
        moduleNames.sort((a, b) -> Integer.compare(b.length(), a.length()));

        // 每个模块只保留第一次被“定义”的位置：模块名 -> {起始位置, 名称长度, 描述偏移}
        java.util.Map<String, int[]> firstPositions = new java.util.LinkedHashMap<>();
        java.util.List<int[]> occupied = new java.util.ArrayList<>();

        for (String modName : moduleNames) {
            int idx = 0;
            while ((idx = text.indexOf(modName, idx)) >= 0) {
                int end = idx + modName.length();
                // 已被更长的模块名占用则跳过，避免重复切分
                if (overlaps(occupied, idx, end)) {
                    idx = end;
                    continue;
                }
                String marker = text.substring(end, Math.min(end + 2, text.length()));
                int descOffset = -1;
                if (marker.startsWith("模块") || marker.startsWith("环节")
                        || marker.startsWith("功能") || marker.startsWith("子系统")) {
                    descOffset = 2;
                } else if (marker.startsWith("中") || marker.startsWith("里")) {
                    descOffset = 1;
                } else if (marker.startsWith("负责") || marker.startsWith("支持") || marker.startsWith("用于")
                        || marker.startsWith("提供") || marker.startsWith("实现") || marker.startsWith("包含")
                        || marker.startsWith("承担") || marker.startsWith("开展")) {
                    descOffset = 0;
                }
                if (descOffset >= 0) {
                    firstPositions.putIfAbsent(modName, new int[]{idx, modName.length(), descOffset});
                    occupied.add(new int[]{idx, end});
                    break;
                }
                idx = end;
            }
        }

        // 按文本位置排序后，逐段截取"模块名 -> 下一个模块名"之间的文本
        java.util.List<java.util.Map.Entry<String, int[]>> positions =
                new java.util.ArrayList<>(firstPositions.entrySet());
        positions.sort((a, b) -> Integer.compare(a.getValue()[0], b.getValue()[0]));

        log.info("parseModuleDescriptions[策略一]: 找到 {} 个模块位置", positions.size());

        for (int i = 0; i < positions.size(); i++) {
            String modName = positions.get(i).getKey();
            int start = positions.get(i).getValue()[0];
            int descStart = start + positions.get(i).getValue()[1] + positions.get(i).getValue()[2];
            int descEnd = (i + 1 < positions.size()) ? positions.get(i + 1).getValue()[0] : text.length();
            String desc = text.substring(descStart, descEnd).trim();

            // 去掉开头的标点与动词（"在X环节，登记..." 截取后残留的逗号在此清理）
            desc = desc.replaceAll("^[，,、；;：:。.\\s]+", "");
            desc = desc.replaceAll("^(负责|支持|用于|提供|实现|包含|主要|核心|承担|开展|落实|完成|组织|进行|的)\\s*", "");
            // 去掉尾部的总结段落
            desc = stripSummary(desc);
            // 去掉结尾的分号/句号，以及下一模块前残留的"在"
            desc = desc.replaceAll("[；;。.，,\\s]+$", "").trim();
            desc = desc.replaceAll("在$", "").trim();
            desc = desc.replaceAll("[；;。.，,\\s]+$", "").trim();

            if (!desc.isEmpty()) {
                String matched = matchModuleName(modName, modules);
                if (matched != null) {
                    result.putIfAbsent(matched, desc);
                    log.info("parseModuleDescriptions[策略一]: 模块[{}] 描述长度={}", matched, desc.length());
                }
            }
        }
        return result;
    }

    /** 区间是否与已占用区间重叠 */
    private static boolean overlaps(java.util.List<int[]> ranges, int start, int end) {
        for (int[] r : ranges) {
            if (start < r[1] && end > r[0]) return true;
        }
        return false;
    }

    /** 去掉描述尾部的总结段落 */
    private static String stripSummary(String desc) {
        int summaryIdx = -1;
        String[] summaryPrefixes = {"各模块", "综上", "总之", "整体来看", "系统通过", "以上模块", "各功能", "系统在"};
        for (String prefix : summaryPrefixes) {
            int idx = desc.indexOf(prefix);
            if (idx > 20) {
                if (summaryIdx < 0 || idx < summaryIdx) summaryIdx = idx;
            }
        }
        if (summaryIdx > 0) desc = desc.substring(0, summaryIdx).trim();
        return desc;
    }

    /**
     * 将 AI 输出的模块名匹配到功能菜单中的模块名。
     * 匹配优先级：精确匹配 > 包含关系 > 公共字符比例(≥40%)
     */
    private static String matchModuleName(String aiName, Map<String, List<String>> modules) {
        if (aiName == null || aiName.isEmpty()) return null;
        String bestMatch = null;
        int bestScore = 0;
        for (String modName : modules.keySet()) {
            int score;
            if (aiName.equals(modName)) {
                return modName; // 精确匹配直接返回
            } else if (aiName.contains(modName) || modName.contains(aiName)) {
                score = 80 + Math.min(aiName.length(), modName.length());
            } else {
                int common = commonCharCount(aiName, modName);
                int minLen = Math.min(aiName.length(), modName.length());
                score = (minLen > 0) ? common * 100 / minLen : 0;
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = modName;
            }
        }
        // 降低阈值到 40%，提高匹配率
        return (bestMatch != null && bestScore >= 40) ? bestMatch : null;
    }

    /** 两个字符串的公共字符数（用于模糊匹配模块名） */
    private static int commonCharCount(String a, String b) {
        if (a == null || b == null) return 0;
        int count = 0;
        for (char c : a.toCharArray()) {
            if (b.indexOf(c) >= 0) count++;
        }
        return count;
    }

    /**
     * 解析功能菜单。支持的每行格式：
     *   1) "子菜单（一级菜单）"——标准格式，全角括号；一级菜单即分组键
     *   2) "子菜单(一级菜单)"——半角括号兼容
     *   3) "功能名：模块名" / "模块名/功能名"——旧格式兼容
     */
    private Map<String, List<String>> parseFunctionMenu(String functionMenu) {
        Map<String, List<String>> modules = new LinkedHashMap<>();
        if (functionMenu == null || functionMenu.trim().isEmpty()) return modules;
        for (String raw : functionMenu.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String func = null, module = null;
            // 1) 优先识别括号格式 子菜单（一级菜单）
            int fs = line.indexOf('（'), fe = line.indexOf('）');
            int hs = line.indexOf('('),  he = line.indexOf(')');
            if (fs > 0 && fe > fs) {
                func = line.substring(0, fs).trim();
                module = line.substring(fs + 1, fe).trim();
            } else if (hs > 0 && he > hs) {
                func = line.substring(0, hs).trim();
                module = line.substring(hs + 1, he).trim();
            }
            // 2) 回退到冒号/斜杠分隔
            if (func == null || func.isEmpty()) {
                String[] parts = line.split("[:：/\\\\]", 2);
                if (parts.length == 2) {
                    func = parts[0].trim();
                    module = parts[1].trim();
                } else {
                    func = line;
                    module = null;
                }
            }
            if (func == null || func.isEmpty()) continue;
            if (module == null || module.isEmpty()) module = "系统功能";
            modules.computeIfAbsent(module, k -> new ArrayList<>()).add(func);
        }
        return modules;
    }

    /** 调用浏览器生成系统截图，返回 标识 -> PNG字节 */
    private Map<String, byte[]> captureScreenshots(String softwareName, String functionMenu) {
        // 命中缓存直接复用，避免每次导出都重新截图
        String cacheKey = projectId + "|" + softwareName + "|"
                + (functionMenu == null ? "" : functionMenu.hashCode());
        Map<String, byte[]> cached = SHOT_CACHE.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            log.info("截图命中缓存：{} 张", cached.size());
            return new LinkedHashMap<>(cached);
        }

        Map<String, byte[]> result = new LinkedHashMap<>();
        // 用 UUID 确保每次调用目录唯一，避免并发任务互相删除文件
        File workDir = new File(System.getProperty("java.io.tmpdir"),
                "ruanzhu_shots_" + projectId + "_" + java.util.UUID.randomUUID());
        try {
            // 优先用浏览器截图
            SystemScreenshotService svc = new SystemScreenshotService(softwareName, functionMenu, workDir);
            Map<String, File> files = svc.captureAll();

            // AWT 绘制兜底：按标识合并补齐浏览器未生成的页面，保证每模块 6 张截图
            AwtScreenshotService awt = new AwtScreenshotService(softwareName, functionMenu, workDir);
            Map<String, File> awtFiles = awt.captureAll();
            for (Map.Entry<String, File> e : awtFiles.entrySet()) {
                if (e.getValue() != null && e.getValue().exists()) {
                    files.putIfAbsent(e.getKey(), e.getValue());
                }
            }
            log.info("截图合并完成：浏览器/兜底共 {} 张", files.size());

            // 先把所有图片读进内存，再清理目录（避免边读边删）
            for (Map.Entry<String, File> e : files.entrySet()) {
                result.put(e.getKey(), Files.readAllBytes(e.getValue().toPath()));
            }
            log.info("截图生成完成，共 {} 张", result.size());
        } catch (Exception e) {
            log.warn("生成系统截图失败，将跳过截图嵌入", e);
        } finally {
            // 无论成功失败都清理临时目录
            try {
                deleteRecursively(workDir);
            } catch (Exception ignored) {}
        }
        if (!result.isEmpty()) SHOT_CACHE.put(cacheKey, result);
        return result;
    }

    private static void deleteRecursively(File f) throws java.io.IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        java.nio.file.Files.deleteIfExists(f.toPath());
    }

    /** 内容不足时用空行补齐，绝不循环复制正文导致内容重复 */
    private static List<LineItem> ensureAtLeast(List<LineItem> src, int minLen) {
        if (src.size() >= minLen) return src;
        List<LineItem> out = new ArrayList<>(src);
        while (out.size() < minLen) out.add(new LineItem(Kind.BLANK, ""));
        return out;
    }

    private void ch1(List<String> out, SoftwareSummaryVO s) {
        out.add(CH1);
        out.add(S1_1);
        aiOnly(out, s != null ? s.getPurpose() : null, "软件简介");
        out.add(S1_2);
        aiOnly(out, s != null ? s.getTargetDomain() : null, "应用领域");
        out.add(S1_3);
        aiOnly(out, s != null ? s.getTechFeatures() : null, "技术特点");
        out.add(S1_4);
        aiOnly(out, s != null ? s.getSystemOverview() : null, "系统架构");
    }
    private void ch2(List<String> out, SoftwareSummaryVO s) {
        String lang = s != null ? s.getLanguage() : null;
        out.add(CH2);
        out.add("【注：本章为系统通用模板内容，非AI生成】");
        out.add(S2_1);
        out.add("2.1.1 硬件环境");
        out.add("服务器端最低配置要求：");
        out.add("（1）CPU：4核及以上，推荐 8核；");
        out.add("（2）内存：8GB 以上，推荐 16GB；");
        out.add("（3）硬盘：50GB 可用空间，推荐使用 SSD；");
        out.add("（4）网络：千兆以太网。");
        out.add("客户端配置要求：");
        out.add("（1）PC：CPU 双核 2GHz 以上，内存 4GB 以上；");
        out.add("（2）移动终端：Android 8.0+ 或 iOS 12.0+；");
        out.add("（3）触控终端：10寸以上触摸屏，Windows 10 或 Android 系统。");
        multi(out, s!=null?s.getRunHardware():null, null);
        out.add("2.1.2 软件环境");
        out.add("服务器端：");
        out.add("（1）操作系统：Windows Server 2019+ 或 CentOS 7+/Ubuntu 20.04+；");
        out.add("（2）运行时：" + LanguageEnvironment.runtime(lang) + "；");
        out.add("（3）数据库：MySQL 8.0+；");
        out.add("（4）对象存储：MinIO（可选，默认本地文件系统）；");
        out.add("（5）缓存：Redis 6.0+（可选，用于会话与热点数据缓存）。");
        out.add("客户端：");
        out.add("（1）浏览器：Chrome 90+ / Edge 90+ / Firefox 88+；");
        out.add("（2）分辨率：建议 1366×768 及以上。");
        multi(out, LanguageEnvironment.runPlatformOr(lang, s != null ? s.getRunPlatform() : null), null);
        out.add(S2_2);
        out.add("2.2.1 数据库准备");
        out.add("（1）安装 MySQL 8.0 并启动服务；");
        out.add("（2）创建数据库：CREATE DATABASE ruanzhu DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;");
        out.add("（3）创建数据库用户并授权：CREATE USER 'ruanzhu'@'%' IDENTIFIED BY 'your_password'; " +
                "GRANT ALL PRIVILEGES ON ruanzhu.* TO 'ruanzhu'@'%'; FLUSH PRIVILEGES;");
        out.add("（4）执行数据库初始化脚本：mysql -u ruanzhu -p ruanzhu < schema.sql。");
        out.add("2.2.2 后端部署");
        out.add("（1）从发布包获取" + LanguageEnvironment.artifact(lang) + "；");
        out.add("（2）将后端发布产物上传至服务器指定目录，如 /opt/app/；");
        out.add("（3）创建配置文件（如 application.yml / config.yaml），设置数据库连接、端口、文件存储路径等参数；");
        out.add("（4）启动后端服务：" + LanguageEnvironment.startCommand(lang) + "；");
        out.add("（5）验证服务启动：访问 http://服务器IP:8080/api/health，返回 success 表示启动成功。");
        out.add("2.2.3 前端部署");
        out.add("（1）从发布包获取前端构建产物 dist 目录；");
        out.add("（2）方式一：将 dist 目录部署至 Nginx，配置反向代理指向后端 API；");
        out.add("（3）方式二：将 dist 目录放置于后端 static 目录，由后端直接提供静态资源服务；");
        out.add("（4）配置 Nginx 示例：server { listen 80; root /usr/share/nginx/html; " +
                "location /api/ { proxy_pass http://localhost:8080/api/; } }。");
        out.add("2.2.4 服务启停");
        out.add("（1）启动后端：nohup " + LanguageEnvironment.startCommand(lang) + " > app.log 2>&1 &");
        out.add("（2）停止后端：" + LanguageEnvironment.stopCommand(lang));
        out.add("（3）查看日志：tail -f app.log");
        out.add(S2_3);
        out.add("2.3.1 系统参数配置");
        out.add("登录系统后进入 系统管理-参数配置，依次配置：");
        out.add("（1）组织信息：机构名称、软件版本、著作权人、开发完成日期；");
        out.add("（2）业务参数：根据实际业务需要配置相关阈值、默认值与规则；");
        out.add("（3）存储配置：文件存储路径、最大上传文件大小、允许的文件类型。");
        out.add("2.3.2 用户与角色配置");
        out.add("（1）进入 系统管理-用户管理，创建各岗位用户账号；");
        out.add("（2）进入 系统管理-角色管理，配置角色权限，包括系统管理员、业务主管、" +
                "一线操作员、数据分析员等；");
        out.add("（3）为用户分配相应角色，用户登录后即可使用对应功能。");
        out.add("2.3.3 基础数据配置");
        out.add("（1）业务分类维护：录入本企业涉及的业务分类及对应的属性字段；");
        out.add("（2）数据字典维护：定义系统中使用的各类枚举值、状态码与等级划分；");
        out.add("（3）合作方维护：录入相关合作方信息及对应的业务范围。");
    }
    private void ch3(List<String> out, SoftwareSummaryVO s, Map<String, List<String>> modules) {
        out.add(CH3);
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
        // 与正文第三章保持同一结构：3.1 登录系统、3.2 工作台首页，其后按模块顺序编号
        out.add(S3_1);
        out.add("打开浏览器，在地址栏输入系统访问地址，进入系统登录页面。" +
                "在登录页面输入用户名和密码，点击 登录 按钮即可进入系统。");
        out.add(S3_2);
        out.add("登录成功后进入系统工作台首页，首页展示关键业务指标统计卡片、各功能模块入口与最近操作记录。");
        int idx = 3;
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
    }

    private void ch4(List<LineItem> out, SoftwareSummaryVO s, Map<String, List<String>> modules,
                     Map<String, byte[]> screenshots) {
        int[] fig4 = {0};
        out.add(new LineItem(Kind.H1, CH4, true));
        out.add(new LineItem(Kind.BODY, "【注：本章为系统通用模板内容，非AI生成】"));

        // 4.1 登录系统
        out.add(new LineItem(Kind.H2, S4_1, true));
        out.add(new LineItem(Kind.BODY, "（1）打开浏览器，在地址栏输入系统访问地址；"));
        out.add(new LineItem(Kind.BODY, "（2）在登录页面输入用户名和密码；"));
        out.add(new LineItem(Kind.BODY, "（3）首次登录系统会强制要求修改初始密码，请按提示设置新密码；"));
        out.add(new LineItem(Kind.BODY, "（4）密码修改成功后，使用新密码重新登录即可进入系统首页（工作台）。"));
        addFigure(out, screenshots.get("login"), "系统登录页面", fig4);

        // 4.2 工作台首页
        out.add(new LineItem(Kind.H2, S4_2, true));
        out.add(new LineItem(Kind.BODY, "登录成功后进入系统工作台首页，首页展示内容包括："));
        out.add(new LineItem(Kind.BODY, "（1）统计卡片：展示今日记录数、本月记录数、异常告警数、在线用户数等关键指标；"));
        out.add(new LineItem(Kind.BODY, "（2）功能模块入口：以卡片形式展示各功能模块，点击可快速进入对应模块；"));
        out.add(new LineItem(Kind.BODY, "（3）最近操作记录：展示系统最近的操作日志，便于了解系统运行状态。"));
        addFigure(out, screenshots.get("home"), "系统工作台首页", fig4);

        // 4.3 起：各功能模块操作步骤（与功能菜单一一对应）
        int idx = 3;
        int modIdx = 1;
        for (Map.Entry<String, List<String>> e : modules.entrySet()) {
            String moduleName = e.getKey();
            out.add(new LineItem(Kind.H2, "4." + idx + " " + moduleName, true));
            out.add(new LineItem(Kind.BODY, "（1）在左侧菜单栏点击 " + moduleName + "，展开该模块下的功能菜单；"));
            out.add(new LineItem(Kind.BODY, "（2）点击需要操作的功能项，进入对应的列表页面，页面展示已录入的数据记录；"));
            out.add(new LineItem(Kind.BODY, "（3）在列表页面可通过顶部搜索框输入关键词进行查询，点击 查询 按钮筛选数据，点击 重置 按钮清空查询条件；"));
            addFigure(out, screenshots.get("list_" + modIdx), moduleName + "列表页面操作", fig4);
            out.add(new LineItem(Kind.BODY, "（4）点击 新增 按钮，打开信息录入表单；"));
            out.add(new LineItem(Kind.BODY, "（5）在表单中填写各项信息，带 * 号的为必填项；"));
            out.add(new LineItem(Kind.BODY, "（6）填写完成后点击 保存 按钮提交数据，系统校验通过后保存成功；"));
            addFigure(out, screenshots.get("form_filled_" + modIdx), moduleName + "信息录入页面", fig4);
            out.add(new LineItem(Kind.BODY, "（7）在列表页面可对已有记录进行 查看、编辑 操作，操作完成后系统自动更新数据。"));
            int n = 1;
            for (String f : e.getValue()) {
                out.add(new LineItem(Kind.BODY, "（" + cnNum(n + 7) + "）" + f + "：在左侧菜单点击 " + f + "，按上述流程进行操作。"));
                n++;
            }
            idx++;
            modIdx++;
        }
    }

    /** 追加一张截图与对应图注（图号按实际嵌入顺序递增，无截图时不生成图注） */
    private static void addFigure(List<LineItem> out, byte[] img, String caption, int[] figNo) {
        if (img == null || img.length == 0) return;
        out.add(new LineItem(img, "png"));
        out.add(new LineItem(Kind.FIGURE_CAPTION,
                String.format("图4-%d %s", ++figNo[0], caption), false));
    }

    private static String cnNum(int n) {
        String[] cn = {"一","二","三","四","五","六","七","八","九","十"};
        return n >= 1 && n <= 10 ? cn[n-1] : String.valueOf(n);
    }

    private void ch5(List<String> out, SoftwareSummaryVO s) {
        out.add(CH5);
        out.add("【注：本章为系统通用模板内容，非AI生成】");
        out.add(S5_1);
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
        out.add(S5_2);
        out.add("Q1：无法登录系统？");
        out.add("A：请检查以下内容：");
        out.add("（1）账号是否被锁定（连续 5 次输错密码会锁定账号，需管理员解锁）；");
        out.add("（2）密码是否过期（系统默认 90 天密码过期，需修改密码）；");
        out.add("（3）数据库连接是否正常（检查数据库服务状态与连接配置）；");
        out.add("（4）网络是否正常（检查客户端与服务器之间的网络连通性）。");
        out.add("Q2：数据录入后保存失败？");
        out.add("A：请检查以下内容：");
        out.add("（1）必填项是否已全部填写（带 * 号的字段为必填项）；");
        out.add("（2）数据格式是否符合要求（如日期、数字、手机号等格式校验）；");
        out.add("（3）是否存在重复数据（部分业务字段要求唯一性）；");
        out.add("（4）网络连接是否正常，可尝试刷新页面后重新提交。");
        out.add("Q3：查询不到已录入的数据？");
        out.add("A：请确认所选查询条件是否正确，时间范围是否覆盖数据录入时间，" +
                "当前用户权限是否覆盖对应数据范围。");
        out.add("Q4：报表导出为空？");
        out.add("A：请确认所选时间范围内是否存在数据，当前用户权限是否覆盖对应数据范围，" +
                "查询条件是否设置正确。");
        out.add("Q5：业务流程审批不通过？");
        out.add("A：请联系审批人确认审批意见，或检查提交的业务数据是否符合审批规则要求。" +
                "如对审批结果有异议，可联系上级管理员协调处理。");
        out.add("Q6：AI 生成内容失败？");
        out.add("A：请联系管理员检查 AI Key 是否有效、网络是否可访问 AI 服务、账户余额是否充足。");
        out.add(S5_3);
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

            String[] layers = {"展示层 (Web 前端 / 移动端)", "应用层 (RESTful API)",
                    "服务层 (后端业务服务)", "数据层 (MySQL / MinIO / Redis)"};
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
                    case 0: mods = new String[]{"工作台", "业务录入", "审批处理", "报表分析"}; break;
                    case 1: mods = new String[]{"认证授权", "业务管理", "流程引擎", "统计分析"}; break;
                    case 2: mods = new String[]{"用户服务", "业务服务", "流程服务", "任务调度"}; break;
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

    /**
     * 仅输出 AI 生成的内容，绝不兜底编造。
     * 如果 AI 值为空或为失败提示，则输出明确的未生成标记。
     */
    private static void aiOnly(List<String> out, String aiVal, String sectionName) {
        if (aiVal != null && !aiVal.trim().isEmpty()
                && !aiVal.contains("生成失败") && !aiVal.contains("未生成")) {
            for (String p : aiVal.split("\r?\n")) {
                String s = p.replace("\t", "    ").trim();
                if (!s.isEmpty()) out.add(s);
            }
        } else {
            String reason = (aiVal != null && aiVal.contains("生成失败"))
                    ? "AI生成失败：" + aiVal
                    : "AI未生成此部分内容";
            out.add("【" + sectionName + " - " + reason + "】");
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

    /** 读取用户在编辑器保存的最新 MANUAL 手册（HTML 转纯文本行）；无保存记录时返回 null */
    private List<String> loadSavedManualLines() {
        List<FileRecord> saved = fileRecordRepository.findByProjectIdAndFileType(projectId, "MANUAL");
        if (saved.isEmpty()) return null;
        FileRecord latest = saved.stream()
                .max(Comparator.comparingInt(v -> v.getVersion() != null ? v.getVersion() : 0))
                .get();
        byte[] data = storageClient.download(latest.getStoragePath());
        List<String> lines = htmlToPlainLines(new String(data, StandardCharsets.UTF_8));
        log.info("说明文档：用保存的 MANUAL name={}, size={}B, lines={}",
                latest.getFileName(), data.length, lines.size());
        return lines.isEmpty() ? null : lines;
    }

    private static List<String> htmlToPlainLines(String html) {
        if (html == null || html.isEmpty()) return java.util.Collections.emptyList();
        String s = html;
        // 剥离 markdown 代码围栏（AI 输出常带 ```html ... ```）
        s = s.replaceAll("(?s)```[a-zA-Z]*\\s*\\n?", "").replace("```", "");
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
            onPage += it.lineWeight;
            final boolean lastLine = (i == items.size() - 1);
            XWPFParagraph para = doc.createParagraph();
            stylePara(para, it.kind);
            spacing(para, it.lineWeight);

            if (it.kind == Kind.IMAGE && it.imageData != null) {
                // 嵌入图片：高度按 IMAGE_LINE_WEIGHT × 16pt 折算，与分页计数保持一致
                try {
                    XWPFRun run = para.createRun();
                    int picType = "png".equalsIgnoreCase(it.imageFormat)
                            ? XWPFDocument.PICTURE_TYPE_PNG
                            : XWPFDocument.PICTURE_TYPE_JPEG;
                    // 按原始宽高比等比缩放（宽度 420pt，最高 320pt），与分页计数保持一致
                    int imgHeightPt = (int) Math.ceil(it.imageHeightPt);
                    run.addPicture(new java.io.ByteArrayInputStream(it.imageData),
                            picType, "diagram.png",
                            Units.toEMU(it.imageWidthPt), Units.toEMU(imgHeightPt));
                } catch (Exception e) {
                    log.warn("DOCX 嵌入图片失败", e);
                }
            } else {
                int size; String font;
                switch (it.kind) {
                    case TITLE:         size = FS_TITLE; font = FONT_H;  break;
                    case VERSION:       size = FS_H2;    font = FONT_CN; break;
                    case TOC:           size = FS_BODY;  font = FONT_CN; break;
                    case H1:            size = FS_H1;    font = FONT_H;  break;
                    case H2:            size = FS_H2;    font = FONT_H;  break;
                    case FIGURE_CAPTION:size = 9;        font = FONT_CN; break;
                    default:            size = FS_BODY;  font = FONT_CN; break;
                }
                XWPFRun run = para.createRun();
                run.setText(it.text);
                run.setFontSize(size);
                run.setFontFamily(font);
                run.setBold(it.bold || it.kind == Kind.TITLE || it.kind == Kind.H1 || it.kind == Kind.H2);
            }

            // 累加 lineWeight 超过或达到本页容量时插分页符；分页符独立成段，不与末行文字混排
            if (onPage >= LPP) {
                onPage = onPage - LPP;
                boolean need = !lastLine || pageBreakAtLastLine;
                if (need) {
                    XWPFParagraph pbPara = doc.createParagraph();
                    spacing(pbPara, 1);
                    pbPara.createRun().addBreak(BreakType.PAGE);
                }
            }
        }
    }

    private static void addPageBreakOnly(XWPFDocument doc) {
        XWPFParagraph para = doc.createParagraph();
        spacing(para, 1);
        para.createRun().addBreak(BreakType.PAGE);
    }

    private static void stylePara(XWPFParagraph para, Kind k) {
        switch (k) {
            case TITLE:
            case VERSION:
            case FIGURE_CAPTION:
                para.setAlignment(ParagraphAlignment.CENTER);
                break;
            case BODY:
                // 首行缩进 2 字符（10pt 宋体 ≈ 210twips/字 × 2 = 420twips）
                para.setIndentationFirstLine(420);
                break;
            default:
                break;
        }
    }

    /** 行距 = lineWeight × 16pt（LSP=320twips），段前段后统一 0，保证每页严格 30 逻辑行 */
    private static void spacing(XWPFParagraph para, int lineWeight) {
        CTPPr ppr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        CTSpacing sp = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        sp.setBefore(BigInteger.ZERO);
        sp.setAfter(BigInteger.ZERO);
        sp.setLine(BigInteger.valueOf((long) LSP * lineWeight));
        sp.setLineRule(STLineSpacingRule.EXACT);
    }
}
