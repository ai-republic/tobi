package com.airepublic.microprofile.javaserver.boot;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.airepublic.microprofile.core.Bootstrap;
import com.airepublic.microprofile.core.IConfigConstants;

public class Boot implements IConfigConstants {
    private static final Logger LOG = LoggerFactory.getLogger(Boot.class);


    public static void main(final String[] args) throws IOException {
        Bootstrap.start();
    }
}
