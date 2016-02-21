package com.yahoo.elide.example.persistence;

import com.yahoo.elide.resources.JsonApiEndpoint;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example backend using Elide library
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        final Server server = new Server(4080);
        final ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        final ServletHolder servletHolder = servletContextHandler.addServlet(ServletContainer.class, "/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages",
                JsonApiEndpoint.class.getPackage().getName());
        servletHolder.setInitParameter("javax.ws.rs.Application",
                ElideResourceConfig.class.getCanonicalName());

        log.info("Web service starting...");
        server.start();

        log.info("Web service running...");
        server.join();
        server.destroy();
    }
}
