/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;
import java.util.function.Supplier;

/**
 * Supplier of Hibernate 3 Data Store.
 */
public class HibernateDataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        // method to force class initialization
        Configuration configuration = new Configuration();
        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        SessionFactory sessionFactory = configuration.configure("hibernate.cfg.xml")
                .setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                .setProperty(Environment.URL,
                        "jdbc:mysql://localhost:" + System.getProperty("mysql.port", "3306") + "/root")
                .setProperty(Environment.USER, "root")
                .setProperty(Environment.PASS, "root")
                .setProperty("hibernate.search.default.directory_provider", "filesystem")
                .setProperty("hibernate.search.default.indexBase", "/tmp/lucene/indexes")
                .buildSessionFactory();

        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport(configuration).setHaltOnError(true);
        schemaExport.drop(false, true);
        schemaExport.execute(false, true, false, true);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new RuntimeException(schemaExport.getExceptions().toString());
        }

        return new HibernateStore(sessionFactory);
    }
}
