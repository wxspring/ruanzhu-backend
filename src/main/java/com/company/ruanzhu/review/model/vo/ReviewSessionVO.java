package com.company.ruanzhu.review.model.vo;

import com.company.ruanzhu.review.enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewSessionVO {
    private Long id;
    private Long projectId;
    private Long reviewerId;
    private String reviewerName;
    private ReviewStatus status;
    private String comments;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
