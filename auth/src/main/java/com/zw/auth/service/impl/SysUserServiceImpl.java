package com.zw.auth.service.impl;

import com.zw.auth.entity.DTO.UserInfoDTO;
import com.zw.auth.entity.SysUserEntity;
import com.zw.auth.mapper.SysUserMapper;
import com.zw.auth.service.SysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequiredArgsConstructor
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserEntity> implements SysUserService {
    private final SysUserMapper sysUserMapper;

    @Override
    public UserInfoDTO authenticate(String userName, String password) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        UserInfoDTO user = sysUserMapper.login(userName, password);
        if (user == null || !Objects.equals(user.getPassword(), password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已停用");
        }
        return user;
    }
}
