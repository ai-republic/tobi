package com.airepublic.tobi.core.spi;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;

import javax.enterprise.context.SessionScoped;

/**
 * The server session which can store session relevant information.
 * 
 * @author Torsten Oltmanns
 *
 */
@SessionScoped
public interface IServerSession extends Serializable, AutoCloseable {

    /**
     * Gets the session id.
     * 
     * @return the session id
     */
    String getId();


    /**
     * Sets the session id.
     * 
     * @param id the session id
     */
    void setId(String id);


    /**
     * Gets the {@link IChannelProcessor} associated with this session.
     * 
     * @return the {@link IChannelProcessor}
     */
    IChannelProcessor getChannelProcessor();


    /**
     * Sets the {@link IChannelProcessor} associated with this session.
     * 
     * @param channelProcessor the {@link IChannelProcessor}
     */
    void setChannelProcessor(IChannelProcessor channelProcessor);


    /**
     * Gets the {@link SocketChannel} associated with this session.
     * 
     * @return the {@link SocketChannel}
     */
    SocketChannel getChannel();


    /**
     * Add the {@link ByteBuffer}s to the read queue.
     * 
     * @param buffer the {@link ByteBuffer}s
     */
    void addToReadBuffer(ByteBuffer... buffer);


    /**
     * Add the {@link ByteBuffer}s to the write queue.
     * 
     * @param buffer the {@link ByteBuffer}s
     */
    void addToWriteBuffer(ByteBuffer... buffer);


    /**
     * Add the {@link ByteBuffer}s to the write queue and use the specified
     * {@link CompletionHandler} to notify any interested.
     * 
     * @param handler the {@link CompletionHandler}
     * @param buffer the {@link ByteBuffer}s
     */
    void addToWriteBuffer(CompletionHandler<?, ?> handler, ByteBuffer... buffer);


    @Override
    void close();


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

}