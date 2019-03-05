/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.hibernate5;

import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.models.generics.Manager;
import com.yahoo.elide.models.triggers.Invoice;
import com.yahoo.elide.utils.ClassScanner;

import example.Filtered;
import example.Parent;
import example.TestCheckMappings;

import org.hibernate.MappingException;
import org.hibernate.ScrollMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.util.EnumSet;
import java.util.function.Supplier;

import javax.persistence.Entity;

/**
 * Supplier of Hibernate 5 Data Store.
 */
public class HibernateDataStoreSupplier implements Supplier<DataStore> {
    @Override
    public DataStore get() {
        // Add additional checks to our static check mappings map.
        // NOTE: This is a bit hacky. We need to do a major overhaul on our test architecture
        TestCheckMappings.MAPPINGS.put("filterCheck", Filtered.FilterCheck.class);
        TestCheckMappings.MAPPINGS.put("filterCheck3", Filtered.FilterCheck3.class);

        // method to force class initialization
        MetadataSources metadataSources = new MetadataSources(
                new StandardServiceRegistryBuilder()
                        .configure("hibernate.cfg.xml")
                        .applySetting(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread")
                        .applySetting(Environment.DIALECT, "org.hibernate.dialect.H2Dialect")
                        .applySetting(Environment.URL, "jdbc:h2:mem:root;IGNORECASE=TRUE")
                        .applySetting(Environment.USER, "root")
                        .applySetting(Environment.PASS, "root")
                        .build());

        try {
            ClassScanner.getAnnotatedClasses(Parent.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Manager.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
            ClassScanner.getAnnotatedClasses(Invoice.class.getPackage(), Entity.class)
                    .forEach(metadataSources::addAnnotatedClass);
        } catch (MappingException e) {
            throw new RuntimeException(e);
        }

        MetadataImplementor metadataImplementor = (MetadataImplementor) metadataSources.buildMetadata();

        EnumSet<TargetType> type = EnumSet.of(TargetType.DATABASE);
        // create example tables from beans
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.drop(type, metadataImplementor);
        schemaExport.execute(type, SchemaExport.Action.CREATE, metadataImplementor);

        if (!schemaExport.getExceptions().isEmpty()) {
            throw new IllegalStateException(schemaExport.getExceptions().toString());
        }

        return new AbstractHibernateStore.Builder(metadataImplementor.buildSessionFactory())
                .withScrollEnabled(true)
                .withScrollMode(ScrollMode.FORWARD_ONLY)
                .build();
    }
}
