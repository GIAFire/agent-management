package com.zw.agent.entity.DTO;

import lombok.Data;

@Data
public class ModelHeaderInput {
    private Long id;
    private String headerName;
    private String headerValue;
    private Boolean remove;
}
