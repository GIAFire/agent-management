package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentMessageEntity;
import com.zw.agent.mapper.AiAgentMessageMapper;
import com.zw.agent.service.AiAgentMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 消息表：保存用户输入、Agent 回复、工具消息等完整上下文 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentMessageServiceImpl extends ServiceImpl<AiAgentMessageMapper, AiAgentMessageEntity> implements AiAgentMessageService {

}
