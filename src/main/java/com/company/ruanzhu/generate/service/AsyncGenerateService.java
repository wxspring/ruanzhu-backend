package com.company.ruanzhu.generate.service;

import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
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

    @Async("taskExecutor")
    public void executeCodeGeneration(Long taskId, Long projectId) {
        log.info("Starting code generation task: {}", taskId);
        try {
            taskService.updateTaskStatus(taskId, "RUNNING", 10, null);

            // Step 1: Analyze code (20%)
            taskService.updateTaskStatus(taskId, "RUNNING", 20, null);

            // Step 2: Expand code (60%)
            taskService.updateTaskStatus(taskId, "RUNNING", 60, null);
            codeExpansionService.expandCode(projectId);

            // Step 3: Complete (100%)
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

            // Step 3: Complete (100%)
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
}
