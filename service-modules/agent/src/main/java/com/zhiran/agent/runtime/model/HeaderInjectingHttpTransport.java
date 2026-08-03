package com.zhiran.agent.runtime.model;

import io.agentscope.core.model.transport.HttpRequest;
import io.agentscope.core.model.transport.HttpResponse;
import io.agentscope.core.model.transport.HttpTransport;
import io.agentscope.core.model.transport.HttpTransportException;
import java.util.LinkedHashMap;
import java.util.Map;
import reactor.core.publisher.Flux;

public final class HeaderInjectingHttpTransport implements HttpTransport {

    private final HttpTransport delegate;
    private final Map<String, String> headers;

    public HeaderInjectingHttpTransport(
            HttpTransport delegate,
            Map<String, String> headers
    ) {
        this.delegate = delegate;
        this.headers = Map.copyOf(headers);
    }

    @Override
    public HttpResponse execute(HttpRequest request) throws HttpTransportException {
        return delegate.execute(withHeaders(request));
    }

    @Override
    public Flux<String> stream(HttpRequest request) {
        return delegate.stream(withHeaders(request));
    }

    @Override
    public void close() {
        // The delegate is AgentScope's shared transport and is closed by its factory.
    }

    private HttpRequest withHeaders(HttpRequest request) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (request.getHeaders() != null) {
            merged.putAll(request.getHeaders());
        }
        merged.putAll(headers);
        return HttpRequest.builder()
                .url(request.getUrl())
                .method(request.getMethod())
                .headers(merged)
                .body(request.getBody())
                .build();
    }
}
