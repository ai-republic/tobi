package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;

public class RestEasyHttpContextBuilder {
    protected ResteasyDeployment deployment = new ResteasyDeployment();
    protected String path = "/";
    protected SecurityDomain securityDomain;


    public ResteasyDeployment getDeployment() {
        return deployment;
    }


    public void setDeployment(final ResteasyDeployment deployment) {
        this.deployment = deployment;
    }


    public String getPath() {
        return path;
    }


    /**
     * Path to bind context
     *
     * @param path
     */
    public void setPath(final String path) {
        this.path = path;
        if (!this.path.startsWith("/")) {
            this.path = "/" + path;
        }
    }


    public SecurityDomain getSecurityDomain() {
        return securityDomain;
    }


    /**
     * Will turn on Basic Authentication
     *
     * @param securityDomain
     */
    public void setSecurityDomain(final SecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
    }


    public void bind() {
        final RestEasyHttpConfiguration config = new RestEasyHttpConfiguration();
        deployment.getDefaultContextObjects().put(ResteasyConfiguration.class, config);

        if (securityDomain != null) {
            // TODO add filtering and security
            // server.addFilterLast("Basic Auth Filter", new
            // RestEasyAsyncBasicAuthFilter(securityDomain));
        }

        deployment.start();

    }


    public void cleanup() {
        deployment.stop();
    }
}
