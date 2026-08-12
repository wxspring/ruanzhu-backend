package com.company.ruanzhu.user.model.dto;

import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String realName;
    private UserRole role;
    private UserStatus status;
}
