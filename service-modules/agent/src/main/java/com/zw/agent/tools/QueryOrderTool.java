package com.zw.agent.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QueryOrderTool extends ToolBase {

    public QueryOrderTool() {
        super(ToolBase.builder()
                .name("query_order")
                .description("查询当前租户下的订单信息")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String orderNo = ToolSchemaUtils.requiredString(param, "orderNo");
            String tenantId = ToolSchemaUtils.runtimeUserId(param);
            return Mono.just(ToolResultBlock.text("tenant=" + tenantId + ", orderNo=" + orderNo));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("orderNo", ToolSchemaUtils.stringProperty("订单号"));
        return ToolSchemaUtils.objectSchema(properties, List.of("orderNo"));
    }
}
