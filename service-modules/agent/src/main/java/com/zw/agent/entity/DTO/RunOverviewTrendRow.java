package com.zw.agent.entity.DTO;

import java.time.LocalDate;
import lombok.Data;

@Data
public class RunOverviewTrendRow {

    private LocalDate runDate;
    private Long runCount;
    private Long successCount;
}
