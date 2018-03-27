package org.nnc.gateway.sigma;

import org.apache.commons.io.IOUtils;
import org.nnc.gateway.RequestDto;
import org.nnc.gateway.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Controller
public class ProxyController {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyController.class);

    @Autowired
    private final FileSystemGateway fileSystemGateway;

    @Autowired
    public ProxyController(FileSystemGateway fileSystemGateway) {
        this.fileSystemGateway = fileSystemGateway;
    }

    @RequestMapping("**")
    @ResponseBody
    public void request(HttpServletRequest request,
                        HttpServletResponse response) {
        final String name = Thread.currentThread().getName() + "-" + UUID.randomUUID();
        LOG.info(name + " start");
        try {
            final RequestDto requestDto = getRequest(request);
            LOG.info("REQUEST method: " + requestDto.getMethod());
            LOG.info("REQUEST path: " + requestDto.getPath());
            LOG.info("REQUEST headers: " + requestDto.getHeaders());
            LOG.info("REQUEST body: " + new String(requestDto.getBody(), StandardCharsets.UTF_8));

            final ResponseDto responseDto = fileSystemGateway.getResponse(name, requestDto);
            LOG.info("RESPONSE headers: " + responseDto.getHeaders());
            LOG.info("RESPONSE body: " + new String(responseDto.getBody(), StandardCharsets.UTF_8));
            setResponse(response, responseDto);
        } catch (Exception e) {
            LOG.error(name + " error", e);
        }
    }

    private static void setResponse(final HttpServletResponse response, final ResponseDto dto) throws IOException {
        for (final Map.Entry<String, String> e : dto.getHeaders().entrySet()) {
                response.setHeader(e.getKey(), e.getValue());
        }

        try (final ServletOutputStream out = response.getOutputStream()) {
            IOUtils.write(dto.getBody(), out);
            out.flush();
        }
    }

    private static RequestDto getRequest(final HttpServletRequest request) throws IOException {
        final RequestDto dto = new RequestDto();

        dto.setMethod(request.getMethod());

        dto.setPath(request.getRequestURI() + preparedQueryString(request.getQueryString()));

        dto.setHeaders(new HashMap<>());
        for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements(); ) {
            final String name = e.nextElement();
            final String value = request.getHeader(name);
            dto.getHeaders().put(name, value);
        }

        dto.setBody(IOUtils.toByteArray(request.getInputStream()));

        return dto;
    }

    private static String preparedQueryString(final String query) {
        return isNotBlank(query) ? "?" + query : "";
    }
}
