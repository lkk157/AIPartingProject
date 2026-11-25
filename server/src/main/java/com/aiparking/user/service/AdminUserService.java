package com.aiparking.user.service;

import com.aiparking.common.BusinessException;
import com.aiparking.common.PageResult;
import com.aiparking.security.UserContext;
import com.aiparking.user.dto.UserVO;
import com.aiparking.user.entity.User;
import com.aiparking.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminUserService {

    private final UserMapper userMapper;

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResult<UserVO> page(long page, long size, String keyword) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 100);

        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        query.orderByDesc(User::getId);

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<UserVO> records = result.getRecords().stream()
                .map(user -> AuthService.toVO(user, userMapper.selectRoleCodesByUserId(user.getId())))
                .toList();

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    public void updateStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态只能为 0 或 1");
        }
        if (userId.equals(UserContext.currentUserId())) {
            throw new BusinessException("不能修改自己的账号状态");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
    }
}
