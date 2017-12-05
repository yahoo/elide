/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.standalone.datastore;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.hibernate5.HibernateSessionFactoryStore;
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
            Collection<ClassMetadata> metadata = this.sessionFactory.getAllClassMetadata().values();
            log.info("Found {} entities", metadata.size());
            /* bind all entities to injector */
            metadata.forEach(meta -> {
                // Ensure they receive proper injection:
                dictionary.bindInitializer(injector::inject, meta.getMappedClass());
                log.debug("Elide bound entity: {}", meta.getEntityName());
            });
        } else {
            log.info("No injector found, not binding one to entities.");
        }
    }
}
