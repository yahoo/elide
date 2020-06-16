/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex.bridgeable;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.hibernate5.AbstractHibernateStore;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.example.beans.HibernateUser;

import org.hibernate.ScrollMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.util.EnumSet;

public class BridgeableDataStoreHarness implements DataStoreTestHarness {
    public static AbstractHibernateStore LATEST_HIBERNATE_STORE;

    private DataStore store;
    private MetadataImplementor metadataImplementor;

    public BridgeableDataStoreHarness() {
        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL, "jdbc:h2:mem:root;IGNORECASE=TRUE")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .applySetting(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
                        .build());

        metadataSources.addAnnotatedClass(HibernateUser.class);

        metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        resetSchema();

        LATEST_HIBERNATE_STORE = new AbstractHibernateStore.Builder(metadataImplementor.buildSessionFactory())
                .withScrollEnabled(true)
                .withScrollMode(ScrollMode.FORWARD_ONLY)
                .build();

        BridgeableRedisStore hbaseStore = new BridgeableRedisStore();

        store = new MultiplexManager(LATEST_HIBERNATE_STORE, hbaseStore);
    }

    private void resetSchema() {
        EnumSet<TargetType> type = EnumSet.of(TargetType.DATABASE);
        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.drop(type, metadataImplementor);
        schemaExport.execute(type, SchemaExport.Action.CREATE, metadataImplementor);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new IllegalStateException(schemaExport.getExceptions().toString());
        }
    }

    @Override
    public DataStore getDataStore() {
        return store;
    }

    @Override
    public void cleanseTestData() {
        resetSchema();
    }
}
