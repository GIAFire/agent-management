package com.zhiran.agent.mapper;

import com.zhiran.agent.entity.AiSkillAgentBindingEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM ai_skill_agent_binding WHERE skill_id = #{skillId} AND tenant_id = #{tenantId}")
    long countIncludingDeleted(
            @Param("skillId") Long skillId,
            @Param("tenantId") Long tenantId
    );
}
