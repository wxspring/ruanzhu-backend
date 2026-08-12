package com.company.ruanzhu.review.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.review.model.dto.ReviewRequest;
import com.company.ruanzhu.review.model.vo.ReviewSessionVO;
import com.company.ruanzhu.review.service.ReviewService;
import com.company.ruanzhu.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/projects/{projectId}/start")
    public Result<ReviewSessionVO> startReview(@PathVariable Long projectId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        ReviewSessionVO session = reviewService.startReview(projectId, principal.getId());
        return Result.success(session);
    }

    @PostMapping("/projects/{projectId}/submit")
    public Result<ReviewSessionVO> submitReview(@PathVariable Long projectId,
                                                 @RequestBody ReviewRequest request) {
        ReviewSessionVO session = reviewService.submitReview(projectId, request);
        return Result.success(session);
    }

    @GetMapping("/projects/{projectId}/latest")
    public Result<ReviewSessionVO> getLatestReview(@PathVariable Long projectId) {
        ReviewSessionVO session = reviewService.getLatestReview(projectId);
        return Result.success(session);
    }

    @GetMapping("/projects/{projectId}/history")
    public Result<List<ReviewSessionVO>> getReviewHistory(@PathVariable Long projectId) {
        return Result.success(reviewService.getReviewHistory(projectId));
    }

    @GetMapping("/projects/{projectId}/approved")
    public Result<Boolean> isProjectApproved(@PathVariable Long projectId) {
        return Result.success(reviewService.isProjectApproved(projectId));
    }
}
