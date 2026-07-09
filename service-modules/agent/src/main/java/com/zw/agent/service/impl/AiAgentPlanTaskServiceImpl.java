package com.zw.agent.service.impl;

import com.zw.agent.entity.AiAgentPlanTaskEntity;
import com.zw.agent.mapper.AiAgentPlanTaskMapper;
import com.zw.agent.service.AiAgentPlanTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态 服务实现类
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Service
public class AiAgentPlanTaskServiceImpl extends ServiceImpl<AiAgentPlanTaskMapper, AiAgentPlanTaskEntity> implements AiAgentPlanTaskService {

    /**
     * 按计划 ID 物理删除任务快照。
     * todo_write 每次都会给出完整任务列表，所以同步前直接清空旧快照再插入新快照，
     * 可以避免逻辑删除记录残留导致 plan_id + task_index 唯一索引冲突。
     */
    @Override
    public int physicalDeleteByPlanId(Long planId) {
        if (planId == null) {
            return 0;
        }
        return baseMapper.physicalDeleteByPlanId(planId);
    }
}
