# Tobi modular server
Tobi is a modular server framework which is Microprofile 3.0 compliant. You only plugin what you really need. Thus it is highly flexible and pluggability needs no extensive configuration.
With Tobi you are not restricted to HTTP. It can serve any proprietary protocol. 
Currently modules exist for HTTP/S with plugins for REST, SSE and WebSocket and many features from the Microprofile orbit.

## Overview
This modular server framework consists of the core server, modules, plugins and features.
Modules, plugins and features can be enabled just by adding their dependency.
Each module can have multiple plugins, e.g. the HTTP/S module can have one, multiple or all of JAX-RS plugin, SSE plugin and/or WebSocket plugin.
Features can be used globally by all modules and plugins.

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

## User guide
To start using Tobi as a server you need to add the following dependency:

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>server-core</artifactId>
	<version>1.0.0</version>
</dependency>
```

Then you can decide whether you like to use OpenWebBean (OWB) or WELD as your CDI container and add the appropriate dependency.

### Using OpenWebBeans
To use OpenWebBeans add the following dependency:

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>feature-cdi-owb</artifactId>
	<version>1.0.0</version>
</dependency>
```

### Using WELD
To use WELD add the following dependency:

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>feature-cdi-weld</artifactId>
	<version>1.0.0</version>
</dependency>
```

### Adding a module
To add a module like HTTP/S to you project you either just add the plugin(s) as which have the dependency of the module or add the module explicitly like:

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>module-http</artifactId>
	<version>1.0.0</version>
</dependency>
```

### Adding a plugin
To add JAX-RS (REST) support simply add the following dependency (this will also add the dependency to `module-http`):

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>plugin-http-jaxrs</artifactId>
	<version>1.0.0</version>
</dependency>
```

Now you just need to add your `javax.ws.rs` annotated resources to your project.
Similar you can use/combine other plugins such as `plugin-http-sse` or `plugin-http-websocket`.

### Adding a feature
The `feature-logging-java` and `feature-mp-config` features are automatically added with the `core-server`.
To add other features to use in your project just add the appropriate dependency like:

```maven
<dependency>
	<groupId>com.ai-republic.tobi</groupId>
	<artifactId>feature-mp-faulttolerance</artifactId>
	<version>1.0.0</version>
</dependency>
```


### Starting the Tobi server
In your projects META-INF folder you will need to place a `microprofile-config.properties` file and configure the port parameters like this:

```properties
workerCount=5
host=localhost
http.port=8080
http.ssl.port=8443
http.keystore.file=~/keystore.jks
http.keystore.password=changeit
http.truststore.file=~/cacerts.jks
http.truststore.password=changeit

```
_NOTE:_ `http.port`and `http.ssl.port` are the 2 properties needed to configure the HTTP/S module. In fact if you would like Tobi only to accept HTTPS you simply don't add the `http.port` property. See also the documentation on the HttpModule.

Then just call:

```java
Tobi.start();
```

to start your Tobi server. It will automatically discover all your resources.


### Example application
An example can be found under the [example](https://github.com/ai-republic/tobi/tree/master/example) project.

