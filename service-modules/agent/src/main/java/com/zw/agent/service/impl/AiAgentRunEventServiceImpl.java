package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentRunEventEntity;
import com.zw.agent.mapper.AiAgentRunEventMapper;
import com.zw.agent.service.AiAgentRunEventService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentRunEventServiceImpl extends ServiceImpl<AiAgentRunEventMapper, AiAgentRunEventEntity> implements AiAgentRunEventService {

}
