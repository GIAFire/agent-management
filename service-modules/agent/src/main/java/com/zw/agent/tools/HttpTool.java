package com.zw.agent.tools;



import com.zw.common.entity.Result;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP请求工具
 * 用于调用外部API接口
 *
 * @author huxuehao
 */
@Slf4j
@Component
public class HttpTool{

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @param headers 请求头（JSON格式）
     * @return 响应结果
     */
    @Tool(name = "http_get", description = "Send HTTP GET request to specified URL")
    public Result httpGet(
            @ToolParam(name = "url", description = "Request URL", required = true) String url,
            @ToolParam(name = "headers", description = "Request headers in JSON format", required = false) String headers
    ) {
        try {
            log.info("HTTP GET Request: {}", url);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null && !headers.isEmpty()) {
                // 解析自定义headers
                parseHeaders(headers, httpHeaders);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.info("HTTP GET Response: status={}", response.getStatusCode());
            return Result.ok(response.getBody());
        } catch (Exception e) {
            log.error("HTTP GET request failed: {}", url, e);
            return Result.fail("HTTP请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param body 请求体（JSON格式）
     * @param headers 请求头（JSON格式）
     * @return 响应结果
     */
    @Tool(name = "http_post", description = "Send HTTP POST request with JSON body")
    public Result httpPost(
            @ToolParam(name = "url", description = "Request URL", required = true) String url,
            @ToolParam(name = "body", description = "Request body in JSON format", required = false) String body,
            @ToolParam(name = "headers", description = "Request headers in JSON format", required = false) String headers
    ) {
        try {
            log.info("HTTP POST Request: {}", url);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null && !headers.isEmpty()) {
                parseHeaders(headers, httpHeaders);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.info("HTTP POST Response: status={}", response.getStatusCode());
            return Result.ok(response.getBody());
        } catch (Exception e) {
            log.error("HTTP POST request failed: {}", url, e);
            return Result.fail("HTTP请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送PUT请求
     *
     * @param url 请求URL
     * @param body 请求体（JSON格式）
     * @param headers 请求头（JSON格式）
     * @return 响应结果
     */
    @Tool(name = "http_put", description = "Send HTTP PUT request with JSON body")
    public Result httpPut(
            @ToolParam(name = "url", description = "Request URL", required = true) String url,
            @ToolParam(name = "body", description = "Request body in JSON format", required = false) String body,
            @ToolParam(name = "headers", description = "Request headers in JSON format", required = false) String headers
    ) {
        try {
            log.info("HTTP PUT Request: {}", url);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null && !headers.isEmpty()) {
                parseHeaders(headers, httpHeaders);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(body, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            
            log.info("HTTP PUT Response: status={}", response.getStatusCode());
            return Result.ok(response.getBody());
        } catch (Exception e) {
            log.error("HTTP PUT request failed: {}", url, e);
            return Result.fail("HTTP请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送DELETE请求
     *
     * @param url 请求URL
     * @param headers 请求头（JSON格式）
     * @return 响应结果
     */
    @Tool(name = "http_delete", description = "Send HTTP DELETE request")
    public Result httpDelete(
            @ToolParam(name = "url", description = "Request URL", required = true) String url,
            @ToolParam(name = "headers", description = "Request headers in JSON format", required = false) String headers
    ) {
        try {
            log.info("HTTP DELETE Request: {}", url);
            
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            
            if (headers != null && !headers.isEmpty()) {
                parseHeaders(headers, httpHeaders);
            }
            
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            
            log.info("HTTP DELETE Response: status={}", response.getStatusCode());
            return Result.ok(response.getBody());
        } catch (Exception e) {
            log.error("HTTP DELETE request failed: {}", url, e);
            return Result.fail("HTTP请求失败: " + e.getMessage());
        }
    }

    /**
     * 解析请求头
     */
    private void parseHeaders(String headersJson, HttpHeaders httpHeaders) {
        try {
            // 简单解析JSON格式的headers
            // 实际项目中建议使用Jackson或其他JSON库
            if (headersJson.startsWith("{") && headersJson.endsWith("}")) {
                String content = headersJson.substring(1, headersJson.length() - 1);
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":");
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        httpHeaders.set(key, value);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse headers: {}", headersJson, e);
        }
    }
}
