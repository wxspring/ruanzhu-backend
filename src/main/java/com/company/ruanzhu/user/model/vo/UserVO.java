package com.company.ruanzhu.user.model.vo;

import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
