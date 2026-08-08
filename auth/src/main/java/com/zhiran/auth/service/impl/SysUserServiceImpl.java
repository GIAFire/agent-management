package com.zhiran.auth.service.impl;

import com.zhiran.auth.entity.DTO.UserInfoDTO;
import com.zhiran.auth.entity.SysUserEntity;
import com.zhiran.auth.mapper.SysUserMapper;
import com.zhiran.auth.service.SysUserService;
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
    public UserInfoDTO login(String userName) {
        return sysUserMapper.login(userName);
    }
}
