/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.util.EnumSet;
import javax.persistence.Entity;

/**
 * IT Test Harness for the Hibernate 5 Data Store.
 */
public class HibernateDataStoreHarness implements DataStoreTestHarness {

    private DataStore store;
    private MetadataImplementor metadataImplementor;

    public HibernateDataStoreHarness() {
        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
                        .applySetting(Environment.URL, "jdbc:h2:mem:root;IGNORECASE=TRUE")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        resetSchema();

        store = new AbstractHibernateStore.Builder(metadataImplementor.buildSessionFactory())
                .withScrollEnabled(true)
                .withScrollMode(ScrollMode.FORWARD_ONLY)
                .build();
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
