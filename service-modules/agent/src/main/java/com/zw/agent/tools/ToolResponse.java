package com.zw.agent.tools;

import lombok.Data;
import org.icepear.echarts.Bar;
import org.icepear.echarts.Option;

@Data
public class ToolResponse {
    Long count;
    Object data;
    String optionTemplate;
    String toLLMMessage;
}
