package org.nnc.gateway.alpha;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.AbstractHttpMessage;
import org.nnc.gateway.RequestDto;
import org.nnc.gateway.ResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class ProxyComponent {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyComponent.class);

    // Какие-то хедеры всё же придётся вычистить руками. Возможно этот список придётся расширить
    private final static Set<String> remove = new HashSet<>(Arrays.asList(
            "host",             // "Host" - может вызвать проблемы, лучше выкинуть.
            "content-length"    // "Content-Length" - в виду особенности InputStreamEntity из хидера надо убрать длинну. она проставится позже автоматически.
    ));

    private final ProxyProperties properties;

    @Autowired
    public ProxyComponent(ProxyProperties properties) {
        this.properties = properties;
    }

    public ResponseDto getResponse(RequestDto requestDto) throws IOException {
        LOG.info("REQUEST method: " + requestDto.getMethod());
        LOG.info("REQUEST path: " + requestDto.getPath());
        LOG.info("REQUEST headers: " + requestDto.getHeaders());
        LOG.info("REQUEST body: " + new String(requestDto.getBody(), StandardCharsets.UTF_8));

        final HttpUriRequest request = getRequest(requestDto);

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            final HttpResponse response = httpClient.execute(request);

            final ResponseDto responseDto = getResponseDto(response);
            LOG.info("RESPONSE headers: " + responseDto.getHeaders());
            LOG.info("RESPONSE body: " + new String(responseDto.getBody(), StandardCharsets.UTF_8));

            return responseDto;
        }
    }

    private HttpUriRequest getRequest(final RequestDto dto) {
        final String url = properties.getUrl() + dto.getPath();

        if ("GET".equals(dto.getMethod())) {
            final HttpGet request = new HttpGet(url);
            setHeaders(dto.getHeaders(), request);
            return request;
        }

        if ("POST".equals(dto.getMethod())) {
            final HttpPost request = new HttpPost(url);
            request.setEntity(new ByteArrayEntity(dto.getBody()));
            setHeaders(dto.getHeaders(), request);
            return request;
        }

        return null;
    }

    private static ResponseDto getResponseDto(final HttpResponse response) throws IOException {
        final ResponseDto dto = new ResponseDto();

        dto.setHeaders(new HashMap<>());
        final Header[] headers = response.getAllHeaders();
        for (Header h : headers) {
            dto.getHeaders().put(h.getName(), h.getValue());
        }
        dto.setBody(IOUtils.toByteArray(response.getEntity().getContent()));

        return dto;
    }

    private static void setHeaders(final Map<String, String> headers, final AbstractHttpMessage request) {
        for (final Map.Entry<String, String> e : headers.entrySet()) {
            if (!remove.contains(e.getKey().toLowerCase())) {
                request.setHeader(e.getKey(), e.getValue());
            }
        }
    }
}
