package com.zw.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zw.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("ag_agent")
public class AgentEntity extends BaseEntity {
    private Long id;
    private String agentName;
    private Integer status;
}
