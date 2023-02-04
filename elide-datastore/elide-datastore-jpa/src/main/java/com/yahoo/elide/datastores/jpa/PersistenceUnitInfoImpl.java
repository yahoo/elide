/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import lombok.Data;

import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * Persistent Unit implementation for Dynamic Configuration.
 */
@Data
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    private String persistenceUnitName;
    private String persistenceProviderClassName;
    private PersistenceUnitTransactionType transactionType;
    private DataSource jtaDataSource;
    private DataSource nonJtaDataSource;
    private List<String> mappingFileNames;
    private List<URL> jarFileUrls;
    private URL persistenceUnitRootUrl;
    private List<String> managedClassNames;
    private SharedCacheMode sharedCacheMode;
    private ValidationMode validationMode;
    private Properties properties;
    private String persistenceXMLSchemaVersion;
    private ClassLoader classLoader;
    private ClassLoader newTempClassLoader;

    public PersistenceUnitInfoImpl(
            String persistenceUnitName,
            List<String> managedClassNames,
            Properties properties,
            ClassLoader loader) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
        this.classLoader = loader;
        this.newTempClassLoader = loader;
    }

    public PersistenceUnitInfoImpl(String persistenceUnitName, List<String> managedClassNames, Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }


    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    @Override
    public void addTransformer(ClassTransformer classTransformer) {
        //Not implemented
    }
}
