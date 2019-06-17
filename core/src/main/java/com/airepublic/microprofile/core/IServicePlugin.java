package com.airepublic.microprofile.core;

import java.util.Set;

public interface IServicePlugin {
    String getName();


    Set<String> getSupportedProtocols();

}
