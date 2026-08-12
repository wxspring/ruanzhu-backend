package com.company.ruanzhu.generate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncGenerateService {

    private final GenerateTaskService taskService;
    private final AiContentService aiContentService;
    private final CodeExpansionService codeExpansionService;

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

            // Step 1: Generate content sections (70%)
            taskService.updateTaskStatus(taskId, "RUNNING", 70, null);

            // Step 2: Complete (100%)
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
