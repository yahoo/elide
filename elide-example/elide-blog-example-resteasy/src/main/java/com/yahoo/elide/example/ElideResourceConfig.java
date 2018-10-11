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
import com.yahoo.elide.resources.JsonApiEndpoint;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Example application for resource config.
 */
public class ElideResourceConfig extends Application {

    @Override
    public Set<Object> getSingletons() {
        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();

        Elide elide = new Elide(new ElideSettingsBuilder(new AbstractHibernateStore.Builder(sessionFactory).build())
                .withAuditLogger(new Slf4jLogger())
                .build());

        Set<Object> set = new HashSet<>();
        set.add(new JsonApiEndpoint(elide, v -> null));
        return set;
    }
}
