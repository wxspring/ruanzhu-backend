package com.company.ruanzhu.review.model.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String action; // APPROVE or REJECT
    private String comments;
}
