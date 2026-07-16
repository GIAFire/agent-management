package com.zw.agent.mapper;

import com.zw.agent.entity.AiSkillFileEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 Mapper 接口
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Mapper
public interface AiSkillFileMapper extends BaseMapper<AiSkillFileEntity> {

}
