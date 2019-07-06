package com.airepublic.microprofile.plugin.http.sse;

import javax.ws.rs.sse.SseEvent;

public class SseEventImpl implements SseEvent {
    private String id;
    private String name;
    private String comment;
    private long reconnectDelay = RECONNECT_NOT_SET;


    @Override
    public String getId() {
        return id;
    }


    void setId(final String id) {
        this.id = id;
    }


    @Override
    public String getName() {
        return name;
    }


    void setName(final String name) {
        this.name = name;
    }


    @Override
    public String getComment() {
        return comment;
    }


    void setComment(final String comment) {
        this.comment = comment;
    }


    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }


    void setReconnectDelay(final long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }


    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay != -1L;
    }

}
