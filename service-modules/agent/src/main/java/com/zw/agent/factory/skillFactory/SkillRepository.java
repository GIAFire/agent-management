package com.zw.agent.factory.skillFactory;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Data
@RequiredArgsConstructor
@Component
public class SkillRepository implements AgentSkillRepository {
    private static final String REPOSITORY_TYPE = "mysql";
    private static final String REPOSITORY_LOCATION = "ai_skill_info";
    private static final String REPOSITORY_SOURCE = "mysql";
    private static final String DIRECTORY_ROLE = "DIRECTORY";
    private static final String SKILL_MD_ROLE = "SKILL_MD";
    private static final String SKILL_MD_PATH = "SKILL.md";

    private Long agentId;

    private final AiSkillInfoService skillInfoService;
    private final AiSkillFileService skillFileService;



    @Override
    public AgentSkill getSkill(String name) {
        SkillFileDTO skillInfo = skillInfoService.getAgentSkill(name,agentId);
        if (skillInfo == null) {
            throw new IllegalArgumentException("Skill not found: " + name);
        }

        Map<String, String> resources = new HashMap<>();
        appendResources(resources, skillInfo.getSkillFileList());
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
            LoadedSkillRecord loadedSkillRecord = skillRecords.computeIfAbsent(
                    skill.getId(),
                    ignored -> new LoadedSkillRecord(
                            skill.getName(),
                            skill.getDescription(),
                            skill.getSkillContent(),
                            skill.getSource(),
                            skill.getMetadataJson()));
            appendResources(loadedSkillRecord.getResources(), skill.getSkillFileList());
        }

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
        return new AgentSkillRepositoryInfo(REPOSITORY_TYPE, REPOSITORY_LOCATION, false);
    }

    @Override
    public String getSource() {
        return REPOSITORY_SOURCE;
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
        return new AgentSkill(metadata, skillContent, resources, resolveSource(source));
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

    private void appendResources(Map<String, String> resources, List<FileDTO> skillFileList) {
        if (resources == null || skillFileList == null || skillFileList.isEmpty()) {
            return;
        }
        for (FileDTO skillFile : skillFileList) {
            if (skillFile == null || shouldSkipResource(skillFile)) {
                continue;
            }
            String content = resolveResourceContent(skillFile);
            if (content != null) {
                resources.put(skillFile.getResourcePath(), content);
            }
        }
    }

    private boolean shouldSkipResource(FileDTO skillFile) {
        String resourcePath = skillFile.getResourcePath();
        if (!StringUtils.hasText(resourcePath)) {
            return true;
        }
        if (SKILL_MD_PATH.equals(resourcePath) || SKILL_MD_ROLE.equals(skillFile.getFileRole())) {
            return true;
        }
        return DIRECTORY_ROLE.equals(skillFile.getFileRole());
    }

    private String resolveResourceContent(FileDTO skillFile) {
        if (skillFile.getResourceContent() != null) {
            return skillFile.getResourceContent();
        }
        if (!StringUtils.hasText(skillFile.getStorageKey())) {
            return null;
        }
        return readLocalSkillFile(skillFile);
    }

    private String readLocalSkillFile(FileDTO skillFile) {
        try {
            Path projectRoot = Paths.get("").toAbsolutePath().normalize();
            Path storagePath = Paths.get(skillFile.getStorageKey());
            Path targetPath = storagePath.isAbsolute()
                    ? storagePath.normalize()
                    : projectRoot.resolve(storagePath).normalize();
            Path allowedRoot = projectRoot.resolve(".agentscope").normalize();
            if (!targetPath.startsWith(allowedRoot)) {
                log.warn("Skipping skill resource outside workspace storage: {}", skillFile.getResourcePath());
                return null;
            }
            if (!Files.exists(targetPath) || Files.isDirectory(targetPath)) {
                return null;
            }
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to read skill resource '{}': {}", skillFile.getResourcePath(), e.getMessage());
            return null;
        }
    }

    private String resolveSource(String source) {
        return StringUtils.hasText(source) ? source.trim() : REPOSITORY_SOURCE;
    }

}
