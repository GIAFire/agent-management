package com.zhiran.auth.mapper;

import com.zhiran.auth.entity.DTO.UserInfoDTO;
import com.zhiran.auth.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-21
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    UserInfoDTO login(String userName, String password);

}
