/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.datastores.hibernate5.AbstractHibernateStore;
import com.yahoo.elide.resources.DefaultOpaqueUserFunction;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Example application for resource config.
 */
public class ElideResourceConfig extends ResourceConfig {
    public ElideResourceConfig() {
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                DefaultOpaqueUserFunction noUserFn = v -> null;
                bind(noUserFn)
                        .to(DefaultOpaqueUserFunction.class)
                        .named("elideUserExtractionFunction");

                SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

                bind(new Elide(new ElideSettingsBuilder(new AbstractHibernateStore.Builder(sessionFactory).build())
                        .withAuditLogger(new Slf4jLogger())
                        .build()))
                        .to(Elide.class).named("elide");
            }
        });
    }
}
