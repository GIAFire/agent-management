package com.zw.agent.mapper;

import com.zw.agent.entity.AiSubagentInstanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 子Agent运行实例表：记录父Agent创建的子Agent实例、会话、工作区和暴露状态 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-07-11
 */
@Mapper
public interface AiSubagentInstanceMapper extends BaseMapper<AiSubagentInstanceEntity> {

}
