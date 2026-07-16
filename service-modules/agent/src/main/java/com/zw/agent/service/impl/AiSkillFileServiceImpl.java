package com.zw.agent.service.impl;

import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.mapper.AiSkillFileMapper;
import com.zw.agent.service.AiSkillFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
public class AiSkillFileServiceImpl extends ServiceImpl<AiSkillFileMapper, AiSkillFileEntity> implements AiSkillFileService {

}
