/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.extension.runtime;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.security.checks.prefab.Collections;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.graphql.GraphQLSettings;
import com.yahoo.elide.jsonapi.JsonApiSettings;
import com.yahoo.elide.swagger.OpenApiBuilder;
import com.yahoo.elide.swagger.resources.ApiDocsEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;
import org.jboss.logging.Logger;

import graphql.execution.DataFetcherExceptionHandler;
import io.quarkus.runtime.Startup;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@ApplicationScoped
public class ElideBeans {
    private static final Logger LOG = Logger.getLogger(ElideBeans.class.getName());
    @ConfigProperty(name = "quarkus.http.root-path")
    String rootPath;
    private ElideConfig config;

    public void setElideConfig(ElideConfig config) {
        this.config = config;
    }

    @Produces
    @Named("elide")
    @ApplicationScoped
    @Startup
    public Elide produceElide(DataStore store, EntityDictionary dictionary) {
        LOG.debug("Creating Elide bean");
        JsonApiSettings.JsonApiSettingsBuilder jsonApiSettingsBuilder = new JsonApiSettings.JsonApiSettingsBuilder();
        jsonApiSettingsBuilder.path(config.jsonApiPath);
        GraphQLSettings.GraphQLSettingsBuilder graphQLSettingsBuilder = new GraphQLSettings.GraphQLSettingsBuilder();
        graphQLSettingsBuilder.path(config.graphqlPath);
        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder()
                .entityDictionary(dictionary)
                .maxPageSize(config.defaultMaxPageSize)
                .defaultPageSize(config.defaultPageSize)
                .auditLogger(new Slf4jLogger())
                .baseUrl(rootPath)
                .settings(jsonApiSettingsBuilder)
                .settings(graphQLSettingsBuilder)
                .dataStore(store);

        if (config.verboseErrors) {
            builder = builder.verboseErrors(true);
        }

        Elide elide = new Elide(builder.build());
        elide.doScans();
        return elide;
    }

    @Produces
    @ApplicationScoped
    public Injector produceInjector(BeanManager manager, Collections.AppendOnly appendOnlyCheckerBean,
                                    Collections.RemoveOnly removeOnlyCheckerBean) {
        LOG.debug("Creating Injector bean");
        return new Injector() {
            @Override
            public void inject(Object entity) {
                //NOOP
//                manager.
            }

            @Override
            public <T> T instantiate(Class<T> cls) {
                return manager.createInstance().select(cls).get();
            }
        };
    }

    @Produces
    @ApplicationScoped
    public EntityDictionary produceDictionary(ClassScanner scanner, Injector injector) {
        LOG.debug("Creating EntityDictionary bean");
        return EntityDictionary.builder().scanner(scanner).injector(injector).build();
    }

    @Produces
    @ApplicationScoped
    public DataStore produceDataStore(EntityDictionary dictionary, EntityManagerFactory entityManagerFactory) {
        LOG.debug("Creating DataStore bean");
        final Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

        DataStore store = new JpaDataStore(entityManagerFactory::createEntityManager,
                em -> new NonJtaTransaction(em, txCancel), entityManagerFactory::getMetamodel);

        store.populateEntityDictionary(dictionary);
        return store;
    }

    @Produces
    @Named("apiDocs")
    @ApplicationScoped
    public List<ApiDocsEndpoint.ApiDocsRegistration> buildSwagger(Elide elide) {
        LOG.debug("Creating ApiDocsRegistration bean");
        EntityDictionary dictionary = elide.getElideSettings().getEntityDictionary();
        List<ApiDocsEndpoint.ApiDocsRegistration> docs = new ArrayList<>();

        dictionary.getApiVersions().stream().forEach(apiVersion -> {
            Info info = new Info().title("Elide Service").version(apiVersion);
            OpenApiBuilder builder = new OpenApiBuilder(dictionary).apiVersion(apiVersion);
            String moduleBasePath = "/apiDocs/";
            OpenAPI openApi = builder.build().info(info).addServersItem(new Server().url(moduleBasePath));
            docs.add(new ApiDocsEndpoint.ApiDocsRegistration("api", () -> openApi, apiVersion));
        });

        return docs;
    }

    @Produces
    public Optional<DataFetcherExceptionHandler> emptyDataFetcherExceptionHandler() {
        return Optional.empty();
    }

    @Produces
    Optional<com.yahoo.elide.core.request.route.RouteResolver> emptyRouteResolver() {
        return Optional.empty();
    }

    @Produces
    public Collections.AppendOnly appendOnlyCheckerBean() {
        LOG.debug("Creating AppendOnly checker bean");
        return new Collections.AppendOnly<>();
    }

    @Produces
    public Collections.RemoveOnly removeOnlyCheckerBean() {
        LOG.debug("Creating RemoveOnly checker bean");
        return new Collections.RemoveOnly<>();
    }
}
