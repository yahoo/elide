/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.util.HashSet;

import javax.persistence.Entity;

public class Util {
    /**
     * Retrieve a hibernate session factory.
     *
     * @param hibernate5ConfigPath File path to hibernate config (i.e. hibernate-cfg.xml)
     * @param modelPackageName Name of package containing all models to be loaded by hibernate
     * @return Hibernate session factory.
     */
    public static SessionFactory getSessionFactory(String hibernate5ConfigPath, String modelPackageName) {
        StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                .configure(new File(hibernate5ConfigPath))
                .build();
        MetadataSources sources = new MetadataSources(standardRegistry);

        getAllEntities(modelPackageName).forEach(sources::addAnnotatedClass);

        Metadata metaData =  sources.getMetadataBuilder().build();
        return metaData.getSessionFactoryBuilder().build();
    }

    /**
     * Get all the entities in a package.
     *
     * @param packageName Package name
     * @return All entities found in package.
     */
    public static HashSet<Class> getAllEntities(String packageName) {
        return new HashSet<>(new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .setUrls(ClasspathHelper.forClassLoader(ClassLoader.getSystemClassLoader()))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(packageName))))
                .getTypesAnnotatedWith(Entity.class));
    }
}
