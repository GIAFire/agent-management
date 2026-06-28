package com.zw.auth.mapper;

import com.zw.auth.entity.DTO.UserInfoDTO;
import com.zw.auth.entity.SysUserEntity;
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
