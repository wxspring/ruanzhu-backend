package com.company.ruanzhu.user.service;

import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;

public interface UserService {
    UserVO createUser(UserCreateRequest request);
    UserVO updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    UserVO getUserById(Long id);
    UserVO getUserByUsername(String username);
    PageResult<UserVO> listUsers(PageRequest pageRequest);
}
