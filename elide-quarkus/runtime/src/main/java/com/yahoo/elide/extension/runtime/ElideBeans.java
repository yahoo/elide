package com.yahoo.elide.extension.runtime;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.audit.Slf4jLogger;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.Injector;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;
import com.yahoo.elide.swagger.SwaggerBuilder;
import com.yahoo.elide.swagger.resources.DocEndpoint;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;

import io.swagger.models.Info;
import io.swagger.models.Swagger;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@ApplicationScoped
public class ElideBeans {
    private ElideConfig config;

    @ConfigProperty(name = "quarkus.http.root-path")
    String rootPath;

    public void setElideConfig(ElideConfig config) {
        this.config = config;
    }

    @Produces
    @Named("elide")
    @Singleton
    public Elide produceElide(DataStore store, EntityDictionary dictionary) {
        ElideSettingsBuilder builder = new ElideSettingsBuilder(store)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(config.defaultMaxPageSize)
                .withDefaultPageSize(config.defaultPageSize)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withBaseUrl(rootPath)
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath(config.baseJsonapi)
                .withGraphQLApiPath(config.baseGraphql);

        if (config.verboseErrors) {
            builder = builder.withVerboseErrors();
        }

        return new Elide(builder.build());
    }

    @Produces
    @Singleton
    public Injector produceInjector(BeanManager manager) {
        return new Injector() {
            @Override
            public void inject(Object entity) {
                //NOOP
            }

            @Override
            public <T> T instantiate(Class<T> cls) {
                return manager.createInstance().select(cls).get();
            }
        };
    }

    @Produces
    @Singleton
    public EntityDictionary produceDictionary(ClassScanner scanner, Injector injector) {
        return EntityDictionary.builder()
                .scanner(scanner)
                .injector(injector)
                .build();
    }

    @Produces
    @Singleton
    public DataStore produceDataStore(
            EntityDictionary dictionary,
            EntityManagerFactory entityManagerFactory
    ) {
        final Consumer<EntityManager> txCancel = em -> em.unwrap(Session.class).cancelQuery();

        DataStore store = new JpaDataStore(
                entityManagerFactory::createEntityManager,
                em -> new NonJtaTransaction(em, txCancel, DEFAULT_LOGGER, true));

        store.populateEntityDictionary(dictionary);
        return store;
    }

    @Produces
    @Named("swagger")
    public List<DocEndpoint.SwaggerRegistration> buildSwagger(EntityDictionary dictionary) {
        List<DocEndpoint.SwaggerRegistration> registrations = new ArrayList<>();

        Info info = new Info()
                .title("Elide Service");

        //TODO - configuration for title and version.

        SwaggerBuilder builder = new SwaggerBuilder(dictionary, info).withLegacyFilterDialect(false);

        registrations.add(new DocEndpoint.SwaggerRegistration("api", builder.build()));

        return registrations;
    }
}
