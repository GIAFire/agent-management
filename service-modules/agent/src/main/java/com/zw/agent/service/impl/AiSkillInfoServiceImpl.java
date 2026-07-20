package com.zw.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zw.agent.constant.AgentConstant;
import com.zw.agent.entity.AiAgentWorkspaceFileEntity;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.SkillFileDTO;
import com.zw.agent.mapper.AiSkillInfoMapper;
import com.zw.agent.service.AiAgentWorkspaceFileService;
import com.zw.agent.service.AiSkillFileService;
import com.zw.agent.service.AiSkillInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zw.common.context.UserContext;
import com.zw.common.context.UserInfo;
import com.zw.common.support.EntityDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.zw.agent.constant.AgentConstant.DEFAULT_SKILL_MD_FILE;

/**
 * <p>
 * Skill定义表：保存可复用能力包的基础信息和当前发布版本 服务实现类
 * </p>
 *
 * @author 智伟
 * @since 2026-07-16
 */
@Service
@RequiredArgsConstructor
public class AiSkillInfoServiceImpl extends ServiceImpl<AiSkillInfoMapper, AiSkillInfoEntity> implements AiSkillInfoService {

}
