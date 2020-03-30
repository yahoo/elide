/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.Injector;
import com.yahoo.elide.audit.Slf4jLogger;
import com.yahoo.elide.contrib.swagger.SwaggerBuilder;
import com.yahoo.elide.core.DataStore;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.datastores.aggregation.AggregationDataStore;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.metadata.MetaDataStore;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.SQLQueryEngine;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.datastores.multiplex.MultiplexManager;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.util.HashMap;
import java.util.TimeZone;
import javax.persistence.EntityManagerFactory;

/**
 * Auto Configuration For Elide Services.  Override any of the beans (by defining your own) to change
 * the default behavior.
 */
@Configuration
@EnableConfigurationProperties(ElideConfigProperties.class)
public class ElideAutoConfiguration {

    /**
     * Creates the Elide instance with standard settings.
     * @param dictionary Stores the static metadata about Elide models.
     * @param dataStore The persistence store.
     * @param settings Elide settings.
     * @return A new elide instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public Elide initializeElide(EntityDictionary dictionary,
                                 DataStore dataStore,
                                 ElideConfigProperties settings) {

        ElideSettingsBuilder builder = new ElideSettingsBuilder(dataStore)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(settings.getMaxPageSize())
                .withDefaultPageSize(settings.getPageSize())
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"));

        return new Elide(builder.build());
    }

    /**
     * Creates the entity dictionary for Elide which contains static metadata about Elide models.
     * Override to load check classes or life cycle hooks.
     * @param beanFactory Injector to inject Elide models.
     * @return a newly configured EntityDictionary.
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityDictionary buildDictionary(AutowireCapableBeanFactory beanFactory) {
        EntityDictionary dictionary = new EntityDictionary(new HashMap<>(),
                new Injector() {
                    @Override
                    public void inject(Object entity) {
                        beanFactory.autowireBean(entity);
                    }

                    @Override
                    public <T> T instantiate(Class<T> cls) {
                        return beanFactory.createBean(cls);
                    }
                });

        dictionary.scanForSecurityChecks();
        return dictionary;
    }

    /**
     * Create a QueryEngine instance for aggregation data store to use.
     * @param entityManagerFactory The JPA factory which creates entity managers.
     * @return An instance of a QueryEngine
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryEngine buildQueryEngine(EntityManagerFactory entityManagerFactory) {
        MetaDataStore metaDataStore = new MetaDataStore();

        return new SQLQueryEngine(metaDataStore, entityManagerFactory);
    }

    /**
     * Creates the DataStore Elide.  Override to use a different store.
     * @param entityManagerFactory The JPA factory which creates entity managers.
     * @param queryEngine QueryEngine instance for aggregation data store
     * @return An instance of a JPA DataStore.
     */
    @Bean
    @ConditionalOnMissingBean
    public DataStore buildDataStore(EntityManagerFactory entityManagerFactory, QueryEngine queryEngine) {
        AggregationDataStore aggregationDataStore = new AggregationDataStore(queryEngine);

        JpaDataStore jpaDataStore = new JpaDataStore(
                () -> { return entityManagerFactory.createEntityManager(); },
                    (em -> { return new NonJtaTransaction(em); }));

        // meta data store needs to be put at first to populate meta data models
        return new MultiplexManager(jpaDataStore, queryEngine.getMetaDataStore(), aggregationDataStore);
    }

    /**
     * Creates a singular swagger document for JSON-API.
     * @param dictionary Contains the static metadata about Elide models.
     * @param settings Elide configuration settings.
     * @return An instance of a JPA DataStore.
     */
    @Bean
    @ConditionalOnMissingBean
    public Swagger buildSwagger(EntityDictionary dictionary, ElideConfigProperties settings) {
        Info info = new Info()
                .title(settings.getSwagger().getName())
                .version(settings.getSwagger().getVersion());

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info);

        Swagger swagger = builder.build().basePath(settings.getJsonApi().getPath());

        return swagger;
    }
}
