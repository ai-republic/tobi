# microprofile
Microprofile modular HTTP, REST, SSE and WebSocket server.

## Overview
This modular server framework consists of the core server, modules, plugins and features.
Modules, plugins and features can be enabled just by adding their dependency.
Each module can have multiple plugins, e.g. the Http module can have one, multiple or all of JAX-RS plugin, SSE plugin and/or WebSocket plugin.
Features can be used globally.

Currently only HTTP module is provided. Any other protocol can be implemented and integrated very easily (see IServerModule).
There are 3 plugins that can be used with the HTTP module:


1.  JAX-RS plugin
2.  SSE plugin
3.  WebSocket plugin
 
Below is an overview of the features:

*   CDI Open-Web-Beans integration 2.0.11
*   CDI Weld integration 3.6.3
*   Microprofile Config 1.3
*   Microprofile Fault-tolerance 2.0
*   Microprofile Health 2.0.1
*   Microprofile JWT-Auth 1.1.1
*   Microprofile Metrics 2.0.1
*   Microprofile Open-API 1.1
*   Microprofile Open Tracing 1.3.1
*   Microprofile Restclient 1.2.0






 