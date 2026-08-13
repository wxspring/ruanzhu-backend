package com.company.ruanzhu.file.generator;

import com.company.ruanzhu.generate.service.CodeExpansionService;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * Generates source code document (程序文档) in Word format.
 * Requirements:
 * - 60 pages total (first 30 + last 30)
 * - At least 50 lines per page
 * - Header with software name and version
 * - Monospace font (Courier New / Consolas)
 * - Line numbers
 */
@Slf4j

public class SourceCodeDocGenerator implements DocumentGenerator {

    private static final int LINES_PER_PAGE = 50;
    private static final int TOTAL_PAGES = 60;
    private static final int TARGET_LINES = LINES_PER_PAGE * TOTAL_PAGES; // 3000 lines

    private final ProjectService projectService;
    private final CodeExpansionService codeExpansionService;

    private final Long projectId;

    public SourceCodeDocGenerator(ProjectService projectService,
                                   CodeExpansionService codeExpansionService,
                                   Long projectId) {
        this.projectService = projectService;
        this.codeExpansionService = codeExpansionService;
        this.projectId = projectId;
    }

    @Override
    public byte[] generateWord() {
        ProjectVO project = projectService.getProjectById(projectId);
        SoftwareSummaryVO summary = project.getSoftwareSummary();

        // Get expanded code
        String code = codeExpansionService.expandCode(projectId);
        String[] lines = code.split("\n");

        try (XWPFDocument document = new XWPFDocument()) {
            // Add header with software name
            addHeader(document, project.getName(), summary != null ? summary.getVersion() : "V1.0");

            // Add code content with line numbers
            addCodeContent(document, lines);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate source code document", e);
            throw new RuntimeException("生成程序文档失败", e);
        }
    }

    @Override
    public String getFileName() {
        ProjectVO project = projectService.getProjectById(projectId);
        return project.getName() + "+程序.docx";
    }

    private void addHeader(XWPFDocument document, String softwareName, String version) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
        XWPFParagraph headerPara = header.getParagraphs().isEmpty()
                ? header.createParagraph()
                : header.getParagraphs().get(0);

        headerPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = headerPara.createRun();
        run.setText(softwareName + " " + version);
        run.setFontSize(9);
        run.setFontFamily("宋体");
    }

    private void addCodeContent(XWPFDocument document, String[] lines) {
        int lineCount = 0;
        int maxLines = Math.min(lines.length, TARGET_LINES);

        for (int i = 0; i < maxLines; i++) {
            lineCount++;
            XWPFParagraph para = document.createParagraph();

            // Set paragraph spacing (single line, compact)
            CTSpacing spacing = para.getCTP().addNewPPr().addNewSpacing();
            spacing.setAfter(BigInteger.ZERO);
            spacing.setBefore(BigInteger.ZERO);
            spacing.setLine(BigInteger.valueOf(240)); // Single line spacing

            // Add line number
            XWPFRun lineNumRun = para.createRun();
            lineNumRun.setText(String.format("%4d | ", lineCount));
            lineNumRun.setFontSize(8);
            lineNumRun.setFontFamily("Courier New");
            lineNumRun.setColor("888888");

            // Add code content
            XWPFRun codeRun = para.createRun();
            String lineContent = lines[i];
            // Truncate very long lines
            if (lineContent.length() > 100) {
                lineContent = lineContent.substring(0, 100) + "...";
            }
            codeRun.setText(lineContent);
            codeRun.setFontSize(9);
            codeRun.setFontFamily("Courier New");

            // Page break every 50 lines
            if (lineCount % LINES_PER_PAGE == 0 && lineCount < maxLines) {
                para.setPageBreak(true);
            }
        }
    }
}
