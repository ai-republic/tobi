package com.airepublic.tobi.core.spi;

/**
 * An enumeration of actions to be performed on the channel connection.
 * 
 * @author Torsten Oltmanns
 *
 */
public enum ChannelAction {
    KEEP_OPEN, CLOSE_INPUT, CLOSE_OUTPUT, CLOSE_ALL;
}
