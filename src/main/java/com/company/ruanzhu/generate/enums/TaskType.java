package com.company.ruanzhu.generate.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskType {
    CODE("CODE", "代码生成"),
    MANUAL("MANUAL", "手册生成"),
    SCREENSHOT("SCREENSHOT", "截图生成");

    @EnumValue
    private final String code;
    private final String desc;

    TaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
