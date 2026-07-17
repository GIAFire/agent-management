package com.zw.agent.config;

import com.alibaba.nacos.api.ai.AiService;
import io.agentscope.core.nacos.skill.NacosSkillRepository;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;

import java.util.List;
import java.util.Properties;

public class WindowsSafeNacosSkillRepository
        extends NacosSkillRepository {

    private final String namespaceId;
    private final String safeSource;

    public WindowsSafeNacosSkillRepository(
            AiService aiService,
            String namespaceId,
            Properties properties,
            List<String> knownSkillNames
    ) {
        super(
                aiService,
                namespaceId,
                properties,
                knownSkillNames
        );

        this.namespaceId =
                namespaceId == null || namespaceId.isBlank()
                        ? "public"
                        : namespaceId.trim();

        this.safeSource =
                "nacos-" + sanitizePathSegment(this.namespaceId);
    }

    /**
     * 下载完成后替换 AgentSkill.source，
     * 避免 getSkillId() 中包含 Windows 非法字符 ':'。
     */
    @Override
    public AgentSkill getSkill(String name) {
        AgentSkill skill = super.getSkill(name);

        return skill.toBuilder()
                .source(safeSource)
                .build();
    }

    /**
     * 避免仓库 source 本身包含冒号。
     */
    @Override
    public String getSource() {
        return safeSource;
    }

    /**
     * location 同样改成路径安全格式。
     */
    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return new AgentSkillRepositoryInfo(
                "nacos",
                "namespace-" + sanitizePathSegment(namespaceId),
                false
        );
    }

    private static String sanitizePathSegment(String value) {
        return value.replaceAll(
                "[\\\\/:*?\"<>|]",
                "-"
        );
    }
}
