package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.AiKnowledgeTaskEntity;
import com.zw.agent.mapper.AiKnowledgeTaskMapper;
import com.zw.agent.service.AiKnowledgeTaskService;
import org.springframework.stereotype.Service;

@Service
public class AiKnowledgeTaskServiceImpl
        extends ServiceImpl<AiKnowledgeTaskMapper, AiKnowledgeTaskEntity>
        implements AiKnowledgeTaskService {
}
