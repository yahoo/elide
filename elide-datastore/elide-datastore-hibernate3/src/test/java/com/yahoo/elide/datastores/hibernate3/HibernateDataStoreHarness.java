/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.DefaultClassScanner;

import example.Company;
import example.Parent;
import example.models.generics.Manager;
import example.models.triggers.Invoice;
import example.models.versioned.BookV2;

import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;

/**
 * IT Test Harness for the Hibernate 3 Data Store.
 */
public class HibernateDataStoreHarness implements DataStoreTestHarness {

    private Configuration configuration;
    private DataStore store;

    public HibernateDataStoreHarness() {
        ClassScanner scanner = DefaultClassScanner.getInstance();
        // method to force class initialization
        configuration = new Configuration();
        try {
            scanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            scanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            scanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            scanner.getAnnotatedClasses(BookV2.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            scanner.getAnnotatedClasses(AsyncQuery.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            scanner.getAnnotatedClasses(Company.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
        } catch (MappingException e) {
            throw new IllegalStateException(e);
        }

        SessionFactory sessionFactory = configuration.configure("hibernate.cfg.xml")
                .setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                .setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
                .setProperty(Environment.URL, "jdbc:h2:mem:root;IGNORECASE=TRUE")
                .setProperty(Environment.USER, "root")
                .setProperty(Environment.PASS, "root")
                .buildSessionFactory();

        resetSchema();

        store = new HibernateStore(sessionFactory, true, ScrollMode.FORWARD_ONLY);
    }

    public void resetSchema() {
        SchemaExport schemaExport = new SchemaExport(configuration).setHaltOnError(true);
        schemaExport.drop(false, true);
        schemaExport.execute(false, true, false, true);

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
