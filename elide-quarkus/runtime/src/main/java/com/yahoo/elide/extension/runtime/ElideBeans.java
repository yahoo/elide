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

import org.hibernate.Session;

import io.quarkus.arc.runtime.BeanContainer;

import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Unmanaged;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@ApplicationScoped
public class ElideBeans {
    @Produces
    @Named("elide")
    @Singleton
    public Elide produceElide(DataStore store, EntityDictionary dictionary) {
        ElideSettingsBuilder builder = new ElideSettingsBuilder(store)
                .withEntityDictionary(dictionary)
                .withDefaultMaxPageSize(100)
                .withDefaultPageSize(100)
                .withJoinFilterDialect(new RSQLFilterDialect(dictionary))
                .withSubqueryFilterDialect(new RSQLFilterDialect(dictionary))
                .withAuditLogger(new Slf4jLogger())
                .withBaseUrl("/")
                .withISO8601Dates("yyyy-MM-dd'T'HH:mm'Z'", TimeZone.getTimeZone("UTC"))
                .withJsonApiPath("json")
                .withGraphQLApiPath("graphql");

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
}
