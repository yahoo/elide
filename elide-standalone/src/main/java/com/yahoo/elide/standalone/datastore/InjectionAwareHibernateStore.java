/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.datastore;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.hibernate5.HibernateSessionFactoryStore;

import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import javax.persistence.metamodel.EntityType;

/**
 * Hibernate store that is aware of the injector. Namely, when objects are created, the injector will run
 * to populate any @Inject fields.
 */
@Slf4j
@Deprecated
public class InjectionAwareHibernateStore extends HibernateSessionFactoryStore {

    private final ServiceLocator injector;

    /**
     * Constructor
     *
     * @param injector hk2 injector to bind.
     * @param sessionFactory Hibernate session factory.
     */
    public InjectionAwareHibernateStore(ServiceLocator injector, SessionFactory sessionFactory) {
        super(sessionFactory, true, ScrollMode.FORWARD_ONLY);
        this.injector = injector;
    }

    @Override
    public void populateEntityDictionary(EntityDictionary dictionary) {
        // Bind all entities
        super.populateEntityDictionary(dictionary);

        if (injector != null) {
            log.info("Binding injector to entities");
            Set<EntityType<?>> types = sessionFactory.getMetamodel().getEntities();
            log.info("Found {} entities", types.size());

            for (EntityType type : sessionFactory.getMetamodel().getEntities()) {
                try {
                    Class mappedClass = type.getJavaType();
                    // Ignore this result. We are just checking to see if it throws an exception meaning that
                    // provided class was _not_ an entity.
                    dictionary.lookupEntityClass(mappedClass);

                    // Bind if successful
                    dictionary.bindEntity(mappedClass);

                    // Make injectable
                    dictionary.bindInitializer(injector::inject, mappedClass);
                } catch (IllegalArgumentException e)  {
                    // Ignore this entity
                    // Turns out that hibernate may include non-entity types in this list when using things
                    // like envers. Since they are not entities, we do not want to bind them into the entity
                    // dictionary
                }
            }
        } else {
            log.info("No injector found, not binding one to entities.");
        }
    }
}
