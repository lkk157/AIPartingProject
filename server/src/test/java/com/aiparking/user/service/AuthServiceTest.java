package com.aiparking.user.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.common.UnauthorizedException;
import com.aiparking.security.JwtService;
import com.aiparking.user.dto.LoginRequest;
import com.aiparking.user.dto.LoginResponse;
import com.aiparking.user.dto.RegisterRequest;
import com.aiparking.user.dto.UserVO;
import com.aiparking.user.entity.User;
import com.aiparking.user.mapper.RoleMapper;
import com.aiparking.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private JwtService jwtService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AuthService newService() {
        return new AuthService(userMapper, roleMapper, encoder, jwtService);
    }

    private RegisterRequest registerRequest(String username, String phone) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword("secret123");
        request.setPhone(phone);
        return request;
    }

    private User existingUser(String username, String rawPassword, int status) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setStatus(status);
        return user;
    }

    @Test
    void should_throw_when_register_duplicate_username() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> newService().register(registerRequest("alice", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void should_throw_when_register_duplicate_phone() {
        when(userMapper.selectCount(any())).thenReturn(0L, 1L);

        assertThatThrownBy(() -> newService().register(registerRequest("alice", "13800138000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("手机号已被注册");
    }

    @Test
    void should_encode_password_and_bind_default_role_when_register_success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(roleMapper.selectIdByCode("USER")).thenReturn(10L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            invocation.<User>getArgument(0).setId(99L);
            return 1;
        });

        UserVO vo = newService().register(registerRequest("alice", ""));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPassword()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", saved.getPassword())).isTrue();
        assertThat(saved.getNickname()).isEqualTo("alice");
        assertThat(saved.getStatus()).isEqualTo(1);
        verify(roleMapper).bindUserRole(99L, 10L);
        assertThat(vo.roles()).containsExactly("USER");
    }

    @Test
    void should_throw_unauthorized_when_login_with_wrong_password() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("alice", "secret123", 1));

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> newService().login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void should_throw_unauthorized_when_login_user_not_found() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("ghost");
        request.setPassword("secret123");

        assertThatThrownBy(() -> newService().login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_throw_forbidden_when_login_disabled_user() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("alice", "secret123", 0));

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        assertThatThrownBy(() -> newService().login(request))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("禁用");
    }

    @Test
    void should_return_token_and_roles_when_login_success() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("alice", "secret123", 1));
        when(userMapper.selectRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
        when(jwtService.generate(eq(1L), eq("alice"), any())).thenReturn("token-abc");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret123");

        LoginResponse response = newService().login(request);

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.user().username()).isEqualTo("alice");
        assertThat(response.user().roles()).containsExactly("USER");
    }

    @Test
    void should_not_call_jwt_when_password_wrong() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("alice", "secret123", 1));

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad");

        assertThatThrownBy(() -> newService().login(request))
                .isInstanceOf(UnauthorizedException.class);
        verify(jwtService, never()).generate(anyLong(), anyString(), any());
    }
}
