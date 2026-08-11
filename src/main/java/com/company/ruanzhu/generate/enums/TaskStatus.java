package com.company.ruanzhu.generate.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING("PENDING", "待处理"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败");

    @EnumValue
    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
