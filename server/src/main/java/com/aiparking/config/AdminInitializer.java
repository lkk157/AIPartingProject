package com.aiparking.config;

import com.aiparking.user.entity.User;
import com.aiparking.user.mapper.RoleMapper;
import com.aiparking.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始化系统管理员账号（开发/演示环境）。
 * 生产环境应通过 app.init-admin.enabled=false 关闭。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public AdminInitializer(UserMapper userMapper, RoleMapper roleMapper,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.init-admin.enabled:true}") boolean enabled) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, "admin")) > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("系统管理员");
        admin.setStatus(1);
        userMapper.insert(admin);
        roleMapper.bindUserRole(admin.getId(), roleMapper.selectIdByCode("SYS_ADMIN"));
        log.info("已初始化系统管理员账号 admin / admin123（仅限开发环境）");
    }
}
