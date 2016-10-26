/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.persistence;

import com.yahoo.elide.Elide;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.datastores.hibernate5.PersistenceStore;
import com.yahoo.elide.resources.JsonApiEndpoint;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * Example application for resource config.
 */
public class ElideResourceConfig extends Application {

    @Override
    public Set<Object> getSingletons() {
        EntityManagerFactory entityManagerFactory =
                Persistence.createEntityManagerFactory("com.yahoo.elide.example");

        Elide elide = new Elide.Builder(new PersistenceStore(entityManagerFactory))
                .withAuditLogger(new Slf4jLogger())
                .build();

        Set<Object> set = new HashSet<>();
        set.add(new JsonApiEndpoint(elide, v -> null));
        return set;
    }
}
