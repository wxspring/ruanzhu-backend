package com.company.ruanzhu.generate.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.generate.model.GenerateTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GenerateTaskRepository extends BaseMapper<GenerateTask> {

    default List<GenerateTask> findByProjectId(Long projectId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GenerateTask>()
                .eq(GenerateTask::getProjectId, projectId)
                .orderByDesc(GenerateTask::getCreatedAt));
    }

    default GenerateTask findLatestByProjectIdAndType(Long projectId, String taskType) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GenerateTask>()
                .eq(GenerateTask::getProjectId, projectId)
                .eq(GenerateTask::getTaskType, taskType)
                .orderByDesc(GenerateTask::getCreatedAt)
                .last("LIMIT 1"));
    }
}
