/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import lombok.extern.slf4j.Slf4j;

/**
 * Example backend using Elide library.
 */
@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        final Server server = new Server(4080);
        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        servletContextHandler.setInitParameter("javax.ws.rs.Application", ElideResourceConfig.class.getName());
        server.setHandler(servletContextHandler);

        final ServletHolder servletHolder = servletContextHandler.addServlet(HttpServletDispatcher.class, "/*");
        servletHolder.setInitOrder(1);

        log.info("Web service starting...");
        server.start();

        log.info("Web service running...");
        server.join();
        server.destroy();
    }
}
