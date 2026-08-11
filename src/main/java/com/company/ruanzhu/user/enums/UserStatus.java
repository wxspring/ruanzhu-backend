package com.company.ruanzhu.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    private final String code;
    private final String desc;

    UserStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
