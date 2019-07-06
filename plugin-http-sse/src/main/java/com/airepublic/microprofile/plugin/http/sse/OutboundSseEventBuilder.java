package com.airepublic.microprofile.plugin.http.sse;

import java.lang.reflect.Type;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;

public class OutboundSseEventBuilder implements Builder {
    private String id;
    private String name;
    private long reconnectDelay = -1L;
    private MediaType mediaType;
    private String comment;
    private Object data;
    private Class<?> type;
    private Type genericType;


    @Override
    public Builder id(final String id) {
        this.id = id;

        return this;
    }


    @Override
    public Builder name(final String name) {
        this.name = name;

        return this;
    }


    @Override
    public Builder reconnectDelay(final long milliseconds) {
        reconnectDelay = milliseconds;

        return this;
    }


    @Override
    public Builder mediaType(final MediaType mediaType) {
        this.mediaType = mediaType;

        return this;
    }


    @Override
    public Builder comment(final String comment) {
        this.comment = comment;

        return this;
    }


    @Override
    public Builder data(final Class type, final Object data) {
        this.type = type;
        this.data = data;

        return this;
    }


    @Override
    public Builder data(final GenericType genericType, final Object data) {
        this.genericType = genericType.getType();
        this.data = data;
        return this;
    }


    @Override
    public Builder data(final Object data) {
        this.data = data;

        return this;
    }


    @Override
    public OutboundSseEvent build() {
        final OutboundSseEventImpl event = new OutboundSseEventImpl();
        event.setComment(comment);
        event.setData(data);
        event.setGenericType(genericType);
        event.setId(id);
        event.setMediaType(mediaType);
        event.setName(name);
        event.setReconnectDelay(reconnectDelay);
        event.setType(type);

        return event;
    }

}
