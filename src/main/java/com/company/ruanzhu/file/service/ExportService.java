package com.company.ruanzhu.file.service;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.file.enums.FileType;
import com.company.ruanzhu.file.converter.WordToPdfConverter;
import com.company.ruanzhu.file.generator.ManualDocGenerator;
import com.company.ruanzhu.file.generator.SourceCodeDocGenerator;
import com.company.ruanzhu.file.generator.SummaryTxtGenerator;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.generate.service.CodeExpansionService;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for exporting all materials as a ZIP file.
 *
 * 交存方式 depositType：
 * - general（一般交存，默认）：源程序前30页+后30页（每页≥50行），文档前30页+后30页（每页≥30行）
 * - exceptional（例外交存）：暂未实现，预留入口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    public static final String DEPOSIT_GENERAL = "general";
    public static final String DEPOSIT_EXCEPTIONAL = "exceptional";

    private final ProjectService projectService;
    private final CodeExpansionService codeExpansionService;
    private final WordToPdfConverter pdfConverter;
    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;

    /**
     * Export all materials for a project as a ZIP file.
     * ZIP contains:
     * - {name}.txt (software summary)
     * - {name}+程序.docx (source code)
     * - {name}+程序.pdf (source code PDF)
     * - {name}+说明.docx (manual)
     * - {name}+说明.pdf (manual PDF)
     */
    public byte[] exportProject(Long projectId, String depositType) throws IOException {
        ProjectVO project = projectService.getProjectById(projectId);
        String baseName = project.getName();

        // 例外交存暂未实现，预留入口
        if (DEPOSIT_EXCEPTIONAL.equalsIgnoreCase(depositType)) {
            throw new BusinessException("例外交存功能即将推出，敬请期待");
        }

        log.info("Exporting project: {}, depositType={}", baseName, DEPOSIT_GENERAL);

        // Generate documents（优先使用用户在编辑页已保存的 GENERATED_CODE / MANUAL）
        SourceCodeDocGenerator codeGen = new SourceCodeDocGenerator(
                projectService, codeExpansionService, fileRecordRepository, storageClient, projectId);
        ManualDocGenerator manualGen = new ManualDocGenerator(
                projectService, fileRecordRepository, storageClient, projectId);
        SummaryTxtGenerator summaryGen = new SummaryTxtGenerator(projectService, projectId);

        byte[] summaryTxt = summaryGen.generateTxt();
        byte[] codeDocx = codeGen.generateWord();
        byte[] manualDocx = manualGen.generateWord();

        // 直接生成 PDF（iText，不依赖 LibreOffice/Aspose）
        byte[] codePdf = codeGen.generatePdf();
        byte[] manualPdf = manualGen.generatePdf();

        // Package into ZIP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add summary TXT
            addZipEntry(zos, baseName + ".txt", summaryTxt);

            // Add source code DOCX
            addZipEntry(zos, baseName + "程序.docx", codeDocx);

            // Add source code PDF
            addZipEntry(zos, baseName + "程序.pdf", codePdf);

            // Add manual DOCX
            addZipEntry(zos, baseName + "说明.docx", manualDocx);

            // Add manual PDF
            addZipEntry(zos, baseName + "说明.pdf", manualPdf);
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

    /**
     * 异步导出：生成 ZIP → 上传 MinIO → 记录 file_record，返回记录供前端下载。
     * 由 AsyncGenerateService.executeExport 调用，调用方负责更新任务进度。
     *
     * @return 已保存的 FileRecord（包含 storagePath / fileName / fileSize）
     */
    public FileRecord exportAndUpload(Long projectId, String depositType) throws IOException {
        ProjectVO project = projectService.getProjectById(projectId);
        String baseName = project.getName();

        // 1. 生成 ZIP 字节（复用同步逻辑）
        byte[] zipBytes = exportProject(projectId, depositType);

        // 2. 上传到 MinIO
        String fileName = baseName + ".zip";
        String storagePath = "projects/" + projectId + "/export/" + fileName;
        storageClient.upload(storagePath, new ByteArrayInputStream(zipBytes), "application/zip");
        log.info("导出 ZIP 已上传: projectId={}, storagePath={}, size={}B", projectId, storagePath, zipBytes.length);

        // 3. 写入 / 更新 file_record（同一项目只保留一份最新导出）
        List<FileRecord> existing = fileRecordRepository.findByProjectIdAndFileType(projectId, FileType.EXPORT_ZIP);
        FileRecord record;
        if (!existing.isEmpty()) {
            record = existing.get(0);
            record.setStoragePath(storagePath);
            record.setFileName(fileName);
            record.setFileSize((long) zipBytes.length);
            record.setVersion(record.getVersion() == null ? 1 : record.getVersion() + 1);
            fileRecordRepository.updateById(record);
        } else {
            record = new FileRecord();
            record.setProjectId(projectId);
            record.setFileType(FileType.EXPORT_ZIP);
            record.setFileName(fileName);
            record.setStoragePath(storagePath);
            record.setFileSize((long) zipBytes.length);
            record.setVersion(1);
            record.setCreatedAt(LocalDateTime.now());
            fileRecordRepository.insert(record);
        }
        return record;
    }

    /**
     * 异步打包上传 ZIP。供保存代码/手册/概要后调用，不阻塞保存操作。
     * 失败仅记录日志，不影响保存流程。
     */
    @Async("taskExecutor")
    public void exportAndUploadAsync(Long projectId, String depositType) {
        try {
            log.info("Async export pack started: projectId={}", projectId);
            exportAndUpload(projectId, depositType);
            log.info("Async export pack completed: projectId={}", projectId);
        } catch (Exception e) {
            log.error("Async export pack failed: projectId={}", projectId, e);
        }
    }

    private void addZipEntry(ZipOutputStream zos, String fileName, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }
}
