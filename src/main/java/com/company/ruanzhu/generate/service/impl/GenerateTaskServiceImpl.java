package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.generate.enums.TaskStatus;
import com.company.ruanzhu.generate.enums.TaskType;
import com.company.ruanzhu.generate.model.GenerateTask;
import com.company.ruanzhu.generate.model.vo.GenerateTaskVO;
import com.company.ruanzhu.generate.repository.GenerateTaskRepository;
import com.company.ruanzhu.generate.service.GenerateTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GenerateTaskServiceImpl implements GenerateTaskService {

    private final GenerateTaskRepository taskRepository;

    @Override
    @Transactional
    public GenerateTaskVO createTask(Long projectId, String taskType) {
        GenerateTask task = new GenerateTask();
        task.setProjectId(projectId);
        task.setTaskType(TaskType.valueOf(taskType));
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());

        taskRepository.insert(task);
        return toVO(task);
    }

    @Override
    public GenerateTaskVO getTask(Long taskId) {
        GenerateTask task = taskRepository.selectById(taskId);
        return task != null ? toVO(task) : null;
    }

    @Override
    public List<GenerateTaskVO> getTasksByProjectId(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage) {
        GenerateTask task = taskRepository.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        task.setStatus(TaskStatus.valueOf(status));
        if (progress != null) {
            task.setProgress(progress);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }
        if (TaskStatus.RUNNING.name().equals(status)) {
            task.setStartedAt(LocalDateTime.now());
        }
        if (TaskStatus.SUCCESS.name().equals(status) || TaskStatus.FAILED.name().equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }

        taskRepository.updateById(task);
    }

    @Override
    @Transactional
    public void updateTaskResult(Long taskId, String resultPath) {
        GenerateTask task = taskRepository.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        task.setResultPath(resultPath);
        taskRepository.updateById(task);
    }

    private GenerateTaskVO toVO(GenerateTask task) {
        GenerateTaskVO vo = new GenerateTaskVO();
        vo.setId(task.getId());
        vo.setProjectId(task.getProjectId());
        vo.setTaskType(task.getTaskType());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setResultPath(task.getResultPath());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }
}
