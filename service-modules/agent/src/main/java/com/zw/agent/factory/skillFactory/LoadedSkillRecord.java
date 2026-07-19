package com.zw.agent.factory.skillFactory;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
public class LoadedSkillRecord {
    private String name;
    private String description;
    private String skillContent;
    private String source;
    private String metadataJson;
    private Map<String, String> resources = new HashMap<>();

    public LoadedSkillRecord(String name, String description, String skillContent, String source, String metadataJson) {
        this.name = name;
        this.description = description;
        this.skillContent = skillContent;
        this.source = source;
        this.metadataJson = metadataJson;
    }

}
