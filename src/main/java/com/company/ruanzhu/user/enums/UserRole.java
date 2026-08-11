package com.company.ruanzhu.user.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {
    STAFF("STAFF", "普通员工"),
    ADMIN("ADMIN", "管理员");

    @EnumValue
    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
