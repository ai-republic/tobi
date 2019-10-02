package com.airepublic.tobi.core.spi;

import javax.inject.Inject;

/**
 * The {@link ContextActivator} is used to activate the session scoped context if coming from
 * another thread.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ContextActivator {
    @Inject
    private IServerContext serverContext;
    @Inject
    private RequestScopedContext requestScopedContext;
    @Inject
    private SessionScopedContext sessionScopedContext;

    /**
     * Activate the {@link IServerSession} with the specified session-id.
     * 
     * @param sessionId the session-id
     */
    public void activate(final String sessionId) {
        final BeanContextStorage store = serverContext.getSessionContext(sessionId);
        requestScopedContext.activate(new BeanContextStorage());
        sessionScopedContext.activate(store);
    }
}
