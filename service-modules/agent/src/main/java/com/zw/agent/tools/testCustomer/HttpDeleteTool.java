package com.zw.agent.tools.testCustomer;

import com.zw.agent.tools.ToolSchemaUtils;
import com.zw.agent.tools.applicationRunner.Tenant;
import com.zw.common.entity.Result;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tenant("1")
@Component
public class HttpDeleteTool extends AbstractHttpTool {

    public HttpDeleteTool() {
        super(ToolBase.builder()
                .name("http_delete")
                .description("Send HTTP DELETE request")
                .inputSchema(inputSchema()));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String url = ToolSchemaUtils.requiredString(param, "url");
            String headers = ToolSchemaUtils.optionalString(param, "headers");
            log.info("HTTP DELETE Request: {}", url);

            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

            log.info("HTTP DELETE Response: status={}", response.getStatusCode());
            return Mono.just(ToolResultBlock.text(Result.ok(response.getBody()).toString()));
        } catch (Exception ex) {
            log.error("HTTP DELETE request failed", ex);
            return Mono.just(ToolResultBlock.text(Result.fail("HTTP请求失败: " + ex.getMessage()).toString()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("url", ToolSchemaUtils.stringProperty("Request URL"));
        properties.put("headers", ToolSchemaUtils.stringProperty("Request headers in JSON format"));
        return ToolSchemaUtils.objectSchema(properties, List.of("url"));
    }
}
