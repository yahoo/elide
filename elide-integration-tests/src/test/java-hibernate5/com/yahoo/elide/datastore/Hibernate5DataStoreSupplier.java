/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastore;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import example.Parent;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import javax.persistence.Entity;
import java.sql.DriverManager;
import java.util.function.Supplier;

/**
 * Supplier of Hibernate5 Data Store.
 */
public class Hibernate5DataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        // method to force class initialization
        MetadataSources metadata = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.URL,
                                "jdbc:mysql://localhost:" + System.getProperty("mysql.port", "3306") + "/root")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadata::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        MetadataImplementor metadataImpl = (MetadataImplementor) metadata.buildMetadata();

        // create Example tables from beans
        SchemaExport export = new SchemaExport(metadataImpl); //.setHaltOnError(true);
        export.drop(false, true);
        export.execute(false, true, false, true);

        if (export.getExceptions().size() != 0) {
            throw new RuntimeException("" + export.getExceptions());
        }

        return new HibernateStore(metadataImpl.buildSessionFactory());
    }
}
