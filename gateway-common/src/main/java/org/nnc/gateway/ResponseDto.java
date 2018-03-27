package org.nnc.gateway;

import java.io.Serializable;
import java.util.Map;

public class ResponseDto implements Serializable {
    private Map<String, String> headers;
    private byte[] body;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
