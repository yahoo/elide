/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate3;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;
import com.yahoo.elide.utils.ClassScanner;

import example.Filtered;
import example.Parent;
import example.TestCheckMappings;

import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import java.util.function.Supplier;

import javax.persistence.Entity;

/**
 * Supplier of Hibernate 3 Data Store.
 */
public class HibernateDataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        // Add additional checks to our static check mappings map.
        // NOTE: This is a bit hacky. We need to do a major overhaul on our test architecture
        TestCheckMappings.MAPPINGS.put("filterCheck", Filtered.FilterCheck.class);
        TestCheckMappings.MAPPINGS.put("filterCheck3", Filtered.FilterCheck3.class);

        // method to force class initialization
        Configuration configuration = new Configuration();
        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
                    .forEach(configuration::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        SessionFactory sessionFactory = configuration.configure("hibernate.cfg.xml")
                .setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                .setProperty(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
                .setProperty(Environment.URL, "jdbc:h2:mem:root;IGNORECASE=TRUE")
                .setProperty(Environment.USER, "root")
                .setProperty(Environment.PASS, "root")
                .buildSessionFactory();

        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport(configuration).setHaltOnError(true);
        schemaExport.drop(false, true);
        schemaExport.execute(false, true, false, true);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new RuntimeException(schemaExport.getExceptions().toString());
        }

        return new HibernateStore(sessionFactory, true, ScrollMode.FORWARD_ONLY);
    }
}
