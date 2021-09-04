package com.yahoo.elide.extension.runtime;

import static com.yahoo.elide.datastores.jpa.JpaDataStore.DEFAULT_LOGGER;
import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.datastores.jpa.JpaDataStore;
import com.yahoo.elide.datastores.jpa.transaction.NonJtaTransaction;

import org.hibernate.Session;

import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@ApplicationScoped
public class ElideBeans {
    @Produces
    public EntityDictionary produceDictionary(ClassScanner scanner) {
        return EntityDictionary.builder().scanner(scanner).build();
    }

    @Produces
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
