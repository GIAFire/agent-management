package com.zw.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "agent.skill.nacos")
public class NacosSkillProperties {

    private String namespaceId;

    private String version;

    private String label;

    private List<String> knownSkillNames = List.of("test-skill","read-skill");
}
