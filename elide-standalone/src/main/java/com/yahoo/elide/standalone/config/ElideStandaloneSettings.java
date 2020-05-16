/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.config;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.Injector;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.standalone.Util;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.models.Swagger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.function.Consumer;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.core.SecurityContext;

/**
 * Interface for configuring an ElideStandalone application.
 */
public interface ElideStandaloneSettings {
    /* Elide settings */
    /**
     * A map containing check mappings for security across Elide. If not provided, then an empty map is used.
     * In case of an empty map, checks can be referenced by their fully qualified class names.
     *
     * @return Check mappings.
     */
    default Map<String, Class<? extends Check>> getCheckMappings() {
        return Collections.emptyMap();
    }

    /**
     * Elide settings to be used for bootstrapping the Elide service. By default, this method constructs an
     * ElideSettings object using the application overrides provided in this class. If this method is overridden,
     * the returned settings object is used over any additional Elide setting overrides.
     *
     * That is to say, if you intend to override this method, expect to fully configure the ElideSettings object to
     * your needs.
     *
     * @param injector Service locator for web service for dependency injection.
     * @return Configured ElideSettings object.
     */
    default ElideSettings getElideSettings(ServiceLocator injector) {
        EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(getModelPackageName(),
                getDatabaseProperties());
        DataStore dataStore = new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                (em -> { return new NonJtaTransaction(em); }));

        EntityDictionary dictionary = new EntityDictionary(getCheckMappings(),
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        injector.inject(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return injector.create(cls);
                    }
                });

        dictionary.scanForSecurityChecks();

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withUseFilterExpressions(true)
                .withEntityDictionary(dictionary)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(getAuditLogger());

        if (enableIS06081Dates()) {
            builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
        }

        return builder.build();
    }

    /**
     * The function used to extract a user from the SecurityContext.
     *
     * @return Function for user extraction.
     */
    default DefaultOpaqueUserFunction getUserExtractionFunction() {
        return SecurityContext::getUserPrincipal;
    }

    /* Non-required application/server settings */
    /**
     * Port for HTTP server to listen on.
     *
     * @return Default: 8080
     */
    default int getPort() {
        return 8080;
    }

    /**
     * Package name containing your models. This package will be recursively scanned for @Entity's and
     * registered with Elide.
     *
     * NOTE: This will scan for all entities in that package and bind this data to a set named "elideAllModels".
     *       If providing a custom ElideSettings object, you can inject this data into your class by using:
     *
     *       <strong>@Inject @Named("elideAllModels") Set&lt;Class&gt; entities;</strong>
     *
     * @return Default: com.yourcompany.elide.models
     */
    default String getModelPackageName() {
        return "com.yourcompany.elide.models";
    }

    /**
     * API root path specification for JSON-API. Namely, this is the mount point of your API. By default it will look
     * something like:
     *   <strong>yourcompany.com/api/v1/YOUR_ENTITY</strong>
     *
     * @return Default: /api/v1/*
     */
    default String getJsonApiPathSpec() {
        return "/api/v1/*";
    }

    /**
     * API root path specification for the GraphQL endpoint. Namely, this is the root uri for GraphQL.
     *
     * @return Default: /graphql/api/v1
     */
    default String getGraphQLApiPathSepc() {
        return "/graphql/api/v1";
    }


    /**
     * API root path specification for the Swagger endpoint. Namely, this is the root uri for Swagger docs.
     *
     * @return Default: /swagger/*
     */
    default String getSwaggerPathSepc() {
        return "/swagger/*";
    }

    /**
     * Enable the JSONAPI endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableJSONAPI() {
        return true;
    }

    /**
     * Enable the GraphQL endpoint. If false, the endpoint will be disabled.
     *
     * @return Default: True
     */
    default boolean enableGraphQL() {
        return true;
    }

    /**
     * Whether Dates should be ISO8601 strings (true) or epochs (false).
     * @return
     */
    default boolean enableIS06081Dates() {
        return true;
    }

    /**
     * Whether or not Codahale metrics, healthchecks, thread, ping, and admin servlet
     * should be enabled.
     * @return
     */
    default boolean enableServiceMonitoring() {
        return true;
    }


    /**
     * Enable swagger documentation by returning non empty map object.
     * @return Map object that maps document name to swagger object.
     */
    default Map<String, Swagger> enableSwagger() {
        return new HashMap<>();
    }


    /**
     * JAX-RS filters to register with the web service.
     *
     * @return Default: Empty
     */
    default List<Class<?>> getFilters() {
        return Collections.emptyList();
    }

    /**
     * Supplemental resource configuration for Elide application. This should be a fully qualified class name.
     *
     * Before calling the consumer, the class will be injected by the ServiceLocator.
     *
     * @return Default: null
     */
    default Consumer<ResourceConfig> getApplicationConfigurator() {
        // Do nothing by default
        return (x) -> { };
    }


    /**
     * Gets properties to configure the database
     *
     * @return Default: ./settings/hibernate.cfg.xml
     */
    default Properties getDatabaseProperties() {
        return new Properties();
    }

    /**
     * A hook to directly modify the jetty servlet context handler as necessary.
     *
     * @param servletContextHandler ServletContextHandler in use by Elide standalone.
     */
    default void updateServletContextHandler(ServletContextHandler servletContextHandler) {
        // Do nothing
    }

    /**
     * Gets the audit logger for elide
     *
     * @return Default: Slf4jLogger
     */
    default AuditLogger getAuditLogger() {
        return new Slf4jLogger();
    }
}
