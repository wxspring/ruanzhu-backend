package com.company.ruanzhu.project.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ProjectStatus {
    CREATED("CREATED", "已创建"),
    SUMMARY_CONFIRMING("SUMMARY_CONFIRMING", "概要确认中"),
    GENERATING("GENERATING", "生成中"),
    REVIEWING("REVIEWING", "待审核"),
    APPROVED("APPROVED", "已审核"),
    EXPORTED("EXPORTED", "已导出");

    @EnumValue
    private final String code;
    private final String desc;

    ProjectStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
