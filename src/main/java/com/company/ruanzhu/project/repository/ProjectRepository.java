package com.company.ruanzhu.project.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.project.enums.ProjectStatus;
import com.company.ruanzhu.project.model.Project;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ProjectRepository extends BaseMapper<Project> {
    default List<Project> findByCreatedBy(Long userId) {
        return selectList(
            new LambdaQueryWrapper<Project>()
                .eq(Project::getCreatedBy, userId)
                .orderByDesc(Project::getCreatedAt)
        );
    }

    default List<Project> findByStatus(ProjectStatus status) {
        return selectList(
            new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, status)
                .orderByDesc(Project::getCreatedAt)
        );
    }
}
