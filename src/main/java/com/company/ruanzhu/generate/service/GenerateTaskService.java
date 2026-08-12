package com.company.ruanzhu.generate.service;

import com.company.ruanzhu.generate.model.vo.GenerateTaskVO;

import java.util.List;

public interface GenerateTaskService {

    GenerateTaskVO createTask(Long projectId, String taskType);

    GenerateTaskVO getTask(Long taskId);

    List<GenerateTaskVO> getTasksByProjectId(Long projectId);

    void updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage);

    void updateTaskResult(Long taskId, String resultPath);
}
