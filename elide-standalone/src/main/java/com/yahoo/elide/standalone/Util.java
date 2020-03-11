/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.utils.ClassScanner;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

/**
 * Util.
 */
public class Util {

    public static EntityManagerFactory getEntityManagerFactory(String modelPackageName, boolean includeAsyncModel, 
            Properties options) {

        // Configure default options for example service
        if (options.isEmpty()) {
            options.put("hibernate.show_sql", "true");
            options.put("hibernate.hbm2ddl.auto", "create");
            options.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
            options.put("hibernate.current_session_context_class", "thread");
            options.put("hibernate.jdbc.use_scrollable_resultset", "true");

            // Collection Proxy & JDBC Batching
            options.put("hibernate.jdbc.batch_size", "50");
            options.put("hibernate.jdbc.fetch_size", "50");
            options.put("hibernate.default_batch_fetch_size", "100");

            // Hikari Connection Pool Settings
            options.putIfAbsent("hibernate.connection.provider_class",
                    "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
            options.putIfAbsent("hibernate.hikari.connectionTimeout", "20000");
            options.putIfAbsent("hibernate.hikari.maximumPoolSize", "30");
            options.putIfAbsent("hibernate.hikari.idleTimeout", "30000");

            options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
            options.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost/elide?serverTimezone=UTC");
            options.put("javax.persistence.jdbc.user", "elide");
            options.put("javax.persistence.jdbc.password", "elide123");
        }

        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl("elide-stand-alone",
                combineModelEntities(modelPackageName, includeAsyncModel), options);

        return new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(persistenceUnitInfo), new HashMap<>())
                .build();
    }

    /**
     * Combine the model entities with Async model.
     *
     * @param modelPackageName Package name
     * @param includeAsyncModel Include Async model package Name
     * @return All entities combined from both package.
     */
    public static List<String> combineModelEntities(String modelPackageName, boolean includeAsyncModel) {

        List<String> modelEntities = getAllEntities(modelPackageName);

        if (includeAsyncModel) {
            modelEntities.addAll(getAllEntities(AsyncQuery.class.getPackage().getName()));
        }
        return modelEntities;
    }

    /**
     * Get all the entities in a package.
     *
     * @param packageName Package name
     * @return All entities found in package.
     */
    public static List<String> getAllEntities(String packageName) {
        return ClassScanner.getAnnotatedClasses(packageName, Entity.class).stream()
                .map(Class::getName)
                .collect(Collectors.toList());
    }
}
