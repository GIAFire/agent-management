package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSubagentAgentBindingEntity;
import com.zw.agent.mapper.AiSubagentAgentBindingMapper;
import com.zw.agent.service.AiSubagentAgentBindingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 主Agent与子Agent绑定表：定义某个主Agent版本可以委派哪些子Agent及调用策略 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Service
public class AiSubagentAgentBindingServiceImpl extends ServiceImpl<AiSubagentAgentBindingMapper, AiSubagentAgentBindingEntity> implements AiSubagentAgentBindingService {

}
