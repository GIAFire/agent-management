package com.zhiran.agent.mapper;

import com.zhiran.agent.entity.AiAgentPlanOpLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent计划操作日志表：记录plan_enter、plan_write、plan_exit、todo_write等计划相关事件 Mapper 接口
 * </p>
 *
 * @author zhiRan
 * @since 2026-07-08
 */
@Mapper
public interface AiAgentPlanOpLogMapper extends BaseMapper<AiAgentPlanOpLogEntity> {

}
