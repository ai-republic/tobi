package com.airepublic.microprofile.util.http.common;

import com.airepublic.microprofile.core.AbstractIOHandler;
import com.airepublic.microprofile.core.IServicePlugin;

public interface IServicePluginHttp extends IServicePlugin {
    Class<? extends AbstractIOHandler> findMapping(String path);
}
