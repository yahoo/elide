/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;

import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.function.Function;

/**
 * Generates JPQL filter fragments for the 'hasmember' and 'hasnomember' operators.
 */
public class HasMemberJPQLGenerator implements JPQLPredicateGenerator{
    private final EntityDictionary dictionary;
    private final boolean negated;

    public HasMemberJPQLGenerator(EntityDictionary dictionary) {
        this(dictionary, false);
    }

    public HasMemberJPQLGenerator(EntityDictionary dictionary, boolean negated) {
        this.dictionary = dictionary;
        this.negated = negated;
    }

    @Override
    public String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator) {
        Preconditions.checkArgument(predicate.getParameters().size() == 1);

        String notMember = negated ? "NOT" : "";

        if (! FilterPredicate.toManyInPath(dictionary, predicate.getPath())) {
            return String.format("%s %s MEMBER OF %s",
                    predicate.getParameters().get(0).getPlaceholder(),
                    notMember,
                    aliasGenerator.apply(predicate.getPath()));
        }

        Path path = predicate.getPath();
        Preconditions.checkArgument(path.lastElement().isPresent());
        Preconditions.checkArgument(! (path.lastElement().get().getType() instanceof Collection));

        //EXISTS (SELECT 1 FROM Author a WHERE a.id = example_Book_authors.id AND a.id = %s)
        return String.format("EXISTS (SELECT 1 FROM %s WHERE %s = %s AND %s = %s");
    }
}
