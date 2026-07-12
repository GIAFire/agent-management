package com.zw.agent.tools.testCustomer;

import com.zw.agent.tools.applicationRunner.Tenant;
import io.agentscope.core.tool.ToolBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Tenant("1")
abstract class AbstractHttpTool extends ToolBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final RestTemplate restTemplate = new RestTemplate();

    protected AbstractHttpTool(ToolBase.Builder builder) {
        super(builder);
    }

    protected HttpHeaders buildHeaders(String headersJson) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if (headersJson == null || headersJson.isBlank()) {
            return httpHeaders;
        }

        try {
            if (headersJson.startsWith("{") && headersJson.endsWith("}")) {
                String content = headersJson.substring(1, headersJson.length() - 1);
                String[] pairs = content.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split(":", 2);
                    if (kv.length == 2) {
                        String key = kv[0].trim().replace("\"", "");
                        String value = kv[1].trim().replace("\"", "");
                        httpHeaders.set(key, value);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to parse headers: {}", headersJson, ex);
        }
        return httpHeaders;
    }
}
