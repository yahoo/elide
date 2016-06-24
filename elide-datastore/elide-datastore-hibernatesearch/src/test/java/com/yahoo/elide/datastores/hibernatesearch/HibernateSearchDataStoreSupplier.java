/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernatesearch;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;
import java.util.function.Supplier;

/**
 * Supplier of Hibernate 5 Data Store.
 */
public class HibernateSearchDataStoreSupplier implements Supplier<DataStore> {
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
                        .applySetting("hibernate.search.default.directory_provider", "filesystem")
                        .applySetting("hibernate.search.default.indexBase", "/tmp/lucene/indexes")
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport(metadataImplementor); //.setHaltOnError(true);
        schemaExport.drop(false, true);
        schemaExport.execute(false, true, false, true);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new RuntimeException(schemaExport.getExceptions().toString());
        }

        return new HibernateSearchStore(metadataImplementor.buildSessionFactory());
    }
}
