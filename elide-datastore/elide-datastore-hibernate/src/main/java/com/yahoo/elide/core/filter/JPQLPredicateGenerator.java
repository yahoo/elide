/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;

import java.util.function.Function;

/**
 * Converts a JPQL column alias and list of arguments into a JPQL filter predicate fragment.
 */
@FunctionalInterface
public interface JPQLPredicateGenerator {
    String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator);
}
