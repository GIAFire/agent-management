package com.zw.agent.entity.DTO;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ModelTrendRow {
    private LocalDate callDate;
    private Long callCount;
}
