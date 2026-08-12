package com.company.ruanzhu.review.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.project.enums.ProjectStatus;
import com.company.ruanzhu.project.service.ProjectService;
import com.company.ruanzhu.review.enums.ReviewStatus;
import com.company.ruanzhu.review.model.ReviewSession;
import com.company.ruanzhu.review.model.dto.ReviewRequest;
import com.company.ruanzhu.review.model.vo.ReviewSessionVO;
import com.company.ruanzhu.review.repository.ReviewSessionRepository;
import com.company.ruanzhu.review.service.ReviewService;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewSessionRepository reviewRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReviewSessionVO startReview(Long projectId, Long reviewerId) {
        // Check if there's already an in-progress review
        ReviewSession existing = reviewRepository.findInProgressByProjectId(projectId);
        if (existing != null) {
            throw new BusinessException("Project already has an in-progress review");
        }

        ReviewSession session = new ReviewSession();
        session.setProjectId(projectId);
        session.setReviewerId(reviewerId);
        session.setStatus(ReviewStatus.IN_PROGRESS);
        session.setCreatedAt(LocalDateTime.now());

        reviewRepository.insert(session);

        // Update project status
        projectService.updateProjectStatus(projectId, ProjectStatus.REVIEWING);

        return toVO(session);
    }

    @Override
    @Transactional
    public ReviewSessionVO submitReview(Long projectId, ReviewRequest request) {
        ReviewSession session = reviewRepository.findInProgressByProjectId(projectId);
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        String action = request.getAction();
        if ("APPROVE".equalsIgnoreCase(action)) {
            session.setStatus(ReviewStatus.APPROVED);
            session.setApprovedAt(LocalDateTime.now());
            session.setComments(request.getComments());

            // Update project status
            projectService.updateProjectStatus(projectId, ProjectStatus.APPROVED);
        } else if ("REJECT".equalsIgnoreCase(action)) {
            session.setStatus(ReviewStatus.REJECTED);
            session.setComments(request.getComments());

            // Update project status back to GENERATED
            projectService.updateProjectStatus(projectId, ProjectStatus.GENERATING);
        } else {
            throw new BusinessException("Invalid review action: " + action);
        }

        reviewRepository.updateById(session);
        return toVO(session);
    }

    @Override
    public ReviewSessionVO getLatestReview(Long projectId) {
        ReviewSession session = reviewRepository.findLatestByProjectId(projectId);
        return session != null ? toVO(session) : null;
    }

    @Override
    public List<ReviewSessionVO> getReviewHistory(Long projectId) {
        return reviewRepository.findByProjectId(projectId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public boolean isProjectApproved(Long projectId) {
        ReviewSession session = reviewRepository.findLatestByProjectId(projectId);
        return session != null && session.getStatus() == ReviewStatus.APPROVED;
    }

    private ReviewSessionVO toVO(ReviewSession session) {
        ReviewSessionVO vo = new ReviewSessionVO();
        vo.setId(session.getId());
        vo.setProjectId(session.getProjectId());
        vo.setReviewerId(session.getReviewerId());
        vo.setStatus(session.getStatus());
        vo.setComments(session.getComments());
        vo.setApprovedAt(session.getApprovedAt());
        vo.setCreatedAt(session.getCreatedAt());

        // Get reviewer name
        User reviewer = userRepository.selectById(session.getReviewerId());
        if (reviewer != null) {
            vo.setReviewerName(reviewer.getRealName() != null ? reviewer.getRealName() : reviewer.getUsername());
        }

        return vo;
    }
}
