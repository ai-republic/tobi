package com.airepublic.microprofile.plugin.http.sse.api;

import java.util.Objects;

public class SseEvent {
    public static final long RETRY_NOT_SET = -1;
    private String id;
    private String name;
    private String comment;
    private String data;
    private long retry = RETRY_NOT_SET;

    public static class Builder {
        private final SseEvent sseEvent = new SseEvent();


        public Builder withId(final String id) {
            sseEvent.setId(id);
            return this;
        }


        public Builder withName(final String name) {
            sseEvent.setName(name);
            return this;
        }


        public Builder withComment(final String comment) {
            sseEvent.setComment(comment);
            return this;
        }


        public Builder withRetry(final long retryDelay) {
            sseEvent.setRetry(retryDelay);
            return this;
        }


        public Builder withData(final String data) {
            sseEvent.setData(data);
            return this;
        }


        public SseEvent build() {
            return sseEvent;
        }
    }


    public SseEvent() {
    }


    public String getId() {
        return id;
    }


    public void setId(final String id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public void setName(final String name) {
        this.name = name;
    }


    public String getComment() {
        return comment;
    }


    public void setComment(final String comment) {
        this.comment = comment;
    }


    public String getData() {
        return data;
    }


    public void setData(final String data) {
        this.data = data;
    }


    public long getRetry() {
        return retry;
    }


    public void setRetry(final long reconnectDelay) {
        retry = reconnectDelay;
    }


    public boolean isReconnectDelaySet() {
        return retry != -1L;
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
        final SseEvent other = (SseEvent) obj;
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }


    @Override
    public String toString() {
        return "SseEvent [id=" + id + ", name=" + name + ", comment=" + comment + ", data=" + data + ", retry=" + retry + "]";
    }
}
