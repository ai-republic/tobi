package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import javax.ws.rs.core.Application;

import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * A customized context builder for Resteasy.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyHttpContextBuilder {
    protected ResteasyDeployment deployment = new ResteasyDeployment();
    protected String path = "/";


    /**
     * Gets the {@link ResteasyDeployment}.
     * 
     * @return the {@link ResteasyDeployment}
     */
    public ResteasyDeployment getDeployment() {
        return deployment;
    }


    /**
     * Sets the {@link ResteasyDeployment}.
     * 
     * @param the {@link ResteasyDeployment}
     */
    public void setDeployment(final ResteasyDeployment deployment) {
        this.deployment = deployment;
    }


    /**
     * Gets the {@link Application} path.
     * 
     * @return the path
     */
    public String getPath() {
        return path;
    }


    /**
     * Path to bind context.
     *
     * @param path the {@link Application} path
     */
    public void setPath(final String path) {
        this.path = path;
        if (!this.path.startsWith("/")) {
            this.path = "/" + path;
        }
    }


    /**
     * Binds and starts the {@link ResteasyDeployment}.
     */
    public void bind() {
        final ResteasyHttpConfiguration config = new ResteasyHttpConfiguration();
        deployment.getDefaultContextObjects().put(ResteasyConfiguration.class, config);

        deployment.start();

    }


    /**
     * Stops the {@link ResteasyDeployment}.
     */
    public void cleanup() {
        deployment.stop();
    }
}
