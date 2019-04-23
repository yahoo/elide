/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.standalone;

import lombok.Data;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Properties;

@Data
/**
 * Data object which allows programmatic configuration of everything inside a persistence.xml file.
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    public PersistenceUnitInfoImpl(String persistenceUnitName, List<String> managedClassNames, Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

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

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    public void addTransformer(ClassTransformer classTransformer) {
        //NOOP
    }

}
