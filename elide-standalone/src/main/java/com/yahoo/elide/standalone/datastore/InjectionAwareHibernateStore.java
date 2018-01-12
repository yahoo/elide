/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.datastore;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.hibernate5.HibernateStore;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.api.ServiceLocator;
import org.hibernate.ScrollMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;

import java.util.Collection;

/**
 * Hibernate store that is aware of the injector. Namely, when objects are created, the injector will run
 * to populate any @Inject fields.
 */
@Slf4j
public class InjectionAwareHibernateStore extends HibernateStore {

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
            Collection<ClassMetadata> metadata = this.sessionFactory.getAllClassMetadata().values();
            log.info("Found {} entities", metadata.size());
            /* bind all entities to injector */
            metadata.forEach(meta -> {
                // Ensure they receive proper injection:
                // Since tools like envers can insert non-entities into our metadata, make sure
                // we ignore non-entities:
                try {
                    // This is only used to catch an exception. If non-entity is passed, then an exception is thrown
                    dictionary.lookupEntityClass(meta.getMappedClass());
                    // If we have gotten this far, we can happily bind the entity
                    dictionary.bindInitializer(injector::inject, meta.getMappedClass());
                    log.debug("Elide bound entity: {}", meta.getEntityName());
                } catch (IllegalArgumentException e) {
                    // Ignore this non-entity
                    log.debug("Elide ignoring non-entity found in hibernate metadata: {}", meta.getEntityName());
                }
            });
        } else {
            log.info("No injector found, not binding one to entities.");
        }
    }
}
