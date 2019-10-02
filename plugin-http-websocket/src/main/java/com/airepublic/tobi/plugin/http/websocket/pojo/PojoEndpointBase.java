/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.airepublic.tobi.plugin.http.websocket.pojo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import com.airepublic.logging.java.SerializableLogger;
import com.airepublic.tobi.plugin.http.websocket.util.ExceptionUtils;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;

/**
 * Base implementation (client and server have different concrete implementations) of the wrapper
 * that converts a POJO instance into a WebSocket endpoint instance.
 */
public abstract class PojoEndpointBase extends Endpoint {

    private final Logger log = new SerializableLogger(PojoEndpointBase.class.getName()); // must not
                                                                                         // be
    // static
    private static final StringManager sm = StringManager.getManager(PojoEndpointBase.class);

    private Object pojo;
    private Map<String, String> pathParameters;
    private PojoMethodMapping methodMapping;


    protected final void doOnOpen(final Session session, final EndpointConfig config) {
        final PojoMethodMapping methodMapping = getMethodMapping();
        final Object pojo = getPojo();
        final Map<String, String> pathParameters = getPathParameters();

        // Add message handlers before calling onOpen since that may trigger a
        // message which in turn could trigger a response and/or close the
        // session
        for (final MessageHandler mh : methodMapping.getMessageHandlers(pojo,
                pathParameters, session, config)) {
            session.addMessageHandler(mh);
        }

        if (methodMapping.getOnOpen() != null) {
            try {
                methodMapping.getOnOpen().invoke(pojo,
                        methodMapping.getOnOpenArgs(
                                pathParameters, session, config));

            } catch (final IllegalAccessException e) {
                // Reflection related problems
                log.log(Level.SEVERE, sm.getString("pojoEndpointBase.onOpenFail", pojo.getClass().getName()), e);
                handleOnOpenOrCloseError(session, e);
            } catch (final InvocationTargetException e) {
                final Throwable cause = e.getCause();
                handleOnOpenOrCloseError(session, cause);
            } catch (final Throwable t) {
                handleOnOpenOrCloseError(session, t);
            }
        }
    }


    private void handleOnOpenOrCloseError(final Session session, final Throwable t) {
        // If really fatal - re-throw
        ExceptionUtils.handleThrowable(t);

        // Trigger the error handler and close the session
        onError(session, t);
        try {
            session.close();
        } catch (final IOException ioe) {
            log.log(Level.WARNING, sm.getString("pojoEndpointBase.closeSessionFail"), ioe);
        }
    }


    @Override
    public final void onClose(final Session session, final CloseReason closeReason) {

        if (methodMapping.getOnClose() != null) {
            try {
                methodMapping.getOnClose().invoke(pojo,
                        methodMapping.getOnCloseArgs(pathParameters, session, closeReason));
            } catch (final Throwable t) {
                log.log(Level.SEVERE, sm.getString("pojoEndpointBase.onCloseFail", pojo.getClass().getName()), t);
                handleOnOpenOrCloseError(session, t);
            }
        }

        // Trigger the destroy method for any associated decoders
        final Set<MessageHandler> messageHandlers = session.getMessageHandlers();
        for (final MessageHandler messageHandler : messageHandlers) {
            if (messageHandler instanceof PojoMessageHandlerWholeBase<?>) {
                ((PojoMessageHandlerWholeBase<?>) messageHandler).onClose();
            }
        }
    }


    @Override
    public final void onError(final Session session, final Throwable throwable) {

        if (methodMapping.getOnError() == null) {
            log.log(Level.SEVERE, sm.getString("pojoEndpointBase.onError", pojo.getClass().getName()), throwable);
        } else {
            try {
                methodMapping.getOnError().invoke(pojo, methodMapping.getOnErrorArgs(pathParameters, session, throwable));
            } catch (final Throwable t) {
                ExceptionUtils.handleThrowable(t);
                log.log(Level.SEVERE, sm.getString("pojoEndpointBase.onErrorFail", pojo.getClass().getName()), t);
            }
        }
    }


    protected Object getPojo() {
        return pojo;
    }


    protected void setPojo(final Object pojo) {
        this.pojo = pojo;
    }


    protected Map<String, String> getPathParameters() {
        return pathParameters;
    }


    protected void setPathParameters(final Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }


    protected PojoMethodMapping getMethodMapping() {
        return methodMapping;
    }


    protected void setMethodMapping(final PojoMethodMapping methodMapping) {
        this.methodMapping = methodMapping;
    }
}
