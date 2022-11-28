/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpql.filter;

import static com.yahoo.elide.core.utils.TypeHelper.appendAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getPathAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getTypeAlias;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.Type;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * Generates JPQL filter fragments for the 'hasmember' and 'hasnomember' operators.
 */
public class HasMemberJPQLGenerator implements JPQLPredicateGenerator {
    private final EntityDictionary dictionary;
    private final boolean negated;

    private static String INNER = "_INNER_";

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

        String notMember = negated ? "NOT " : "";

        if (! FilterPredicate.toManyInPath(dictionary, predicate.getPath())) {
            return String.format("%s %sMEMBER OF %s",
                    predicate.getParameters().get(0).getPlaceholder(),
                    notMember,
                    aliasGenerator.apply(predicate.getPath()));
        }

        Path path = predicate.getPath();
        Preconditions.checkArgument(path.lastElement().isPresent());
        Preconditions.checkArgument(! (path.lastElement().get().getType() instanceof Collection));

        //Creates JPQL like:
        //EXISTS (SELECT 1
        //FROM example.Book
        //    _INNER_example_Book
        //LEFT JOIN
        //    _INNER_example_Book.authors  _INNER_example_Book_authors
        //WHERE
        //    _INNER_example_Book.id = example_Book.id
        //    AND
        //    _INNER_example_Book_authors.name = :authors_name_7c02839c_0)
        return String.format("%sEXISTS (SELECT 1 FROM %s WHERE %s = %s AND %s = %s)",
                notMember,
                getFromClause(path),
                getInnerQueryIdField(path),
                getOuterQueryIdField(path, aliasGenerator),
                getInnerFilterFieldReference(path),
                predicate.getParameters().get(0).getPlaceholder());
    }

    private String getOuterQueryIdField(Path path, Function<Path, String> aliasGenerator) {
        Path.PathElement firstElement = path.getPathElements().get(0);
        Type<?> modelType = firstElement.getType();
        String idField = dictionary.getIdFieldName(modelType);
        Type<?> idFieldType = dictionary.getIdType(modelType);

        Path idPath = new Path(Arrays.asList(new Path.PathElement(
                modelType,
                idFieldType,
                idField
        )));

        return aliasGenerator.apply(idPath);
    }

    private String getInnerQueryIdField(Path path) {
        Path.PathElement firstElement = path.getPathElements().get(0);

        Path firstElementPath = new Path(Arrays.asList(firstElement));
        Type<?> modelType = firstElement.getType();
        String idField = dictionary.getIdFieldName(modelType);

        return INNER + getPathAlias(firstElementPath, dictionary) + "." + idField;
    }

    private String getInnerFilterFieldReference(Path path) {
        Path.PathElement lastElement = path.lastElement().get();
        String fieldName = lastElement.getFieldName();

        return INNER + getPathAlias(path, dictionary) + "." + fieldName;
    }

    private String getFromClause(Path path) {
        Path.PathElement firstElement = path.getPathElements().get(0);
        Path.PathElement lastElement = path.lastElement().get();

        String entityName = firstElement.getType().getCanonicalName();
        String currentAlias = INNER + getTypeAlias(firstElement.getType());

        StringBuilder fromClause = new StringBuilder();
        fromClause.append(entityName).append(" ").append(currentAlias);

        for (Path.PathElement element : path.getPathElements()) {

            //No need to join the last path segment.
            if (element == lastElement) {
                break;
            }
            String nextAlias = appendAlias(currentAlias, element.getFieldName());

            fromClause.append(" LEFT JOIN ")
                    .append(currentAlias).append(".").append(element.getFieldName())
                    .append(" ").append(nextAlias);

            currentAlias = nextAlias;
        }

        return fromClause.toString();
    }
}
