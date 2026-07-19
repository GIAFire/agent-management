package com.zw.agent.factory.skillFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.zw.agent.entity.AiSkillFileEntity;
import com.zw.agent.entity.AiSkillInfoEntity;
import com.zw.agent.entity.DTO.FileDTO;
import com.zw.agent.entity.DTO.SkillFileDTO;
import com.zw.agent.service.AiSkillFileService;
import com.zw.agent.service.AiSkillInfoService;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.util.JsonUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Data
@RequiredArgsConstructor
@Component
public class SkillRepository implements AgentSkillRepository {
    private Long agentId;

    private final AiSkillInfoService skillInfoService;
    private final AiSkillFileService skillFileService;



    @Override
    public AgentSkill getSkill(String name) {
        SkillFileDTO skillInfo = skillInfoService.getAgentSkill(name,agentId);

        List<FileDTO> skillFileList =skillInfo.getSkillFileList();
        Map<String, String> resources = new HashMap<>();
        for (FileDTO skillFile : skillFileList) {
            resources.put(skillFile.getResourcePath(), skillFile.getResourceContent());
        }
        return buildSkill(skillInfo.getName(),
                skillInfo.getDescription(),
                skillInfo.getSkillContent(),
                skillInfo.getSource(),
                skillInfo.getMetadataJson(),
                resources);
    }

    @Override
    public List<String> getAllSkillNames() {
        List<String> skillNames = new ArrayList<>();
        skillInfoService.list().forEach(skillInfo -> skillNames.add(skillInfo.getName()));
        return skillNames;
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        List<SkillFileDTO> skillInfo = skillInfoService.getAgentSkillName(agentId);
        Map<Long, LoadedSkillRecord> skillRecords = new HashMap<>();
        for (SkillFileDTO skill : skillInfo){
            LoadedSkillRecord loadedSkillRecord = new LoadedSkillRecord(skill.getName(),
                    skill.getDescription(),
                    skill.getSkillContent(),
                    skill.getSource(),
                    skill.getMetadataJson());
            List<FileDTO> fileList = skill.getSkillFileList();
            for (FileDTO file : fileList){
                Map<String, String> resourceMap = new HashMap<>();
                resourceMap.put(file.getResourcePath(),file.getResourceContent());
                loadedSkillRecord.setResources(resourceMap);
            }
            skillRecords.put(skill.getId(),loadedSkillRecord);
        };

        List<AgentSkill> skills = new ArrayList<>(skillRecords.size());
        for (LoadedSkillRecord record : skillRecords.values()) {
            try {
                skills.add(
                        buildSkill(
                                record.getName(),
                                record.getDescription(),
                                record.getSkillContent(),
                                record.getSource(),
                                record.getMetadataJson(),
                                record.getResources()));
            } catch (Exception e) {
                log.warn("Failed to build skill: {}", e.getMessage(), e);
            }
        }
        return skills;
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean force) {
        return false;
    }

    @Override
    public boolean delete(String skillName) {
        return false;
    }

    @Override
    public boolean skillExists(String skillName) {
        return false;
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public void setWriteable(boolean writeable) {

    }

    @Override
    public boolean isWriteable() {
        return false;
    }


    private AgentSkill buildSkill(
            String name,
            String description,
            String skillContent,
            String source,
            String metadataJson,
            Map<String, String> resources) {
        Map<String, Object> metadata = deserializeMetadata(metadataJson, name, description);
        return new AgentSkill(metadata, skillContent, resources, source);
    }

    private Map<String, Object> deserializeMetadata(
            String metadataJson, String name, String description) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                Map<String, Object> parsed =
                        JsonUtils.getJsonCodec()
                                .fromJson(
                                        metadataJson, new TypeReference<Map<String, Object>>() {});
                if (parsed != null) {
                    metadata.putAll(parsed);
                }
            } catch (RuntimeException e) {
                log.warn(
                        "Failed to deserialize metadata_json for skill '{}', falling back to core"
                                + " metadata",
                        name,
                        e);
            }
        }
        metadata.put("name", name);
        metadata.put("description", description);
        return metadata;
    }



}
