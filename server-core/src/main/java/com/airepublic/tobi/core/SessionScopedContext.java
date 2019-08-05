package com.airepublic.tobi.core;

import java.lang.annotation.Annotation;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.airepublic.tobi.core.spi.SessionContext;

public class SessionScopedContext implements AlterableContext {
    private final static ThreadLocal<SessionContext> sessionContext = new ThreadLocal<>();


    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        validateSession();

        final T bean = contextual.create(creationalContext);
        sessionContext.get().addBean((Contextual<Object>) contextual, (CreationalContext<Object>) creationalContext, bean);
        creationalContext.push(bean);

        return bean;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Contextual<T> contextual) {
        validateSession();
        return (T) sessionContext.get().getBean((Contextual<Object>) contextual);
    }


    public void activate(final SessionContext sessionContext) {
        SessionScopedContext.sessionContext.set(sessionContext);
        validateSession();
    }


    public void deactivate() {
        validateSession();

        sessionContext.get().destroy();
        sessionContext.remove();
    }


    @Override
    public boolean isActive() {
        return sessionContext.get() != null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void destroy(final Contextual<?> contextual) {
        validateSession();

        sessionContext.get().destroy((Contextual<Object>) contextual);
    }


    private void validateSession() {
        if (sessionContext.get() == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " has not been activated!");
        }
    }

}
