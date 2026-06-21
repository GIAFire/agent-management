package com.zw.auth.service.impl;

import com.zw.auth.entity.SysUserEntity;
import com.zw.auth.mapper.SysUserMapper;
import com.zw.auth.service.SysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-21
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserEntity> implements SysUserService {

    @Override
    public SysUserEntity authenticate(String userName, String password) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        SysUserEntity user = lambdaQuery()
                .eq(SysUserEntity::getUserName, userName)
                .one();
        if (user == null || !Objects.equals(user.getPassword(), password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已停用");
        }
        return user;
    }
}
