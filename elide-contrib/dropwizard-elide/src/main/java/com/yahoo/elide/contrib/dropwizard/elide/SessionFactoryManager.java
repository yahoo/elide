/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dropwizard.elide;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.SessionFactory;

/**
 * Managed SessionFactory
 *
 * Same as the SessionFactoryManager in dropwizard-hibernate
 */
public class SessionFactoryManager implements Managed {
    private final SessionFactory factory;
    private final ManagedDataSource dataSource;

    public SessionFactoryManager(SessionFactory factory, ManagedDataSource dataSource) {
        this.factory = factory;
        this.dataSource = dataSource;
    }

    @VisibleForTesting
    ManagedDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void start() throws Exception {
        dataSource.start();
    }

    @Override
    public void stop() throws Exception {
        factory.close();
        dataSource.stop();
    }
}
