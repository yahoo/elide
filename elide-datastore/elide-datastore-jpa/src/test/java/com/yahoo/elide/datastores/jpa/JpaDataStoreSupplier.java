/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Supplier of Hibernate 5 Data Store.
 */
@Slf4j
public class JpaDataStoreSupplier implements Supplier<DataStore> {
    private static final String JDBC_PREFIX = "jdbc:mysql://localhost:";
    private static final String JDBC_SUFFIX = "/root?serverTimezone=UTC";
    private static final String MYSQL_PORT_PROPERTY = "mysql.port";
    private static final String MYSQL_PORT = "3306";
    private static final String ROOT = "root";

    @Override
    public DataStore get() {
        Map<String, Object> options = new HashMap<>();
        ArrayList<Class> bindClasses = new ArrayList<>();

        try {
            bindClasses.addAll(ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class));
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
        options.put("javax.persistence.jdbc.url", JDBC_PREFIX
                                + System.getProperty(MYSQL_PORT_PROPERTY, MYSQL_PORT)
                                + JDBC_SUFFIX);
        options.put("javax.persistence.jdbc.user", ROOT);
        options.put("javax.persistence.jdbc.password", ROOT);
        options.put(AvailableSettings.LOADED_CLASSES, bindClasses);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("elide-tests", options);
        HibernateEntityManager em = (HibernateEntityManager) emf.createEntityManager();

        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL, JDBC_PREFIX
                                + System.getProperty(MYSQL_PORT_PROPERTY, MYSQL_PORT)
                                + JDBC_SUFFIX)
                        .applySetting(Environment.USER, ROOT)
                        .applySetting(Environment.PASS, ROOT)
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        EnumSet<TargetType> type = EnumSet.of(TargetType.DATABASE);
        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.drop(type, metadataImplementor);
        schemaExport.execute(type, SchemaExport.Action.CREATE, metadataImplementor);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new IllegalStateException(schemaExport.getExceptions().toString());
        }

        return new JpaDataStore(
                () -> { return em; },
                (entityManager) -> { return new NonJtaTransaction(entityManager); }
        );
    }
}
