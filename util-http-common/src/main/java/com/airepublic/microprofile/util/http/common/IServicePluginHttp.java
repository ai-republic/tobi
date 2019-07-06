package com.airepublic.microprofile.util.http.common;

import com.airepublic.microprofile.core.spi.IIOHandler;
import com.airepublic.microprofile.core.spi.IServicePlugin;

public interface IServicePluginHttp extends IServicePlugin {
    Class<? extends IIOHandler> findMapping(String path);
}
