package com.airepublic.microprofile.util.http.common;

import java.nio.ByteBuffer;

public class HttpResponse {
    private String scheme = "http";
    private String version = "1.1";
    private HttpStatus status;
    private Headers headers;
    private ByteBuffer body;


    public HttpResponse() {
    }


    public HttpResponse(final HttpStatus status) {
        this(status, new Headers(), null);
    }


    public HttpResponse(final HttpStatus status, final Headers headers) {
        this(status, headers, null);
    }


    public HttpResponse(final HttpStatus status, final Headers headers, final ByteBuffer body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }


    public String getScheme() {
        return scheme;
    }


    public HttpResponse withScheme(final String scheme) {
        this.scheme = scheme;

        return this;
    }


    public String getVersion() {
        return version;
    }


    public HttpResponse withVersion(final String version) {
        this.version = version;

        return this;
    }


    public HttpStatus getStatus() {
        return status;
    }


    public HttpResponse withStatus(final HttpStatus status) {
        this.status = status;

        return this;
    }


    public Headers getHeaders() {
        return headers;
    }


    public HttpResponse withHeaders(final Headers headers) {
        this.headers = headers;

        return this;
    }


    public ByteBuffer getBody() {
        return body;
    }


    public HttpResponse withBody(final ByteBuffer body) {
        this.body = body;

        return this;
    }


    public ByteBuffer getHeaderBuffer() {
        final StringBuffer str = new StringBuffer();
        str.append(scheme.toUpperCase() + "/" + version + " " + status.code() + " " + status.name() + "\r\n");

        if (headers != null) {
            final StringBuffer headerBuf = headers.entrySet().stream().map(entry -> {
                final StringBuffer buf = new StringBuffer();
                entry.getValue().stream().forEach(value -> buf.append(entry.getKey() + ": " + value + "\r\n"));
                return buf;
            }).collect(StringBuffer::new, StringBuffer::append, StringBuffer::append);

            str.append(headerBuf);
            str.append("\r\n");
        }

        return ByteBuffer.wrap(str.toString().getBytes());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (body == null ? 0 : body.hashCode());
        result = prime * result + (headers == null ? 0 : headers.hashCode());
        result = prime * result + (status == null ? 0 : status.hashCode());
        return result;
    }


    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HttpResponse other = (HttpResponse) obj;
        if (body == null) {
            if (other.body != null) {
                return false;
            }
        } else if (!body.equals(other.body)) {
            return false;
        }
        if (headers == null) {
            if (other.headers != null) {
                return false;
            }
        } else if (!headers.equals(other.headers)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "HttpResponse [status=" + status + ", headers=" + headers + ", body=" + body + "]";
    }

}
