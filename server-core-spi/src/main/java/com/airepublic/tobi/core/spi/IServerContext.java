package com.airepublic.tobi.core.spi;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * The context storage for the server information such as host, sessions and custom application
 * attributes.
 * 
 * @author Torsten Oltmanns
 *
 */
public interface IServerContext {

    /**
     * Sets a key/value pair.
     * 
     * @param key the key
     * @param value the value
     */
    void setAttribute(String key, Object value);


    /**
     * Gets the value for the specified key or null if the key or value does not exist
     * 
     * @param key the key
     * @return the value or null
     */
    Object getAttribute(String key);


    /**
     * Gets the value for the specified key of the specified type.
     * 
     * @param key the key
     * @param type the type of the value
     * @return the value or null if it does not exist
     */
    <T> T getAttribute(final String key, final Class<T> type);


    /**
     * Checks whether the key exists.
     * 
     * @param key the key
     * @return true if the key exists, otherwise false
     */
    boolean hasAttribute(String key);


    /**
     * Gets the string value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    String getString(String key);


    /**
     * Gets the integer value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    Integer getInt(String key);


    /**
     * Gets the boolean value for the specified key.
     * 
     * @param key the key
     * @return the value or null if it does not exist
     */
    Boolean getBoolean(String key);


    /**
     * Gets the configured host name.
     * 
     * @return the host name
     */
    String getHost();


    /**
     * Gets the configured worker count.
     * 
     * @return the worker count
     */
    int getWorkerCount();


    /**
     * Adds the {@link IServerSession} for the {@link SocketAddress}.
     * 
     * @param remoteAddress the {@link SocketAddress}
     * @param session the {@link IServerSession}
     */
    void addServerSession(final SocketAddress remoteAddress, final IServerSession session);


    /**
     * Gets the {@link IServerSession} for the {@link SocketAddress}.
     * 
     * @param remoteAddress the {@link SocketAddress}
     * @return the {@link IServerSession}
     */
    IServerSession getServerSession(final SocketAddress remoteAddress);


    /**
     * Removes the {@link IServerSession}.
     * 
     * @param session the {@link IServerSession}
     * @throws IOException if something goes wrong
     */
    void removeServerSession(final IServerSession session) throws IOException;


    /**
     * Gets the {@link BeanContextStorage} for the session id.
     * 
     * @param sessionId the session id
     * @return the {@link BeanContextStorage}
     */
    BeanContextStorage getSessionContext(String sessionId);


    /**
     * Adds the {@link BeanContextStorage} for the session id.
     * 
     * @param sessionId the session id
     * @param sessionContext the {@link BeanContextStorage}
     */
    void addSessionContext(String sessionId, BeanContextStorage sessionContext);


    /**
     * Removes the {@link BeanContextStorage} for the session id.
     * 
     * @param sessionId the session id
     */
    void removeSessionContext(String sessionId);

}