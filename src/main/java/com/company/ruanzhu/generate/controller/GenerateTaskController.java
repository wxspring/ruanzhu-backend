package com.company.ruanzhu.generate.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.generate.model.vo.GenerateTaskVO;
import com.company.ruanzhu.generate.service.AsyncGenerateService;
import com.company.ruanzhu.generate.service.GenerateTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class GenerateTaskController {

    private final GenerateTaskService taskService;
    private final AsyncGenerateService asyncGenerateService;

    @PostMapping("/projects/{projectId}/generate")
    public Result<GenerateTaskVO> startGeneration(@PathVariable Long projectId,
                                                   @RequestParam String taskType) {
        GenerateTaskVO task = taskService.createTask(projectId, taskType);

        // Start async execution
        switch (taskType) {
            case "CODE":
                asyncGenerateService.executeCodeGeneration(task.getId(), projectId);
                break;
            case "MANUAL":
                asyncGenerateService.executeManualGeneration(task.getId(), projectId);
                break;
            case "SUMMARY":
                asyncGenerateService.executeSummaryGeneration(task.getId(), projectId);
                break;
            default:
                throw new IllegalArgumentException("Unknown task type: " + taskType);
        }

        return Result.success(task);
    }

    @GetMapping("/{taskId}")
    public Result<GenerateTaskVO> getTask(@PathVariable Long taskId) {
        GenerateTaskVO task = taskService.getTask(taskId);
        if (task == null) {
            return Result.error("Task not found");
        }
        return Result.success(task);
    }

    @GetMapping("/projects/{projectId}")
    public Result<List<GenerateTaskVO>> getTasksByProject(@PathVariable Long projectId) {
        return Result.success(taskService.getTasksByProjectId(projectId));
    }
}
