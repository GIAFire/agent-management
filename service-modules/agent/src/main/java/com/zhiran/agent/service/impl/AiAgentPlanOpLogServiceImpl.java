package com.zhiran.agent.service.impl;

import com.zhiran.agent.entity.AiAgentPlanOpLogEntity;
import com.zhiran.agent.mapper.AiAgentPlanOpLogMapper;
import com.zhiran.agent.service.AiAgentPlanOpLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件 服务实现类
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-08
 */
@Service
public class AiAgentPlanOpLogServiceImpl extends ServiceImpl<AiAgentPlanOpLogMapper, AiAgentPlanOpLogEntity> implements AiAgentPlanOpLogService {

}
