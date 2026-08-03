package com.zhiran.agent.entity.DTO;

import java.time.LocalDate;
import lombok.Data;

@Data
public class RunOverviewTrendRow {

    private LocalDate runDate;
    private Long runCount;
    private Long successCount;
}
