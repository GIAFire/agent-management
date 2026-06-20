package com.zw.agent.service.impl;

import com.zw.agent.entity.AiTenantEntity;
import com.zw.agent.mapper.AiTenantMapper;
import com.zw.agent.service.AiTenantService;
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
public class AiTenantServiceImpl extends ServiceImpl<AiTenantMapper, AiTenantEntity> implements AiTenantService {

}
