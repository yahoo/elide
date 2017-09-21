/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import com.yahoo.elide.standalone.config.ElideResourceConfig;
import com.yahoo.elide.standalone.config.RuntimeSettings;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigCache;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Entry point for the Elide standalone application.
 *
 * Usage: java -cp &lt;MODEL_JAR&gt;:elide-standalone.jar
 *
 * NOTE: The settings can be passed as system properties (i.e. -D params) or set in a properties file.
 *       By default, this file will try to load the "elide-settings.properties" file from your current working directory
 *       or the $CWD/settings directory. Lastly, it will check the root of your classpath for the same file.
 */
@Slf4j
public class Main {
    private static final RuntimeSettings SETTINGS;

    static {
        // NOTE: This is to avoid owner blowing up if this is unspecified.
        if (System.getProperty("elide.settings") == null) {
            System.setProperty("elide.settings", "./elide-settings.properties");
        }
        SETTINGS = ConfigCache.getOrCreate(RuntimeSettings.class, System.getProperties());
    }

    public static void main(String[] args) throws Exception {
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        log.info("Starting jetty server on port: {}", SETTINGS.port());
        Server jettyServer = new Server(SETTINGS.port());
        jettyServer.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, SETTINGS.jsonApiPathSpec());
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages", "com.yahoo.elide.resources");
        jerseyServlet.setInitParameter("javax.ws.rs.Application", ElideResourceConfig.class.getCanonicalName());

        try {
            jettyServer.start();
            log.info("Jetty started!");
            jettyServer.join();
        } catch (Exception e) {
            log.error("Unexpected exception caught: {}", e.getMessage(), e);
            throw e;
        } finally {
            jettyServer.destroy();
        }
    }
}
