package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentStateLogEntity;
import com.zw.agent.mapper.AiAgentStateLogMapper;
import com.zw.agent.service.AiAgentStateLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AgentState状态引用表：记录AgentScope运行时状态在外部存储中的位置和元数据 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Service
public class AiAgentStateLogServiceImpl extends ServiceImpl<AiAgentStateLogMapper, AiAgentStateLogEntity> implements AiAgentStateLogService {

}
