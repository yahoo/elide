/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.jpql.filter;

import static com.paiondata.elide.core.utils.TypeHelper.appendAlias;
import static com.paiondata.elide.core.utils.TypeHelper.getPathAlias;
import static com.paiondata.elide.core.utils.TypeHelper.getTypeAlias;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.filter.predicates.FilterPredicate.FilterParameter;
import com.paiondata.elide.core.type.Type;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generates JPQL filter fragments for the 'supersetof' and 'notsupersetof' operators.
 */
public class SupersetOfJPQLGenerator implements JPQLPredicateGenerator {
    private final EntityDictionary dictionary;
    private final boolean negated;

    private static String INNER = "_INNER_";

    public SupersetOfJPQLGenerator(EntityDictionary dictionary) {
        this(dictionary, false);
    }

    public SupersetOfJPQLGenerator(EntityDictionary dictionary, boolean negated) {
        this.dictionary = dictionary;
        this.negated = negated;
    }

    @Override
    public String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator) {
        Preconditions.checkArgument(!predicate.getParameters().isEmpty());

        String notSuperset = negated ? "NOT " : "";

        if (! FilterPredicate.toManyInPath(dictionary, predicate.getPath())) {
            List<String> parts = new ArrayList<>();
            for (FilterParameter parameter : predicate.getParameters()) {
                parts.add(String.format("%s %sMEMBER OF %s",
                        parameter.getPlaceholder(),
                        notSuperset,
                        aliasGenerator.apply(predicate.getPath())));
            }
            String operator = negated ? " OR " : " AND ";
            return parts.stream().collect(Collectors.joining(operator));
        }

        Path path = predicate.getPath();
        Preconditions.checkArgument(path.lastElement().isPresent());
        Preconditions.checkArgument(! (path.lastElement().get().getType() instanceof Collection));

        int size = predicate.getValues().stream().collect(Collectors.toSet()).size();

        //Creates JPQL like:
        //2 =
        //        (
        //                  SELECT    count(DISTINCT _inner_example_book_authors.NAME)
        //                  FROM      example.book _inner_example_book
        //                  LEFT JOIN _inner_example_book.authors _inner_example_book_authors
        //                  WHERE     _inner_example_book.id = example_book.id
        //                  AND       _inner_example_book_authors.NAME IN (:authors_name_510e2458_0,
        //                                                                 :authors_name_510e2458_1))
        String inClause = predicate.getParameters().stream().map(p -> p.getPlaceholder())
                .collect(Collectors.joining(","));

        return String.format("%s%s = (SELECT COUNT(DISTINCT %s) FROM %s WHERE %s = %s AND %s IN (%s))",
                notSuperset,
                size,
                getInnerFilterFieldReference(path),
                getFromClause(path),
                getInnerQueryIdField(path),
                getOuterQueryIdField(path, aliasGenerator),
                getInnerFilterFieldReference(path),
                inClause);
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
