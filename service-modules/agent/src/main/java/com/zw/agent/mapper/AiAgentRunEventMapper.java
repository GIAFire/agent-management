package com.zw.agent.mapper;

import com.zw.agent.entity.AiAgentRunEventEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent 运行事件表：保存流式 token、工具调用、权限请求等事件 Mapper 接口
 * </p>
 *
 * @author 
 * @since 2026-06-20
 */
@Mapper
public interface AiAgentRunEventMapper extends BaseMapper<AiAgentRunEventEntity> {

}
