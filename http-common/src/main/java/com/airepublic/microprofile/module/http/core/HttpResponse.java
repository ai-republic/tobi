package com.airepublic.microprofile.module.http.core;

import java.nio.ByteBuffer;

public class HttpResponse {
    private final String protocol = "HTTP/1.1";
    private HttpStatus status;
    private Headers headers;
    private ByteBuffer body;


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


    public HttpStatus getStatus() {
        return status;
    }


    public void setStatus(final HttpStatus status) {
        this.status = status;
    }


    public Headers getHeaders() {
        return headers;
    }


    public void setHeaders(final Headers headers) {
        this.headers = headers;
    }


    public ByteBuffer getBody() {
        return body;
    }


    public void setBody(final ByteBuffer body) {
        this.body = body;
    }


    public ByteBuffer getHeaderBuffer() {
        final StringBuffer str = new StringBuffer();
        str.append(protocol + " " + status.code() + " " + status.name() + "\r\n");

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
