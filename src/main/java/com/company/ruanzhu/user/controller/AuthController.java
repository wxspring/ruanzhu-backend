package com.company.ruanzhu.user.controller;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.LoginRequest;
import com.company.ruanzhu.user.model.vo.LoginVO;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.repository.UserRepository;
import com.company.ruanzhu.user.security.JwtTokenProvider;
import com.company.ruanzhu.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        LoginVO vo = new LoginVO();
        vo.setToken(token);

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setRole(user.getRole());
        userVO.setStatus(user.getStatus());
        vo.setUser(userVO);

        return Result.success(vo);
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.selectById(principal.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());

        return Result.success(vo);
    }
}
