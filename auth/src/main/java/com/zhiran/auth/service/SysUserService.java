package com.zhiran.auth.service;

import com.zhiran.auth.entity.DTO.UserInfoDTO;
import com.zhiran.auth.entity.SysUserEntity;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 
 * @since 2026-06-21
 */
public interface SysUserService extends IService<SysUserEntity> {

    UserInfoDTO authenticate(String userName, String password);
}
