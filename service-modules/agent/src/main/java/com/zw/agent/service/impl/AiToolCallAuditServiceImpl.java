package com.zw.agent.service.impl;

import com.zw.agent.entity.AiToolCallAuditEntity;
import com.zw.agent.mapper.AiToolCallAuditMapper;
import com.zw.agent.service.AiToolCallAuditService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 工具调用审计表：记录Agent每一次工具调用的权限结果、参数、结果和耗时 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-06-28
 */
@Service
public class AiToolCallAuditServiceImpl extends ServiceImpl<AiToolCallAuditMapper, AiToolCallAuditEntity> implements AiToolCallAuditService {

}
