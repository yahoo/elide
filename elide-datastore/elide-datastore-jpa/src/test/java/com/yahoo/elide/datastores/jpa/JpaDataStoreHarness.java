/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * IT Test Harness for the JpaDataStore.
 */
public class JpaDataStoreHarness implements DataStoreTestHarness {

    private static final String JDBC = "jdbc:h2:mem:root;IGNORECASE=TRUE";
    private static final String ROOT = "root";

    private DataStore store;
    private MetadataImplementor metadataImplementor;

    public JpaDataStoreHarness() {
        Map<String, Object> options = new HashMap<>();
        ArrayList<Class<?>> bindClasses = new ArrayList<>();

        try {
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class));
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class));
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class));
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(AsyncQuery.class.getPackage(), Entity.class));
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        options.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        options.put("javax.persistence.jdbc.url", JDBC);
        options.put("javax.persistence.jdbc.user", ROOT);
        options.put("javax.persistence.jdbc.password", ROOT);
        options.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        options.put(AvailableSettings.LOADED_CLASSES, bindClasses);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("elide-tests", options);

        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL, JDBC)
                        .applySetting(Environment.USER, ROOT)
                        .applySetting(Environment.PASS, ROOT)
                        .applySetting(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
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

        store = new JpaDataStore(
                () -> { return emf.createEntityManager(); },
                (entityManager) -> { return new NonJtaTransaction(entityManager); }
        );
    }

    public void resetSchema() {
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
