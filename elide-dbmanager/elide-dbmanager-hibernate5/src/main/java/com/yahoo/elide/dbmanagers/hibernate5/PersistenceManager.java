package com.yahoo.elide.dbmanagers.hibernate5;

import com.google.common.base.Preconditions;
import com.yahoo.elide.core.DatabaseManager;
import com.yahoo.elide.core.DatabaseTransaction;
import com.yahoo.elide.core.EntityDictionary;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.EntityType;

/**
 * Manager for javax.persistence compatible db resource
 */
public class PersistenceManager implements DatabaseManager {
    private final EntityManagerFactory entityManagerFactory;

    public PersistenceManager(EntityManagerFactory entityManagerFactory) {
        Preconditions.checkNotNull(entityManagerFactory);
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        for (EntityType entity : entityManagerFactory.getMetamodel().getEntities()) {
            dictionary.bindEntity(entity.getBindableJavaType());
        }
    }

    @Override
    public DatabaseTransaction beginTransaction() {
        return new PersistenceTransaction(entityManagerFactory.createEntityManager());
    }
}
