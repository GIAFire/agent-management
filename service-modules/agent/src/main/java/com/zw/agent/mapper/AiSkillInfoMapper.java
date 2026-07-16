package com.zw.agent.mapper;

import com.zw.agent.entity.AiSkillInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillInfoMapper extends BaseMapper<AiSkillInfoEntity> {

}
