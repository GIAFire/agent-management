package com.zw.agent.mapper;

import com.zw.agent.entity.AiSkillAgentBindingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Agent与Skill绑定表：定义某个Agent配置版本安装哪些Skill以及安装作用域 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillAgentBindingMapper extends BaseMapper<AiSkillAgentBindingEntity> {

}
