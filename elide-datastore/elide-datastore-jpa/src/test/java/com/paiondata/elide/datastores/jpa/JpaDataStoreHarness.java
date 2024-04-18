/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jpa;

import static com.paiondata.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;

import com.paiondata.elide.async.models.AsyncQuery;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.datastore.test.DataStoreTestHarness;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.core.utils.DefaultClassScanner;
import com.paiondata.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.paiondata.elide.datastores.jpql.porting.QueryLogger;
import example.Company;
import example.Parent;
import example.models.generics.Manager;
import example.models.targetEntity.SWE;
import example.models.triggers.Invoice;
import example.models.versioned.BookV2;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * IT Test Harness for the JpaDataStore.
 */
public class JpaDataStoreHarness implements DataStoreTestHarness {

    private static final String JDBC = "jdbc:h2:mem:root;IGNORECASE=TRUE;MODE=MYSQL;NON_KEYWORDS=VALUE,USER";
    private static final String ROOT = "root";

    private DataStore store;
    private MetadataImplementor metadataImplementor;
    private final Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;

    public JpaDataStoreHarness() {
        this(DEFAULT_LOGGER, true);
    }

    public JpaDataStoreHarness(Map<String, Object> options) {
        this(DEFAULT_LOGGER, true, options);
    }

    public JpaDataStoreHarness(QueryLogger logger, boolean delegateToInMemoryStore) {
        this(logger, delegateToInMemoryStore, new HashMap<>());
    }

    public JpaDataStoreHarness(QueryLogger logger, boolean delegateToInMemoryStore, Map<String, Object> initialOptions) {
        Map<String, Object> options = new LinkedHashMap<>(initialOptions);
        ClassScanner scanner = new DefaultClassScanner();
        ArrayList<Class<?>> bindClasses = new ArrayList<>();

        try {
            bindClasses.addAll(scanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(SWE.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(BookV2.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(AsyncQuery.class.getPackage(), Entity.class));
            bindClasses.addAll(scanner.getAnnotatedClasses(Company.class.getPackage(), Entity.class));
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        options.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        options.put("jakarta.persistence.jdbc.url", JDBC);
        options.put("jakarta.persistence.jdbc.user", ROOT);
        options.put("jakarta.persistence.jdbc.password", ROOT);
        options.put("hibernate.dialect", "com.paiondata.elide.datastores.jpa.H2MySQLDialect");
        options.put(AvailableSettings.LOADED_CLASSES, bindClasses);

        this.entityManagerFactory = Persistence.createEntityManagerFactory("elide-tests", options);

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
            scanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            scanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            scanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            scanner.getAnnotatedClasses(AsyncQuery.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            scanner.getAnnotatedClasses(Company.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        resetSchema();

        store = buildJpaDataStore(entityManagerFactory, logger, delegateToInMemoryStore);
    }

    protected DataStore buildJpaDataStore(EntityManagerFactory emf, QueryLogger logger, boolean delegateToInMemoryStore) {
        return new JpaDataStore(
                () -> {
                    this.entityManager = emf.createEntityManager();
                    return this.entityManager;
                },
                entityManager -> new NonJtaTransaction(entityManager, txCancel, logger, delegateToInMemoryStore, true),
                emf::getMetamodel
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

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }
}
