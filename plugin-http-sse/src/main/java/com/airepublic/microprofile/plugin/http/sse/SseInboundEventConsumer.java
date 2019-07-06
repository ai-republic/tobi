package com.airepublic.microprofile.plugin.http.sse;

import java.util.function.Consumer;

import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

/**
 * Helper class to register {@link SseEventSource} consumers.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SseInboundEventConsumer {
    private Consumer<InboundSseEvent> onEvent;
    private Consumer<Throwable> onError;
    private Runnable onComplete;


    public SseInboundEventConsumer(final Consumer<InboundSseEvent> onEvent) {
        this(onEvent, null, null);
    }


    public SseInboundEventConsumer(final Consumer<InboundSseEvent> onEvent, final Consumer<Throwable> onError) {
        this(onEvent, onError, null);
    }


    public SseInboundEventConsumer(final Consumer<InboundSseEvent> onEvent, final Consumer<Throwable> onError, final Runnable onComplete) {
        this.onEvent = onEvent;
        this.onError = onError;
        this.onComplete = onComplete;
    }


    public Consumer<InboundSseEvent> getOnEvent() {
        return onEvent;
    }


    public void setOnEvent(final Consumer<InboundSseEvent> onEvent) {
        this.onEvent = onEvent;
    }


    public Consumer<Throwable> getOnError() {
        return onError;
    }


    public void setOnError(final Consumer<Throwable> onError) {
        this.onError = onError;
    }


    public Runnable getOnComplete() {
        return onComplete;
    }


    public void setOnComplete(final Runnable onComplete) {
        this.onComplete = onComplete;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (onComplete == null ? 0 : onComplete.hashCode());
        result = prime * result + (onError == null ? 0 : onError.hashCode());
        result = prime * result + (onEvent == null ? 0 : onEvent.hashCode());
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
        final SseInboundEventConsumer other = (SseInboundEventConsumer) obj;
        if (onComplete == null) {
            if (other.onComplete != null) {
                return false;
            }
        } else if (!onComplete.equals(other.onComplete)) {
            return false;
        }
        if (onError == null) {
            if (other.onError != null) {
                return false;
            }
        } else if (!onError.equals(other.onError)) {
            return false;
        }
        if (onEvent == null) {
            if (other.onEvent != null) {
                return false;
            }
        } else if (!onEvent.equals(other.onEvent)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "SseInboundEventConsumer [onEvent=" + onEvent + ", onError=" + onError + ", onComplete=" + onComplete + "]";
    }
}
