/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dropwizard.elide;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableList;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;
import org.hibernate.SessionFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ElideBundleIT {
    private final DataSourceFactory dbConfig = new DataSourceFactory();
    private final ImmutableList<Class<?>> entities = ImmutableList.of(Author.class, Book.class);
    private final SessionFactoryFactory factory = mock(SessionFactoryFactory.class);
    private final SessionFactory sessionFactory = mock(SessionFactory.class);
    private final Configuration configuration = mock(Configuration.class);
    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final ElideBundle<Configuration> bundle = new ElideBundle<Configuration>(entities, factory) {
        @Override
        public DataSourceFactory getDataSourceFactory(Configuration configuration) {
            return dbConfig;
        }
    };

    @BeforeTest
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());

        when(factory.build(eq(bundle),
                any(Environment.class),
                any(DataSourceFactory.class),
                anyList(),
                eq("hibernate"))).thenReturn(sessionFactory);
    }

    @Test
    public void buildsASessionFactory() throws Exception {
        bundle.run(configuration, environment);

        verify(factory).build(bundle, environment, dbConfig, entities, "elide-bundle");
    }

    @Test
    public void defaultGetUserFnDidNothing() throws Exception {
        Assert.assertNull(bundle.getUserFn(configuration, environment).apply(mock(SecurityContext.class)));
    }

    @Test
    public void hasADataStore() throws Exception {
        Assert.assertTrue(bundle.getDataStore(configuration, environment) instanceof HibernateStore);
    }
}
