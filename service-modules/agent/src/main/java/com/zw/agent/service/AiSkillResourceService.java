package com.zw.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zw.agent.entity.AiSkillResourceEntity;
import com.zw.agent.entity.DTO.AiSkillResourceSaveRequest;
import java.util.List;

public interface AiSkillResourceService extends IService<AiSkillResourceEntity> {

    List<AiSkillResourceEntity> listBySkill(Long skillId);

    String getContent(Long id);

    AiSkillResourceEntity createResource(AiSkillResourceSaveRequest request);

    AiSkillResourceEntity updateResource(AiSkillResourceSaveRequest request);

    boolean deleteResource(Long id);

    int deleteFolder(Long skillId, String folderPath);
}
