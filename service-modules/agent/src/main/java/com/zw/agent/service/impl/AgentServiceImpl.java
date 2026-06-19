package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.agent.entity.AgentEntity;
import com.zw.agent.mapper.AgentMapper;
import com.zw.agent.service.AgentService;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, AgentEntity> implements AgentService {
}
