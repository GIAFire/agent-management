package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentStateOpLogEntity;
import com.zw.agent.mapper.AiAgentStateOpLogMapper;
import com.zw.agent.service.AiAgentStateOpLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * AgentState操作日志表：记录状态加载、保存、压缩、清理等操作 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-04
 */
@Service
public class AiAgentStateOpLogServiceImpl extends ServiceImpl<AiAgentStateOpLogMapper, AiAgentStateOpLogEntity> implements AiAgentStateOpLogService {

}
