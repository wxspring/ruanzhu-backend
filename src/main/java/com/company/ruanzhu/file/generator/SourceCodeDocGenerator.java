package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.generate.service.CodeExpansionService;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 源程序文档：60 页 = 前 30 + 后 30，每页 50 行。
 * <p>
 * 页面设置：A4 纵向，无页眉页脚（吃空间），上下 8mm / 左右 10mm 边距。
 * 行距用 EXACT：bodyHeight / 50 ≈ 320 twips (16pt)，Calibri 8pt。
 * 代码不足 3000 行时循环拼接，保证前后 30 页都是真实代码。
 * 每页满 50 段即在该段末加 PAGE break，总 break 59 = 60 页。
 */
@Slf4j
public class SourceCodeDocGenerator implements DocumentGenerator {

    static final int LPP = 50;
    static final int FP  = 30;
    static final int BP  = 30;
    static final int FL  = LPP * FP;          // 1500
    static final int BL  = LPP * BP;          // 1500
    static final int TOTAL_OUT_LINES = FL + BL; // 3000

    private static final String FONT = "Calibri";
    private static final int FS = 8;
    // A4 正文高度 = 297 - 8 - 8 = 281mm = 15930 twips = 796.5pt
    // 每页 50 行 → 每行 ≤ 796.5/50 = 15.93pt = 318.6 twips
    // 取 310 twips (15.5pt) 留余量，避免末行溢出产生空白页
    private static final int LSP = 310;            // 15.5pt EXACT

    private final ProjectService projectService;
    private final CodeExpansionService codeExpansionService;
    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;
    private final Long projectId;

    public SourceCodeDocGenerator(ProjectService projectService,
                                  CodeExpansionService codeExpansionService,
                                  FileRecordRepository fileRecordRepository,
                                  StorageClient storageClient,
                                  Long projectId) {
        this.projectService = projectService;
        this.codeExpansionService = codeExpansionService;
        this.fileRecordRepository = fileRecordRepository;
        this.storageClient = storageClient;
        this.projectId = projectId;
    }

    @Override public byte[] generateWord() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();
        List<String> src = normalizedLines(resolveCodeContent());
        int N = src.size();

        // 代码不足 3000 行时循环拼接，保证前/后 30 页都是真实代码
        List<String> expanded = ensureAtLeast(src, TOTAL_OUT_LINES);
        List<String> front = new ArrayList<>(expanded.subList(0, FL));
        List<String> back  = new ArrayList<>(expanded.subList(expanded.size() - BL, expanded.size()));

        try (XWPFDocument doc = new XWPFDocument()) {
            setupPage(doc);
            writeBlock(doc, front, true);
            writeBlock(doc, back, false);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            log.info("程序文档：源={}行 循环后={}行 输出=60页x{}行", N, expanded.size(), LPP);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("generateWord failed", e);
            throw new RuntimeException("生成程序文档失败", e);
        }
    }

    @Override public String getFileName() {
        return projectService.getProjectById(projectId).getName() + "程序.docx";
    }

    /**
     * 用 iText 直接生成 PDF（不依赖 LibreOffice/Aspose）。
     * A4，上下 8mm / 左右 10mm 边距，Calibri 8pt，每页 50 行。
     */
    @Override public byte[] generatePdf() {
        List<String> src = normalizedLines(resolveCodeContent());
        List<String> expanded = ensureAtLeast(src, TOTAL_OUT_LINES);
        List<String> front = new ArrayList<>(expanded.subList(0, FL));
        List<String> back  = new ArrayList<>(expanded.subList(expanded.size() - BL, expanded.size()));

        // A4: 595×842pt; 边距 8mm≈22.7pt, 10mm≈28.3pt
        Rectangle page = PageSize.A4;
        float mTop = 22.7f, mBottom = 22.7f, mLeft = 28.3f, mRight = 28.3f;
        com.itextpdf.text.Document doc = new com.itextpdf.text.Document(page, mLeft, mRight, mTop, mBottom);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();

            // 字体：用 Helvetica（iText 内置）8pt，行距 15.5pt
            Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK);
            float leading = 15.5f; // 每行 15.5pt × 50 = 775pt < 正文 796pt

            List<String> all = new ArrayList<>(front);
            all.addAll(back);

            int lineNo = 0;
            for (String line : all) {
                lineNo++;
                Paragraph p = new Paragraph(line != null ? line : "", font);
                p.setLeading(leading);
                p.setSpacingAfter(0);
                p.setSpacingBefore(0);
                // 每 50 行强制换页
                if (lineNo % LPP == 0 && lineNo < all.size()) {
                    doc.add(p);
                    doc.newPage();
                } else {
                    doc.add(p);
                }
            }
            doc.close();
            log.info("程序文档 PDF 生成：{} 行，每页 {} 行", all.size(), LPP);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("generatePdf failed", e);
            throw new RuntimeException("生成程序文档 PDF 失败", e);
        }
    }

    /** 代码不足 minLen 时循环拼接 */
    private static List<String> ensureAtLeast(List<String> src, int minLen) {
        if (src.size() >= minLen) return src;
        List<String> out = new ArrayList<>(minLen);
        while (out.size() < minLen) out.addAll(src);
        return out;
    }

    private String resolveCodeContent() {
        List<FileRecord> saved = fileRecordRepository.findByProjectIdAndFileType(projectId, "GENERATED_CODE");
        if (!saved.isEmpty()) {
            FileRecord latest = saved.stream()
                    .max(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 0))
                    .get();
            byte[] data = storageClient.download(latest.getStoragePath());
            String c = new String(data, StandardCharsets.UTF_8);
            log.info("程序文档：用保存的 GENERATED_CODE name={}, size={}B, lines={}",
                    latest.getFileName(), data.length, countLines(c));
            return c;
        }
        String f = codeExpansionService.expandCode(projectId);
        log.info("程序文档：GENERATED_CODE 缺失，回退 expandCode lines={}", countLines(f));
        return f;
    }

    private static int countLines(String c) {
        if (c == null || c.isEmpty()) return 0;
        return c.split("\r?\n").length;
    }

    private static List<String> normalizedLines(String c) {
        if (c == null) c = "";
        List<String> r = new ArrayList<>();
        for (String s : c.split("\r?\n")) r.add(s.replace("\t", "    "));
        return r;
    }

    // ===== 页面设置：A4 纵向，紧凑边距，无页眉页脚 =====
    private static void setupPage(XWPFDocument doc) {
        CTSectPr sect = doc.getDocument().getBody().addNewSectPr();
        CTPageSz sz = sect.addNewPgSz();
        sz.setW(BigInteger.valueOf(11906));   // A4 宽度 210mm
        sz.setH(BigInteger.valueOf(16838));   // A4 高度 297mm
        CTPageMar m = sect.addNewPgMar();
        m.setTop(BigInteger.valueOf(mm2t(8)));
        m.setBottom(BigInteger.valueOf(mm2t(8)));
        m.setLeft(BigInteger.valueOf(mm2t(10)));
        m.setRight(BigInteger.valueOf(mm2t(10)));
        // 不设 Header/Footer 距离，不需要页眉页脚，省空间给正文
    }
    private static int mm2t(int mm){ return (int) Math.round(mm * 1440.0 / 25.4); }

    // ===== 输出 =====
    private static void writeBlock(XWPFDocument doc, List<String> lines, boolean pageBreakAtLastLine) {
        int onPage = 0;
        for (int i = 0; i < lines.size(); i++) {
            onPage++;
            final boolean lastLine = (i == lines.size() - 1);
            XWPFParagraph para = doc.createParagraph();
            setExactSpacing(para, LSP);

            XWPFRun run = para.createRun();
            String t = lines.get(i);
            if (t != null && !t.isEmpty()) run.setText(t, 0);
            run.setFontFamily(FONT);
            run.setFontSize(FS);

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

    private static void setExactSpacing(XWPFParagraph para, int lspTwips) {
        CTPPr ppr = para.getCTP().addNewPPr();
        CTSpacing sp = ppr.addNewSpacing();
        sp.setBefore(BigInteger.ZERO);
        sp.setAfter(BigInteger.ZERO);
        sp.setLine(BigInteger.valueOf(lspTwips));
        sp.setLineRule(STLineSpacingRule.EXACT);
    }
}
