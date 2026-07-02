package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentModelEntity;
import com.zw.agent.mapper.AiAgentModelMapper;
import com.zw.agent.service.AiAgentModelService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 模型配置表：把凭证、模型名、生成参数组合成可被 Agent 选择的模型 服务实现类
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Service
public class AiAgentModelServiceImpl extends ServiceImpl<AiAgentModelMapper, AiAgentModelEntity> implements AiAgentModelService {

}
