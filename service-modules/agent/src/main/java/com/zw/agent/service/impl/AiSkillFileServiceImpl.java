package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.DTO.AiSkillFileSaveRequest;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.mapper.AiSkillFileMapper;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.agent.service.AiSkillFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * Skill附属文件表：保存Skill目录下的SKILL.md、references、scripts和样例资源 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
@RequiredArgsConstructor
public class AiSkillFileServiceImpl extends ServiceImpl<AiSkillFileMapper, AiSkillFileEntity> implements AiSkillFileService {
}
