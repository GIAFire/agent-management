package com.zw.agent.tools.testCustomer;

import com.zw.agent.tools.ToolSchemaUtils;
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

@Component
public class HttpGetTool extends AbstractHttpTool {

    public HttpGetTool() {
        super(ToolBase.builder()
                .name("http_get")
                .description("Send HTTP GET request to specified URL")
                .inputSchema(inputSchema()));
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        try {
            String url = ToolSchemaUtils.requiredString(param, "url");
            String headers = ToolSchemaUtils.optionalString(param, "headers");
            log.info("HTTP GET Request: {}", url);

            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            log.info("HTTP GET Response: status={}", response.getStatusCode());
            return Mono.just(ToolResultBlock.text(Result.ok(response.getBody()).toString()));
        } catch (Exception ex) {
            log.error("HTTP GET request failed", ex);
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
