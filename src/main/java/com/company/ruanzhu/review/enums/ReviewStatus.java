package com.company.ruanzhu.review.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ReviewStatus {
    IN_PROGRESS("IN_PROGRESS", "审核中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝");

    @EnumValue
    private final String code;
    private final String desc;

    ReviewStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
