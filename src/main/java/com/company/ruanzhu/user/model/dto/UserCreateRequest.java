package com.company.ruanzhu.user.model.dto;

import com.company.ruanzhu.user.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100")
    private String password;

    @Size(max = 50, message = "真实姓名最长50")
    private String realName;

    private UserRole role = UserRole.STAFF;
}
