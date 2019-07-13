package com.airepublic.microprofile.plugin.http.sse;

import java.util.Objects;

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


    public void setId(final String id) {
        this.id = id;
    }


    @Override
    public String getName() {
        return name;
    }


    public void setName(final String name) {
        this.name = name;
    }


    @Override
    public String getComment() {
        return comment;
    }


    public void setComment(final String comment) {
        this.comment = comment;
    }


    @Override
    public long getReconnectDelay() {
        return reconnectDelay;
    }


    public void setReconnectDelay(final long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }


    @Override
    public boolean isReconnectDelaySet() {
        return reconnectDelay != -1L;
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, name);
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
        final SseEventImpl other = (SseEventImpl) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }

}
