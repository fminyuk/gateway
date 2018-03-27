package org.nnc.gateway.alpha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProxyProperties {
    private final String url;

    public ProxyProperties(
            @Value("${proxy.url}") String url
    ) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
