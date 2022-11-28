/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpql.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.datastores.jpql.filter.JPQLPredicateGenerator;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Maps an entity field to a custom JPQL Fragment Generator.
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Inherited
public @interface JPQLFilterFragment {

    /**
     * The operator to bind the fragment to.
     * @return the operator
     */
    Operator operator();

    /**
     * The function which generates the fragment.
     * @return the generator
     */
    Class<? extends JPQLPredicateGenerator> generator();
}
