package com.zw.agent.runtime.model;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Map;

public final class AnthropicModelClientConfigurer {

    private static final Field CLIENT_FIELD = clientField();

    private AnthropicModelClientConfigurer() {
    }

    public static AnthropicChatModel configure(
            AnthropicChatModel model,
            String baseUrl,
            String apiKey,
            long timeoutMs,
            Map<String, String> headers
    ) {
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .baseUrl(baseUrl)
                .timeout(Duration.ofMillis(timeoutMs))
                .maxRetries(0);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.apiKey(apiKey);
        }
        headers.forEach(builder::replaceHeaders);
        AnthropicClient client = builder.build();
        try {
            AnthropicClient previous = (AnthropicClient) CLIENT_FIELD.get(model);
            CLIENT_FIELD.set(model, client);
            if (previous != null) {
                try {
                    previous.close();
                } catch (RuntimeException ignored) {
                    // The replacement client is already installed and remains usable.
                }
            }
            return model;
        } catch (IllegalAccessException exception) {
            client.close();
            throw new IllegalStateException(
                    "Unable to configure Anthropic HTTP headers",
                    exception
            );
        }
    }

    private static Field clientField() {
        try {
            Field field = AnthropicChatModel.class.getDeclaredField("client");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
