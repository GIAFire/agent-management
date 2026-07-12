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
public class HttpPutTool extends AbstractHttpTool {

    public HttpPutTool() {
        super(ToolBase.builder()
                .name("http_put")
                .description("Send HTTP PUT request with JSON body")
                .inputSchema(inputSchema()));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String url = ToolSchemaUtils.requiredString(param, "url");
            String body = ToolSchemaUtils.optionalString(param, "body");
            String headers = ToolSchemaUtils.optionalString(param, "headers");
            log.info("HTTP PUT Request: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(body, buildHeaders(headers));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            log.info("HTTP PUT Response: status={}", response.getStatusCode());
            return Mono.just(ToolResultBlock.text(Result.ok(response.getBody()).toString()));
        } catch (Exception ex) {
            log.error("HTTP PUT request failed", ex);
            return Mono.just(ToolResultBlock.text(Result.fail("HTTP请求失败: " + ex.getMessage()).toString()));
        }
    }

    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("url", ToolSchemaUtils.stringProperty("Request URL"));
        properties.put("body", ToolSchemaUtils.stringProperty("Request body in JSON format"));
        properties.put("headers", ToolSchemaUtils.stringProperty("Request headers in JSON format"));
        return ToolSchemaUtils.objectSchema(properties, List.of("url"));
    }
}
