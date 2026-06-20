package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentVersionEntity;
import com.zw.agent.mapper.AiAgentVersionMapper;
import com.zw.agent.service.AiAgentVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent 版本表：保存每次可视化配置发布后的不可变快照 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentVersionServiceImpl extends ServiceImpl<AiAgentVersionMapper, AiAgentVersionEntity> implements AiAgentVersionService {

}
