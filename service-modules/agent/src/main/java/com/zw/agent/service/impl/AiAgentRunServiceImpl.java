package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunEntity;
import com.zw.agent.mapper.AiAgentRunMapper;
import com.zw.agent.service.AiAgentRunService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 运行表：一次用户请求对应一次 AgentScope call 或 streamEvents 执行 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentRunServiceImpl extends ServiceImpl<AiAgentRunMapper, AiAgentRunEntity> implements AiAgentRunService {

}
