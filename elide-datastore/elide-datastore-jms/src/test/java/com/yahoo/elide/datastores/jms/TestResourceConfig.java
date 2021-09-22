package com.yahoo.elide.datastores.jms;

import com.yahoo.elide.core.audit.Slf4jLogger;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Inject;

/**
 * Resource configuration for integration tests.
 */
public class TestResourceConfig extends ResourceConfig {

    @Inject
    public TestResourceConfig(ServiceLocator injector) {
        register(new TestBinder(new Slf4jLogger(), injector));
    }
}
