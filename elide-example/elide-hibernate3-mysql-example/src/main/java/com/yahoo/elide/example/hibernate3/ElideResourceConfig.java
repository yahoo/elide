/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.hibernate3;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.datastores.hibernate3.HibernateStore;

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
                SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
                Elide elide = new Elide(new ElideSettingsBuilder(new HibernateStore.Builder(sessionFactory).build())
                        .withAuditLogger(new Slf4jLogger())
                        .build());
                bind(elide).to(Elide.class).named("elide");
            }
        });
    }
}
