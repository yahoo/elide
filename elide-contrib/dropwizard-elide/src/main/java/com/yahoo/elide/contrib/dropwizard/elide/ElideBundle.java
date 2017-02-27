/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dropwizard.elide;

import com.google.common.collect.ImmutableList;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.AuditLogger;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import com.yahoo.elide.jsonapi.JsonApiMapper;
import com.yahoo.elide.resources.JsonApiEndpoint;
import com.yahoo.elide.security.PermissionExecutor;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.internal.scanning.AnnotationAcceptingListener;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.hibernate.SessionFactory;

import javax.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 * An Elide Bundle for Dropwizard.
 *
 * @param <T> Dropwizard Configuration
 */
public abstract class ElideBundle<T extends Configuration>
        implements ConfiguredBundle<T>, DatabaseConfiguration<T>, ElideConfiguration<T> {

    public static final String DEFAULT_NAME = "elide-bundle";

    private final ImmutableList<Class<?>> entities;
    private final SessionFactoryFactory sessionFactoryFactory;

    /**
     * Create a new ElideBundle.
     *
     * @param pckg string with package containing Hibernate entities (classes annotated with Hibernate {@code @Entity}
     *             annotation) e. g. {@code com.codahale.fake.db.directory.entities}
     */
    protected ElideBundle(String pckg) {
        this(pckg, new SessionFactoryFactory());
    }

    protected ElideBundle(String pckg, SessionFactoryFactory sessionFactoryFactory) {
        this(new String[] { pckg }, sessionFactoryFactory);
    }

    protected ElideBundle(String[] pckgs, SessionFactoryFactory sessionFactoryFactory) {
        this(findEntityClassesFromDirectory(pckgs), sessionFactoryFactory);
    }

    public ElideBundle(Class<?> entity, Class<?>... entities) {
        this(ImmutableList.<Class<?>>builder().add(entity).add(entities).build(),
                new SessionFactoryFactory());
    }

    public ElideBundle(ImmutableList<Class<?>> entities,
                       SessionFactoryFactory sessionFactoryFactory) {
        this.entities = entities;
        this.sessionFactoryFactory = sessionFactoryFactory;
    }

    protected String name() {
        return DEFAULT_NAME;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        final DataStore dataStore = getDataStore(configuration, environment);
        final AuditLogger auditLogger = getAuditLogger(configuration, environment);
        final EntityDictionary entityDictionary = getEntityDictionary(configuration, environment);
        final JsonApiMapper jsonApiMapper = getJsonApiMapper(configuration, environment);
        final Function<RequestScope, PermissionExecutor> permissionExecutor
                = getPermissionExecutor(configuration, environment);

        final JsonApiEndpoint.DefaultOpaqueUserFunction getUserFn = getUserFn(configuration, environment);

        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore);
                if (auditLogger != null) {
                    builder = builder.withAuditLogger(auditLogger);
                }

                if (entityDictionary != null) {
                    builder = builder.withEntityDictionary(entityDictionary);
                }

                if (jsonApiMapper != null) {
                    builder = builder.withJsonApiMapper(jsonApiMapper);
                }

                if (permissionExecutor != null) {
                    builder = builder.withPermissionExecutor(permissionExecutor);
                }
                bind(new Elide(builder.build())).to(Elide.class).named("elide");

                bind(getUserFn)
                        .to(JsonApiEndpoint.DefaultOpaqueUserFunction.class)
                        .named("elideUserExtractionFunction");
            }
        });
    }

    protected void configure(org.hibernate.cfg.Configuration configuration) {
    }

    /**
     * By default created a HibernateStore.
     *
     * @param configuration Dropwizard configuration
     * @param environment Dropwizard environment
     * @return a HibernateStore
     */
    @Override
    public DataStore getDataStore(T configuration, Environment environment) {
        final PooledDataSourceFactory dbConfig = getDataSourceFactory(configuration);
        SessionFactory sessionFactory = sessionFactoryFactory.build(this, environment, dbConfig, entities, name());

        return new HibernateStore.Builder(sessionFactory).build();
    }

    public ImmutableList<Class<?>> getEntities() {
        return entities;
    }

    public SessionFactoryFactory getSessionFactoryFactory() {
        return sessionFactoryFactory;
    }

    /**
     * Method scanning given directory for classes containing Hibernate @Entity annotation.
     *
     * @param pckgs string array with packages containing Hibernate entities (classes annotated with @Entity annotation)
     *             e.g. com.codahale.fake.db.directory.entities
     * @return ImmutableList with classes from given directory annotated with Hibernate @Entity annotation
     */
    public static ImmutableList<Class<?>> findEntityClassesFromDirectory(String[] pckgs) {
        @SuppressWarnings("unchecked")
        final AnnotationAcceptingListener asl = new AnnotationAcceptingListener(Entity.class);
        final PackageNamesScanner scanner = new PackageNamesScanner(pckgs, true);

        while (scanner.hasNext()) {
            final String next = scanner.next();
            if (asl.accept(next)) {
                try (final InputStream in = scanner.open()) {
                    asl.process(next, in);
                } catch (IOException e) {
                    throw new RuntimeException("AnnotationAcceptingListener failed to process scanned resource: "
                            + next);
                }
            }
        }

        final ImmutableList.Builder<Class<?>> builder = ImmutableList.builder();
        asl.getAnnotatedClasses().forEach(builder::add);

        return builder.build();
    }
}
