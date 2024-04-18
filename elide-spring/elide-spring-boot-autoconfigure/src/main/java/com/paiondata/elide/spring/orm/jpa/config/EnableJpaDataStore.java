/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.orm.jpa.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure multiple JpaDataStores.
 *
 * @see com.paiondata.elide.datastores.jpa.JpaDataStore
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(EnableJpaDataStores.class)
public @interface EnableJpaDataStore {
    /**
     * The entity manager factory bean name to be used for the JpaDataStore.
     *
     * @return the entity manager factory bean name.
     */
    String entityManagerFactoryRef() default "entityManagerFactory";

    /**
     * The platform transaction manager bean name to be used for the JpaDataStore.
     *
     * @return the platform transaction manager bean name.
     */
    String transactionManagerRef() default "transactionManager";

    /**
     * The entity classes to manage. Otherwise all entities in the entity manager
     * factory will be managed.
     *
     * @return the entity classes to manage
     */
    Class<?>[] managedClasses() default {};
}
