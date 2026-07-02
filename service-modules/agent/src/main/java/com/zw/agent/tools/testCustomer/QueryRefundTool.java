package com.zw.agent.tools.testCustomer;

import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.permission;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@permission("order:list")
public class QueryRefundTool extends ToolBase {

    public QueryRefundTool() {
        super(ToolBase.builder()
                .name("query_refund")
                .description("查询当前租户下的退款信息")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String refundNo = ToolSchemaUtils.requiredString(param, "refundNo");
            String tenantId = ToolSchemaUtils.runtimeUserId(param);
            return Mono.just(ToolResultBlock.text("tenant=" + tenantId + ", refundNo=" + refundNo));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("refundNo", ToolSchemaUtils.stringProperty("退款单号"));
        return ToolSchemaUtils.objectSchema(properties, List.of("refundNo"));
    }
}
