/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.datastore.test.DataStoreTestHarness;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;
import com.yahoo.elide.utils.ClassScanner;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;

/**
 *  IT Test Harness for the Hibernate 3 Data Store.
 */
public class HibernateDataStoreHarness implements DataStoreTestHarness {

    private Configuration configuration;
    private DataStore store;

    public HibernateDataStoreHarness() {
        // method to force class initialization
        configuration = new Configuration();
        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
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
