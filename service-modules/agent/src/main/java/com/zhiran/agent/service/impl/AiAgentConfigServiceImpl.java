package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiAgentConfigEntity;
import com.zhiran.agent.mapper.AiAgentConfigMapper;
import com.zhiran.agent.service.AiAgentConfigService;
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
public class AiAgentConfigServiceImpl extends ServiceImpl<AiAgentConfigMapper, AiAgentConfigEntity> implements AiAgentConfigService {

}
