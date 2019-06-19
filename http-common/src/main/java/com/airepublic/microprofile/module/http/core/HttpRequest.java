package com.airepublic.microprofile.module.http.core;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.Principal;

public class HttpRequest {
    private String requestLine;
    private final Headers headers;
    private ByteBuffer body;
    private URI uri;


    public HttpRequest(final String requestLine, final Headers headers) {
        this(requestLine, headers, null);
    }


    public HttpRequest(final String requestLine, final Headers headers, final ByteBuffer body) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.body = body;
    }


    public Headers getHeaders() {
        return headers;
    }


    public String getRequestLine() {
        return requestLine;
    }


    public void setRequestLine(final String requestLine) {
        this.requestLine = requestLine;
    }


    public ByteBuffer getBody() {
        return body;
    }


    public void setBody(final ByteBuffer body) {
        this.body = body;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (body == null ? 0 : body.hashCode());
        result = prime * result + (headers == null ? 0 : headers.hashCode());
        result = prime * result + (requestLine == null ? 0 : requestLine.hashCode());
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
        final HttpRequest other = (HttpRequest) obj;
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
        if (requestLine == null) {
            if (other.requestLine != null) {
                return false;
            }
        } else if (!requestLine.equals(other.requestLine)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "HttpRequest [requestLine=" + requestLine + ", headers=" + headers + ", body=" + body + "]";
    }


    public URI getUri() throws URISyntaxException {
        if (uri == null) {
            if (requestLine == null) {
                new URISyntaxException(requestLine, "is null");
            }

            uri = new URI(getScheme() + "://" + getHost() + ":" + getPort() + getPath() + (getQuery().isBlank() ? "" : "?" + getQuery()));
        }

        return uri;
    }


    public void setUri(final URI uri) {
        this.uri = uri;
    }


    public String getMethod() {
        if (requestLine == null) {
            new URISyntaxException(requestLine, "is null");
        }

        final int idx = requestLine.indexOf(" ");

        if (idx != -1) {
            return requestLine.substring(0, idx);
        }

        throw new IllegalArgumentException("No request line present: " + requestLine);
    }


    public String getHost() {
        final String hosts = headers.getFirst(Headers.HOST);
        final String[] hostport = hosts.split(":");
        return hostport[0];
    }


    public int getPort() {
        final String hosts = headers.getFirst(Headers.HOST);
        final String[] hostport = hosts.split(":");

        if (hostport.length > 1) {
            return Integer.parseInt(hostport[1].strip());
        } else {
            return 80;
        }
    }


    public String getPath() {
        final String[] requestParts = requestLine.split(" ");
        final String[] requestQuery = requestParts[1].split("\\?");

        if (requestQuery.length > 0) {
            return requestQuery[0];
        } else {
            return "";
        }
    }


    public String getQuery() {
        final String[] requestParts = requestLine.split(" ");
        final String[] requestQuery = requestParts[1].split("\\?");

        if (requestQuery.length > 1) {
            return requestQuery[1];
        } else {
            return "";
        }
    }


    public String getScheme() {
        final String[] requestParts = requestLine.split(" ");
        final String[] requestQuery = requestParts[2].split("/");

        if (requestQuery.length > 1) {
            return requestQuery[0].toLowerCase();
        } else {
            return "http";
        }
    }


    public boolean isSecure() {
        return getScheme().equals("https") || getScheme().equals("wss");
    }


    public Principal getUserPrincipal() {
        // TODO implement user handling
        return null;
    }

}
