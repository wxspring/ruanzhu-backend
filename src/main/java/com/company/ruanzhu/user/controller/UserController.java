package com.company.ruanzhu.user.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @GetMapping
    public Result<PageResult<UserVO>> listUsers(@Valid PageRequest pageRequest) {
        return Result.success(userService.listUsers(pageRequest));
    }
}
