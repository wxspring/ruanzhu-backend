package com.company.ruanzhu.review.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.review.enums.ReviewStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_session")
public class ReviewSession {
    private Long id;
    private Long projectId;
    private Long reviewerId;
    private ReviewStatus status;
    private String comments;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
}
