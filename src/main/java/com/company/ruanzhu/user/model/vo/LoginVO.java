package com.company.ruanzhu.user.model.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO user;
}
