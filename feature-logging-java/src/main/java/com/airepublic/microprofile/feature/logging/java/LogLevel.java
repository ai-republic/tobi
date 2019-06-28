package com.airepublic.microprofile.feature.logging.java;

import java.util.logging.Level;

public enum LogLevel {
    ALL(Level.ALL), INFO(Level.INFO), FINE(Level.FINE), FINER(Level.FINER), FINEST(Level.FINEST), CONFIG(Level.CONFIG), WARNING(Level.WARNING), SEVERE(Level.SEVERE), OFF(Level.OFF);
    private Level level;


    LogLevel(final Level level) {
        this.level = level;
    }


    public Level getLevel() {
        return level;
    }
}