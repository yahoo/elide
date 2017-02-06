/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.multiplex.bridgeable;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;
import com.yahoo.elide.example.beans.HibernateUser;
import org.hibernate.ScrollMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.util.function.Supplier;

public class BridgeableStoreSupplier implements Supplier<DataStore> {
    public static HibernateStore LATEST_HIBERNATE_STORE;

    @Override
    public DataStore get() {
        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL,
                                "jdbc:mysql://localhost:" + System.getProperty("mysql.port", "3306") + "/root")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .build());

        metadataSources.addAnnotatedClass(HibernateUser.class);

        MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport(metadataImplementor); //.setHaltOnError(true);
        schemaExport.drop(false, true);
        schemaExport.execute(false, true, false, true);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new RuntimeException(schemaExport.getExceptions().toString());
        }

        LATEST_HIBERNATE_STORE = new HibernateStore.Builder(metadataImplementor.buildSessionFactory())
            .withScrollEnabled(true)
            .withScrollMode(ScrollMode.FORWARD_ONLY)
            .build();

        BridgeableRedisStore hbaseStore = new BridgeableRedisStore();

        return new MultiplexManager(LATEST_HIBERNATE_STORE, hbaseStore);
    }
}
