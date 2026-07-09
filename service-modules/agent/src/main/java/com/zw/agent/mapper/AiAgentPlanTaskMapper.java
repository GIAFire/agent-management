package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentPlanTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Agent计划任务表：保存todo_write生成的结构化任务清单和执行状态 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-08
 */
@Mapper
public interface AiAgentPlanTaskMapper extends BaseMapper<AiAgentPlanTaskEntity> {

    /**
     * 按计划 ID 物理删除任务快照。
     * todo_write 是全量替换语义，任务表又有 plan_id + task_index 唯一索引；
     * 如果使用逻辑删除，旧记录仍会占用唯一键，第二次同步相同序号时会触发 DuplicateKeyException。
     */
    int physicalDeleteByPlanId(@Param("planId") Long planId);
}
