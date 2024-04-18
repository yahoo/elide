/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.extension.runtime;

import com.paiondata.elide.Elide;
import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.audit.Slf4jLogger;
import com.paiondata.elide.core.datastore.DataStore;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.dictionary.Injector;
import com.paiondata.elide.core.security.checks.prefab.Collections;
import com.paiondata.elide.core.utils.ClassScanner;
import com.paiondata.elide.datastores.jpa.JpaDataStore;
import com.paiondata.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.paiondata.elide.graphql.GraphQLSettings;
import com.paiondata.elide.jsonapi.JsonApiSettings;
import com.paiondata.elide.swagger.OpenApiBuilder;
import com.paiondata.elide.swagger.OpenApiDocument;
import com.paiondata.elide.swagger.resources.ApiDocsEndpoint;
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
        ElideSettings.ElideSettingsBuilder builder = ElideSettings.builder()
                .entityDictionary(dictionary)
                .maxPageSize(config.defaultMaxPageSize)
                .defaultPageSize(config.defaultPageSize)
                .auditLogger(new Slf4jLogger())
                .baseUrl(rootPath)
                .settings(new JsonApiSettings.JsonApiSettingsBuilder())
                .settings(new GraphQLSettings.GraphQLSettingsBuilder())
                .dataStore(store);

        if (config.verboseErrors) {
            builder = builder.verboseErrors(true);
        }

        LOG.debug("Scanning for security checks...");
        dictionary.scanForSecurityChecks();

        return new Elide(builder.build());
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
            docs.add(new ApiDocsEndpoint.ApiDocsRegistration("api", () -> openApi,
                    OpenApiDocument.Version.OPENAPI_3_0.getValue(), apiVersion));
        });

        return docs;
    }

    @Produces
    public Optional<DataFetcherExceptionHandler> emptyDataFetcherExceptionHandler() {
        return Optional.empty();
    }

    @Produces
    Optional<com.paiondata.elide.core.request.route.RouteResolver> emptyRouteResolver() {
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
