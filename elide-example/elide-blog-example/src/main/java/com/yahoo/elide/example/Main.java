/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.standalone.ElideStandalone;
import com.yahoo.elide.standalone.Util;
import com.yahoo.elide.standalone.config.ElideStandaloneSettings;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.ServiceLocator;

import javax.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Example backend using Elide library.
 */
@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        ElideStandalone elide = new ElideStandalone(new ElideStandaloneSettings() {
            public int getPort() {
                return 4080;
            }

            public String getJsonApiPathSpec() {
                return "/*";
            }

            public String getModelPackageName() {

                    //This needs to be changed to the package where your models live.
                    return "com.yahoo.elide.example.models";
            }

            //TODO - Remove once getDatabaseProperties is in ElideStandalone
            public ElideSettings getElideSettings(ServiceLocator injector) {
                EntityManagerFactory entityManagerFactory = Util.getEntityManagerFactory(getModelPackageName(),
                        getDatabaseProperties());

                DataStore dataStore = new JpaDataStore(
                        () -> { return entityManagerFactory.createEntityManager(); },
                        (em -> { return new NonJtaTransaction(em); }));

                EntityDictionary dictionary = new EntityDictionary(getCheckMappings(), injector::inject);

                ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                        .withUseFilterExpressions(true)
                        .withEntityDictionary(dictionary)
                        .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                        .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary));

                if (enableIS06081Dates()) {
                    builder = builder.withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));
                }

                return builder.build();
            }

            public Properties getDatabaseProperties() {

                Properties dbProps;
                try {
                    dbProps = new Properties();
                    dbProps.load(
                            Main.class.getClassLoader().getResourceAsStream("dbconfig.properties")
                    );
                    return dbProps;
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        elide.start();
    }
}
