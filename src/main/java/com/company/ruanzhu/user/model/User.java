package com.company.ruanzhu.user.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.enums.UserStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
