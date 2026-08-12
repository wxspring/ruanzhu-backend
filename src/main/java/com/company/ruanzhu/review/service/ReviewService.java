package com.company.ruanzhu.review.service;

import com.company.ruanzhu.review.model.dto.ReviewRequest;
import com.company.ruanzhu.review.model.vo.ReviewSessionVO;

import java.util.List;

public interface ReviewService {

    ReviewSessionVO startReview(Long projectId, Long reviewerId);

    ReviewSessionVO submitReview(Long projectId, ReviewRequest request);

    ReviewSessionVO getLatestReview(Long projectId);

    List<ReviewSessionVO> getReviewHistory(Long projectId);

    boolean isProjectApproved(Long projectId);
}
