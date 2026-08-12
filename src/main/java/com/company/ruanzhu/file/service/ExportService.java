package com.company.ruanzhu.file.service;

import com.company.ruanzhu.file.converter.WordToPdfConverter;
import com.company.ruanzhu.file.generator.ManualDocGenerator;
import com.company.ruanzhu.file.generator.SourceCodeDocGenerator;
import com.company.ruanzhu.file.generator.SummaryTxtGenerator;
import com.company.ruanzhu.generate.service.CodeExpansionService;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for exporting all materials as a ZIP file.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ProjectService projectService;
    private final CodeExpansionService codeExpansionService;
    private final WordToPdfConverter pdfConverter;

    /**
     * Export all materials for a project as a ZIP file.
     * ZIP contains:
     * - {name}.txt (software summary)
     * - {name}+程序.docx (source code)
     * - {name}+程序.pdf (source code PDF)
     * - {name}+说明.docx (manual)
     * - {name}+说明.pdf (manual PDF)
     */
    public byte[] exportProject(Long projectId) throws IOException {
        ProjectVO project = projectService.getProjectById(projectId);
        String baseName = project.getName();

        log.info("Exporting project: {}", baseName);

        // Generate documents
        SourceCodeDocGenerator codeGen = new SourceCodeDocGenerator(projectService, codeExpansionService, projectId);
        ManualDocGenerator manualGen = new ManualDocGenerator(projectService, projectId);
        SummaryTxtGenerator summaryGen = new SummaryTxtGenerator(projectService, projectId);

        byte[] summaryTxt = summaryGen.generateTxt();
        byte[] codeDocx = codeGen.generateWord();
        byte[] manualDocx = manualGen.generateWord();

        // Convert to PDF (placeholder implementation)
        byte[] codePdf = pdfConverter.convert(codeDocx);
        byte[] manualPdf = pdfConverter.convert(manualDocx);

        // Package into ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add summary TXT
            addZipEntry(zos, baseName + ".txt", summaryTxt);

            // Add source code DOCX
            addZipEntry(zos, baseName + "+程序.docx", codeDocx);

            // Add source code PDF
            addZipEntry(zos, baseName + "+程序.pdf", codePdf);

            // Add manual DOCX
            addZipEntry(zos, baseName + "+说明.docx", manualDocx);

            // Add manual PDF
            addZipEntry(zos, baseName + "+说明.pdf", manualPdf);
        }

        log.info("Export completed for project: {}", baseName);
        return baos.toByteArray();
    }

    /**
     * Get the ZIP file name for a project.
     */
    public String getZipFileName(Long projectId) {
        ProjectVO project = projectService.getProjectById(projectId);
        return project.getName() + ".zip";
    }

    private void addZipEntry(ZipOutputStream zos, String fileName, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }
}
