package com.company.ruanzhu.generate.service;

import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.service.ExportService;
import com.company.ruanzhu.file.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncGenerateService {

    private final GenerateTaskService taskService;
    private final AiContentService aiContentService;
    private final CodeExpansionService codeExpansionService;
    private final ManualGenerationService manualGenerationService;
    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;
    private final ExportService exportService;

    @Async("taskExecutor")
    public void executeCodeGeneration(Long taskId, Long projectId) {
        log.info("Starting code generation task: {}", taskId);
        try {
            taskService.updateTaskStatus(taskId, "RUNNING", 10, null);

            // Step 1: Analyze code (20%)
            taskService.updateTaskStatus(taskId, "RUNNING", 20, null);

            // Step 2: Expand/generate code (60%)
            taskService.updateTaskStatus(taskId, "RUNNING", 40, null);
            String generatedCode = codeExpansionService.expandCode(projectId);

            // Step 3: Save to storage (80%)
            taskService.updateTaskStatus(taskId, "RUNNING", 60, null);
            String fileName = "generated_code_" + projectId + ".txt";
            String storagePath = "projects/" + projectId + "/code/" + fileName;

            // Convert to bytes and upload
            byte[] contentBytes = generatedCode.getBytes(StandardCharsets.UTF_8);
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(contentBytes);
            storageClient.upload(storagePath, inputStream, "text/plain");

            // Create or update file record
            FileRecord fileRecord = new FileRecord();
            fileRecord.setProjectId(projectId);
            fileRecord.setFileType("GENERATED_CODE");
            fileRecord.setFileName(fileName);
            fileRecord.setStoragePath(storagePath);
            fileRecord.setFileSize((long) contentBytes.length);
            fileRecord.setVersion(1);

            // Check if generated code file already exists
            java.util.List<FileRecord> existingFiles = fileRecordRepository.findByProjectIdAndFileType(projectId, "GENERATED_CODE");
            if (!existingFiles.isEmpty()) {
                // Update existing record
                FileRecord existing = existingFiles.get(0);
                existing.setStoragePath(storagePath);
                existing.setFileSize(fileRecord.getFileSize());
                existing.setVersion(existing.getVersion() + 1);
                fileRecordRepository.updateById(existing);
            } else {
                // Insert new record
                fileRecordRepository.insert(fileRecord);
            }

            // Step 4: 打包上传 ZIP（导出时可直接下载）
            try {
                exportService.exportAndUpload(projectId, "general");
                log.info("Export pack uploaded after code generation: taskId={}", taskId);
            } catch (Exception ex) {
                log.warn("Export pack after code generation failed (non-fatal): {}", ex.getMessage());
            }

            // Step 5: Complete (100%)
            taskService.updateTaskStatus(taskId, "SUCCESS", 100, null);
            log.info("Code generation task completed: {}", taskId);

        } catch (Exception e) {
            log.error("Code generation task failed: {}", taskId, e);
            taskService.updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeManualGeneration(Long taskId, Long projectId) {
        log.info("Starting manual generation task: {}", taskId);
        try {
            taskService.updateTaskStatus(taskId, "RUNNING", 10, null);

            // Step 1: Generate manual content (70%)
            taskService.updateTaskStatus(taskId, "RUNNING", 40, null);
            String htmlContent = manualGenerationService.generateManual(projectId);

            // Step 2: Save to storage (90%)
            taskService.updateTaskStatus(taskId, "RUNNING", 70, null);
            String fileName = "manual_" + projectId + ".html";
            String storagePath = "projects/" + projectId + "/manual/" + fileName;

            // Convert to bytes and upload
            byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(contentBytes);
            storageClient.upload(storagePath, inputStream, "text/html");

            // Create or update file record
            FileRecord fileRecord = new FileRecord();
            fileRecord.setProjectId(projectId);
            fileRecord.setFileType("MANUAL");
            fileRecord.setFileName(fileName);
            fileRecord.setStoragePath(storagePath);
            fileRecord.setFileSize((long) contentBytes.length);
            fileRecord.setVersion(1);

            // Check if manual file already exists
            java.util.List<FileRecord> existingFiles = fileRecordRepository.findByProjectIdAndFileType(projectId, "MANUAL");
            if (!existingFiles.isEmpty()) {
                // Update existing record
                FileRecord existing = existingFiles.get(0);
                existing.setStoragePath(storagePath);
                existing.setFileSize(fileRecord.getFileSize());
                existing.setVersion(existing.getVersion() + 1);
                fileRecordRepository.updateById(existing);
            } else {
                // Insert new record
                fileRecordRepository.insert(fileRecord);
            }

            // Step 3: 打包上传 ZIP（导出时可直接下载）
            try {
                exportService.exportAndUpload(projectId, "general");
                log.info("Export pack uploaded after manual generation: taskId={}", taskId);
            } catch (Exception ex) {
                log.warn("Export pack after manual generation failed (non-fatal): {}", ex.getMessage());
            }

            // Step 4: Complete (100%)
            taskService.updateTaskStatus(taskId, "SUCCESS", 100, null);
            log.info("Manual generation task completed: {}", taskId);

        } catch (Exception e) {
            log.error("Manual generation task failed: {}", taskId, e);
            taskService.updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void executeSummaryGeneration(Long taskId, Long projectId) {
        log.info("Starting summary generation task: {}", taskId);
        try {
            taskService.updateTaskStatus(taskId, "RUNNING", 10, null);

            // Step 1: Generate all fields (90%)
            taskService.updateTaskStatus(taskId, "RUNNING", 90, null);

            // Step 2: Complete (100%)
            taskService.updateTaskStatus(taskId, "SUCCESS", 100, null);
            log.info("Summary generation task completed: {}", taskId);

        } catch (Exception e) {
            log.error("Summary generation task failed: {}", taskId, e);
            taskService.updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    /**
     * 异步导出材料 ZIP。
     * 进度划分：10%(启动) → 40%(生成程序文档) → 70%(生成说明文档+截图) → 90%(打包+上传MinIO) → 100%(完成)
     */
    @Async("taskExecutor")
    public void executeExport(Long taskId, Long projectId, String depositType) {
        log.info("Starting export task: {}, projectId={}, depositType={}", taskId, projectId, depositType);
        try {
            taskService.updateTaskStatus(taskId, "RUNNING", 10, null);

            // 导出（内部生成程序/说明文档+截图+打包ZIP+上传MinIO+记录file_record）
            // 这里无法精确拆分内部进度，按经验给一个粗粒度进度：进入导出时 40%，完成时 90%
            taskService.updateTaskStatus(taskId, "RUNNING", 40, null);
            FileRecord record = exportService.exportAndUpload(projectId, depositType);

            taskService.updateTaskStatus(taskId, "RUNNING", 90, null);

            // 记录结果路径，前端拿到后调下载接口
            taskService.updateTaskResult(taskId, record.getStoragePath());
            taskService.updateTaskStatus(taskId, "SUCCESS", 100, null);
            log.info("Export task completed: {}, file={}", taskId, record.getFileName());
        } catch (Exception e) {
            log.error("Export task failed: {}", taskId, e);
            taskService.updateTaskStatus(taskId, "FAILED", null, e.getMessage());
        }
    }

    /**
     * 静默异步打包上传 ZIP（无任务记录、无进度回调）。
     * 在 AI 生成代码/手册、保存代码/手册/概要后调用，使导出按钮始终能直接下载最新材料。
     */
    @Async("taskExecutor")
    public void executeExportPack(Long projectId) {
        try {
            log.info("Async export pack started: projectId={}", projectId);
            exportService.exportAndUpload(projectId, "general");
            log.info("Async export pack completed: projectId={}", projectId);
        } catch (Exception e) {
            log.error("Async export pack failed: projectId={}", projectId, e);
        }
    }
}
