package com.yahoo.elide.async.integration.framework;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.common.collect.Lists;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.async.integration.tests.AsyncIntegrationTest;
import com.yahoo.elide.async.models.security.AsyncQueryOperationChecks.AsyncQueryOwner;
import com.yahoo.elide.async.models.security.AsyncQueryOperationChecks.AsyncQueryStatusValue;
import com.yahoo.elide.async.service.AsyncCleanerService;
import com.yahoo.elide.async.service.AsyncExecutorService;
import com.yahoo.elide.async.service.AsyncQueryDAO;
import com.yahoo.elide.async.service.DefaultAsyncQueryDAO;
import com.yahoo.elide.audit.InMemoryLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.security.checks.Check;

public class AsyncIntegrationTestApplicationResourceConfig extends ResourceConfig {
    public static final InMemoryLogger LOGGER = new InMemoryLogger();

    @Inject
    public AsyncIntegrationTestApplicationResourceConfig(ServiceLocator injector) {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                Map<String, Class<? extends Check>> checkMappings = new HashMap<>();
                checkMappings.put("Principal is Owner", AsyncQueryOwner.class);
                checkMappings.put("value is Cancelled", AsyncQueryStatusValue.class);

                EntityDictionary dictionary = new EntityDictionary(checkMappings, injector::inject);
                Elide elide = new Elide(new ElideSettingsBuilder(AsyncIntegrationTest.getDataStore())
                        .withEntityDictionary(dictionary)
                        .withAuditLogger(new Slf4jLogger())
                        .build());

                AsyncQueryDAO asyncQueryDao = new DefaultAsyncQueryDAO(elide, elide.getDataStore());

                AsyncExecutorService asyncExecService = new AsyncExecutorService(elide, 7,
                        7, asyncQueryDao);

                AsyncCleanerService asyncCleanerService = new AsyncCleanerService(elide, 7,
                        7, asyncQueryDao);

                // Bind elide instance for injection into endpoint
                bind(elide).to(Elide.class).named("elide");

                // Bind additional elements
                bind(elide.getElideSettings()).to(ElideSettings.class);
                bind(elide.getElideSettings().getDictionary()).to(EntityDictionary.class);
                bind(elide.getElideSettings().getDataStore()).to(DataStore.class).named("elideDataStore");

                bind(asyncQueryDao).to(AsyncQueryDAO.class);
                bind(asyncExecService).to(AsyncExecutorService.class);
                bind(asyncCleanerService).to(AsyncCleanerService.class);
            };
        });

        registerFilters(Lists.newArrayList(AsyncAuthFilter.class));
    }

    /**
     * Register provided JAX-RS filters.
     */
    private void registerFilters(List<Class<?>> filters) {
        filters.forEach(this::register);
    }

}
