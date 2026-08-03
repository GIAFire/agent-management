package com.zhiran.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

@Component
public class SkillContentMetadataParser {

    private static final Pattern FRONT_MATTER = Pattern.compile(
            "\\A\\uFEFF?---[ \\t]*\\R([\\s\\S]*?)\\R---[ \\t]*(?:\\R|\\z)"
    );
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Yaml yaml;

    public SkillContentMetadataParser() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(10);
        options.setCodePointLimit(1024 * 1024);
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    public SkillContentMetadata parseRequired(String skillContent) {
        if (!StringUtils.hasText(skillContent)) {
            throw new IllegalArgumentException("SKILL.md content must not be blank");
        }
        Matcher matcher = FRONT_MATTER.matcher(skillContent);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "SKILL.md must start with YAML metadata delimited by ---"
            );
        }
        Object parsed;
        try {
            parsed = yaml.load(matcher.group(1));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("SKILL.md metadata is not valid YAML", exception);
        }
        if (!(parsed instanceof Map<?, ?> metadata)) {
            throw new IllegalArgumentException("SKILL.md metadata must be a YAML object");
        }
        String name = requiredScalar(metadata.get("name"), "name");
        String description = requiredScalar(metadata.get("description"), "description");
        return new SkillContentMetadata(name, description);
    }

    public String createInitialContent(
            String skillCode,
            String description,
            String displayName
    ) {
        return "---\n"
                + "name: " + yamlString(skillCode) + "\n"
                + "description: " + yamlString(description) + "\n"
                + "---\n\n"
                + "# " + displayName + "\n";
    }

    private String requiredScalar(Object value, String field) {
        if (value instanceof Map<?, ?> || value instanceof Iterable<?>) {
            throw new IllegalArgumentException(
                    "SKILL.md metadata." + field + " must be a scalar value"
            );
        }
        String text = value == null ? null : String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(
                    "SKILL.md metadata." + field + " must not be blank"
            );
        }
        return text;
    }

    private String yamlString(String value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? "" : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to build SKILL.md metadata", exception);
        }
    }

    public record SkillContentMetadata(String name, String description) {
    }
}
