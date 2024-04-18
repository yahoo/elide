/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jpql.filter;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;

import java.util.function.Function;

/**
 * Converts a column alias and list of arguments into a JPQL filter predicate fragment.
 */
@FunctionalInterface
public interface JPQLPredicateGenerator {

    /**
     * Generates a JPQL/SQL expression for a filter predicate.
     * @param predicate The predicate to generate.
     * @param aliasGenerator Takes a predicate path and converts it into a JPQL/SQL column alias.
     * @return A JPQL/SQL filter expression.
     */
    String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator);
}
