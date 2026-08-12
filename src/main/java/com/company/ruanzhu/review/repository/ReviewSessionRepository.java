package com.company.ruanzhu.review.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.ruanzhu.review.model.ReviewSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReviewSessionRepository extends BaseMapper<ReviewSession> {

    default List<ReviewSession> findByProjectId(Long projectId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewSession>()
                .eq(ReviewSession::getProjectId, projectId)
                .orderByDesc(ReviewSession::getCreatedAt));
    }

    default ReviewSession findLatestByProjectId(Long projectId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewSession>()
                .eq(ReviewSession::getProjectId, projectId)
                .orderByDesc(ReviewSession::getCreatedAt)
                .last("LIMIT 1"));
    }

    default ReviewSession findInProgressByProjectId(Long projectId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ReviewSession>()
                .eq(ReviewSession::getProjectId, projectId)
                .eq(ReviewSession::getStatus, "IN_PROGRESS")
                .last("LIMIT 1"));
    }
}
