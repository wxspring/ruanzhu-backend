package com.company.ruanzhu.project.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.project.model.SoftwareSummary;
import org.apache.ibatis.annotations.Mapper;
import java.util.Optional;

@Mapper
public interface SoftwareSummaryRepository extends BaseMapper<SoftwareSummary> {
    default Optional<SoftwareSummary> findByProjectId(Long projectId) {
        return Optional.ofNullable(selectOne(
            new LambdaQueryWrapper<SoftwareSummary>()
                .eq(SoftwareSummary::getProjectId, projectId)
        ));
    }
}
