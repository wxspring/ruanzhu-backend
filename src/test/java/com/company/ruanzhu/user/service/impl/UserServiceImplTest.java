package com.company.ruanzhu.user.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.model.User;
import com.company.ruanzhu.user.model.dto.UserCreateRequest;
import com.company.ruanzhu.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // Setup if needed
    }

    @Test
    void createUser_Success() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setRealName("Test User");
        request.setRole(UserRole.STAFF);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.insert(any(User.class))).thenReturn(1);

        var result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).insert(any(User.class));
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(request));
    }
}
