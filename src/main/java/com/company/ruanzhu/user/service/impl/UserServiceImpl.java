package com.company.ruanzhu.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.model.dto.UserUpdateRequest;
import com.company.ruanzhu.user.model.vo.UserVO;
import com.company.ruanzhu.user.repository.UserRepository;
import com.company.ruanzhu.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserVO createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());

        userRepository.insert(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userRepository.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userRepository.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    public UserVO getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public PageResult<UserVO> listUsers(PageRequest pageRequest) {
        Page<User> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (pageRequest.getSortBy() != null) {
            // Add sorting if needed
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userRepository.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                pageRequest.getPage(),
                pageRequest.getSize()
        );
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }
}
