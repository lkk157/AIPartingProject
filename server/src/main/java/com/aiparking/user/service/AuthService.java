package com.aiparking.user.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.ForbiddenException;
import com.aiparking.common.UnauthorizedException;
import com.aiparking.security.JwtService;
import com.aiparking.security.UserContext;
import com.aiparking.user.dto.LoginRequest;
import com.aiparking.user.dto.LoginResponse;
import com.aiparking.user.dto.RegisterRequest;
import com.aiparking.user.dto.UserVO;
import com.aiparking.user.entity.User;
import com.aiparking.user.mapper.RoleMapper;
import com.aiparking.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, RoleMapper roleMapper,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserVO register(RegisterRequest request) {
        if (existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (StringUtils.hasText(request.getPhone()) && existsByPhone(request.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname()
                : request.getUsername());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : null);
        user.setStatus(1);
        userMapper.insert(user);

        Long roleId = roleMapper.selectIdByCode(DEFAULT_ROLE);
        roleMapper.bindUserRole(user.getId(), roleId);

        return toVO(user, List.of(DEFAULT_ROLE));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new ForbiddenException("账号已被禁用，请联系管理员");
        }

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        String token = jwtService.generate(user.getId(), user.getUsername(), roles);
        return new LoginResponse(token, toVO(user, roles));
    }

    public UserVO me() {
        Long userId = UserContext.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UnauthorizedException("用户不存在");
        }
        return toVO(user, userMapper.selectRoleCodesByUserId(userId));
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)) > 0;
    }

    private boolean existsByPhone(String phone) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)) > 0;
    }

    static UserVO toVO(User user, List<String> roles) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getStatus(),
                roles,
                user.getCreatedAt());
    }
}
