package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentSessionEntity;
import com.zw.agent.mapper.AiAgentSessionMapper;
import com.zw.agent.service.AiAgentSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 会话表：保存用户与 Agent 的一次连续对话 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentSessionServiceImpl extends ServiceImpl<AiAgentSessionMapper, AiAgentSessionEntity> implements AiAgentSessionService {

}
