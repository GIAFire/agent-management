package com.zw.agent.tools.testCustomer;

import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.Tenant;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tenant("1")
@Component
public class SimpleTools extends ToolBase {

    public SimpleTools() {
        super(ToolBase.builder()
                .name("get_current_time")
                .description("Returns the current time in a given IANA timezone.")
                .inputSchema(inputSchema())
                .readOnly(true)
                .concurrencySafe(true));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String timezone = ToolSchemaUtils.requiredString(param, "timezone");
            String currentTime = LocalDateTime.now(ZoneId.of(timezone))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Mono.just(ToolResultBlock.text(currentTime));
        } catch (Exception ex) {
            return Mono.just(ToolResultBlock.error(ex.getMessage()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("timezone", ToolSchemaUtils.stringProperty("IANA timezone, e.g. Asia/Shanghai"));
        return ToolSchemaUtils.objectSchema(properties, List.of("timezone"));
    }
}
