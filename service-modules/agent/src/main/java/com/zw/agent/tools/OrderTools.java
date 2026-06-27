package com.zw.agent.tools;

import com.zw.agent.tools.applicationRunner.permission;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderTools {

    @Tool(
            name = "query_order",
            description = "查询当前租户下的订单信息",
            readOnly = true,
            concurrencySafe = true
    )
    public String queryOrder(
            @ToolParam(name = "orderNo", description = "订单号")
            String orderNo,
            // 不加 @ToolParam，表示这个参数不是模型传入，而是运行时注入
            String tenantId
    ) {
        return "tenant=" + tenantId + ", orderNo=" + orderNo;
    }

    @permission("order:list")
    @Tool(
            name = "query_refund",
            description = "查询当前租户下的退款信息",
            readOnly = true,
            concurrencySafe = true
    )
    public String queryRefund(
            @ToolParam(name = "refundNo", description = "退款单号")
            String refundNo,
            String tenantId
    ) {
        return "tenant=" + tenantId + ", refundNo=" + refundNo;
    }
}