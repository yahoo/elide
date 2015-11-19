/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate3.HibernateStore;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;
import java.util.function.Supplier;

/**
 * Supplier of Hibernate5 Data Store.
 */
public class Hibernate3DataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        // method to force class initialization
        Configuration c = new Configuration();

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class).forEach(c::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        SessionFactory sessionFactory = c.configure("hibernate.cfg.xml")
                .setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                .setProperty(Environment.URL,
                        "jdbc:mysql://localhost:" + System.getProperty("mysql.port", "3306") + "/root")
                .setProperty(Environment.USER, "root")
                .setProperty(Environment.PASS, "root")
                .buildSessionFactory();

        // create Example tables from beans
        SchemaExport se = new SchemaExport(c).setHaltOnError(true);
        se.drop(false, true);
        se.execute(false, true, false, true);

        if (se.getExceptions().size() != 0) {
            throw new RuntimeException("" + se.getExceptions());
        }

        return new HibernateStore(sessionFactory);
    }
}
