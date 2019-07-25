package com.airepublic.microprofile.util.http.common;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class AsyncHttpReader {
    private final List<ByteBuffer> requestBuffers = new ArrayList<>();
    private boolean fullyRead = false;
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;


    /**
     * This method will parse the input buffer. If the request is fully read it will return true
     * otherwise false if more data is needed.
     * 
     * @param buffer
     * @return if the request is fully read it will return true otherwise false if more data is
     *         needed
     * @throws IOException if reading from the buffer fails
     */
    public boolean receiveBuffer(final ByteBuffer buffer) throws IOException {
        if (!fullyRead) {
            if (buffer.hasRemaining()) {
                requestBuffers.add(BufferUtil.copyRemainingBuffer(buffer));
            }

            // check if finished receiving incoming buffers
            if (buffer.limit() == buffer.capacity()) {
                fullyRead = false;
            } else {
                fullyRead = true;
            }
        } else {
            throw new IOException("Request has already been fully read. Illegal incoming second request!");
        }

        return fullyRead;
    }


    public boolean isFullyRead() {
        return fullyRead;
    }


    public HttpRequest getHttpRequest() throws IOException {
        if (httpRequest == null) {
            if (fullyRead) {
                final Headers headers = new Headers();
                final String requestLine = parseHeaders(headers);
                final ByteBuffer body = parseBody();
                httpRequest = new HttpRequest(requestLine, headers).withBody(body);
            } else {
                throw new IOException("Illegal state - request has not been fully read!");
            }
        }

        return httpRequest;
    }


    public HttpResponse getHttpResponse() throws IOException {
        if (httpResponse == null) {
            if (fullyRead) {
                final Headers headers = new Headers();
                final String firstLine = parseHeaders(headers);
                final ByteBuffer body = parseBody();
                final HttpStatus status = parseHttpStatus(firstLine);
                httpResponse = new HttpResponse(status, headers, body);
            } else {
                throw new IOException("Illegal state - request has not been fully read!");
            }
        }

        return httpResponse;
    }


    private HttpStatus parseHttpStatus(final String firstLine) {
        final String[] split = firstLine.split(" ");
        return HttpStatus.forCode(Integer.valueOf(split[1]));
    }


    private String parseHeaders(final Headers headers) throws IOException {
        boolean isFinishedHeaders = false;
        boolean isFirstLine = true;
        String firstLine = null;

        for (final ByteBuffer buffer : requestBuffers) {
            if (!isFinishedHeaders) {
                while (buffer.hasRemaining() && !isFinishedHeaders) {

                    String line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));

                    if (line != null) {
                        line = line.strip();

                        // check if line is empty
                        if (line.isBlank()) {
                            isFinishedHeaders = true;

                            while (line != null && line.isBlank()) {
                                line = BufferUtil.readLine(buffer, Charset.forName("ASCII"));
                            }

                            break;
                        } else {
                            if (isFirstLine) {
                                firstLine = line;
                                isFirstLine = false;
                            } else {
                                final int idx = line.indexOf(':');

                                if (idx != -1) {
                                    final String value = line.substring(idx + 1);
                                    headers.add(line.substring(0, idx), value.strip());
                                } else if (idx == -1) {
                                    throw new IOException("Could not parse header information: " + line);
                                }
                            }
                        }
                    } else {
                        throw new IOException("Could not read header line (null)!");
                    }
                }

                if (isFinishedHeaders) {
                    break;
                }
            }
        }

        return firstLine;
    }


    private ByteBuffer parseBody() throws IOException {
        final ByteBuffer body = BufferUtil.combineBuffers(requestBuffers);
        body.position(0);
        return body;
    }


    public void clear() {
        requestBuffers.clear();
        httpRequest = null;
        httpResponse = null;
        fullyRead = false;
    }
}
