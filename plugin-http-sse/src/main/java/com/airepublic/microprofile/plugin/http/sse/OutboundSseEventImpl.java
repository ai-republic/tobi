package com.airepublic.microprofile.plugin.http.sse;

import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;

public class OutboundSseEventImpl extends SseEventImpl implements OutboundSseEvent {
    private Object data;
    private Class<?> type;
    private Type genericType;
    private MediaType mediaType;


    @Override
    public Class<?> getType() {
        return type;
    }


    void setType(final Class<?> type) {
        this.type = type;
    }


    @Override
    public Type getGenericType() {
        return genericType;
    }


    void setGenericType(final Type genericType) {
        this.genericType = genericType;
    }


    @Override
    public MediaType getMediaType() {
        return mediaType;
    }


    void setMediaType(final MediaType mediaType) {
        this.mediaType = mediaType;
    }


    @Override
    public Object getData() {
        return data;
    }


    void setData(final Object data) {
        this.data = data;
    }
}
