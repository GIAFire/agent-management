package com.zhiran.agent.entity.DTO;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ModelTrendRow {
    private LocalDate callDate;
    private Long callCount;
}
