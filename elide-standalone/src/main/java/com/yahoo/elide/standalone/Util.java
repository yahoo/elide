/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;

public class Util {

    public static EntityManager getEntityManager(String modelPackageName, Properties options) {

        // Configure default options for example service
        if (options.isEmpty()) {
            options.put("hibernate.show_sql", "true");
            options.put("hibernate.hbm2ddl.auto", "create");
            options.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
            options.put("hibernate.current_session_context_class", "thread");
            options.put("hibernate.jdbc.use_scrollable_resultset", "true");

            //TODO - Maybe configure Hikari or C3PO as a best practice

            options.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
            options.put("javax.persistence.jdbc.url", "jdbc:mysql://localhost/elide?serverTimezone=UTC");
            options.put("javax.persistence.jdbc.user", "elide");
            options.put("javax.persistence.jdbc.password", "elide123");
        }

        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfoImpl("elide-stand-alone",
                getAllEntities(modelPackageName), options);

        EntityManagerFactory emf =  new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(persistenceUnitInfo), new HashMap<>())
                .build();

        return emf.createEntityManager();
    }

    /**
     * Get all the entities in a package.
     *
     * @param packageName Package name
     * @return All entities found in package.
     */
    public static List<String> getAllEntities(String packageName) {
        return new ArrayList<>(new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forClassLoader(ClassLoader.getSystemClassLoader()))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))))
                .getTypesAnnotatedWith(Entity.class))
                .stream()
                .map(Class::getName)
                .collect(Collectors.toList());
    }
}
