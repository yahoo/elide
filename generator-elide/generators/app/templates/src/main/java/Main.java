package <%= groupId %>;

import com.yahoo.elide.resources.JsonApiEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Example backend using Elide library.
 */
@Slf4j
public class Main {
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
