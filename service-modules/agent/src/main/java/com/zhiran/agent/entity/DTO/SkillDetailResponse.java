package com.zhiran.agent.entity.DTO;

import java.util.List;

public record SkillDetailResponse(
        Long id,
        String skillCode,
        String skillName,
        String description,
        String skillContent,
        String category,
        List<String> tags,
        String riskLevel,
        Byte status,
        List<String> roleCodes
) {
}
