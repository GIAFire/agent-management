package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentPlanOpLogEntity;
import com.zw.agent.mapper.AiAgentPlanOpLogMapper;
import com.zw.agent.service.AiAgentPlanOpLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Service
public class AiAgentPlanOpLogServiceImpl extends ServiceImpl<AiAgentPlanOpLogMapper, AiAgentPlanOpLogEntity> implements AiAgentPlanOpLogService {

}
