package com.yahoo.elide.example.hibernate3;

import com.yahoo.elide.Elide;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.dbmanagers.hibernate3.HibernateManager;
import com.yahoo.elide.resources.JsonApiEndpoint;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Example application for resource config
 */
public class ElideResourceConfig extends ResourceConfig {
    public ElideResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                JsonApiEndpoint.DefaultOpaqueUserFunction noUserFn = v -> null;
                bind(noUserFn).to(JsonApiEndpoint.DefaultOpaqueUserFunction.class).named("elideUserExtractionFunction");

                SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
                Elide elide = new Elide(new Slf4jLogger(), new HibernateManager(sessionFactory));
                bind(elide).to(Elide.class).named("elide");
            }
        });
    }
}
