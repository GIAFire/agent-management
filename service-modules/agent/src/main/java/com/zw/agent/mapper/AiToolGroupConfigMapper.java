package com.zw.agent.mapper;

import com.zw.agent.entity.AiToolGroupConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 工具组配置表 Mapper 接口
 * </p>
 *
 * @author 智纬
 * @since 2026-06-27
 */
@Mapper
public interface AiToolGroupConfigMapper extends BaseMapper<AiToolGroupConfigEntity> {

    List<AiToolGroupConfigEntity> selectGroups();

    AiToolGroupConfigEntity selectGroupById(@Param("id") Long id);

    int insertGroup(AiToolGroupConfigEntity entity);

    int updateGroupById(AiToolGroupConfigEntity entity);
}
