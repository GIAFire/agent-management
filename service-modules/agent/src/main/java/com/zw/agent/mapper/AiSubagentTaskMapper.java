package com.zw.agent.mapper;

import com.zw.agent.entity.AiSubagentTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 子Agent任务表：记录agent_spawn/agent_send产生的同步或后台委派任务 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Mapper
public interface AiSubagentTaskMapper extends BaseMapper<AiSubagentTaskEntity> {

}
