package com.zw.agent.service.impl;

import com.zw.agent.entity.SysTenantEntity;
import com.zw.agent.mapper.SysTenantMapper;
import com.zw.agent.service.SysTenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户表：平台多租户隔离的根表 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class SysTenantServiceImpl extends ServiceImpl<SysTenantMapper, SysTenantEntity> implements SysTenantService {

}
