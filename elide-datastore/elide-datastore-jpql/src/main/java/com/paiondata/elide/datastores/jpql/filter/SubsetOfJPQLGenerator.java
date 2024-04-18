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
 * Generates JPQL filter fragments for the 'subsetof' and 'notsubsetof' operators.
 */
public class SubsetOfJPQLGenerator implements JPQLPredicateGenerator {
    private final EntityDictionary dictionary;
    private final boolean negated;

    private static String INNER = "_INNER_";

    public SubsetOfJPQLGenerator(EntityDictionary dictionary) {
        this(dictionary, false);
    }

    public SubsetOfJPQLGenerator(EntityDictionary dictionary, boolean negated) {
        this.dictionary = dictionary;
        this.negated = negated;
    }

    @Override
    public String generate(FilterPredicate predicate, Function<Path, String> aliasGenerator) {
        Preconditions.checkArgument(!predicate.getParameters().isEmpty());

        String notSubset = negated ? "NOT " : "";

        if (! FilterPredicate.toManyInPath(dictionary, predicate.getPath())) {
            List<String> parts = new ArrayList<>();
            for (FilterParameter parameter : predicate.getParameters()) {
                parts.add(String.format("(CASE %s MEMBER OF %s WHEN true THEN 1 ELSE 0 END)",
                        parameter.getPlaceholder(),
                        aliasGenerator.apply(predicate.getPath())));
            }
            String sum = parts.stream().collect(Collectors.joining(" + "));
            return String.format("%ssize(%s) = %s",
                    notSubset,
                    aliasGenerator.apply(predicate.getPath()),
                    sum);
        }

        Path path = predicate.getPath();
        Preconditions.checkArgument(path.lastElement().isPresent());
        Preconditions.checkArgument(! (path.lastElement().get().getType() instanceof Collection));

        //Creates JPQL like:
        //(
        //        SELECT    Count(DISTINCT _inner_example_author_books_chapters.title)
        //        FROM      example.author _INNER_example_Author
        //        LEFT JOIN _inner_example_author.books _INNER_example_Author_books
        //        LEFT JOIN _inner_example_author_books.chapters _INNER_example_Author_books_chapters
        //        where     _inner_example_author.id = example_author.id) =
        //       (
        //                 SELECT    count(DISTINCT _inner_example_author_books_chapters.title)
        //                 FROM      example.author _inner_example_author
        //                 LEFT JOIN _inner_example_author.books _inner_example_author_books
        //                 LEFT JOIN _inner_example_author_books.chapters _inner_example_author_books_chapters
        //                 WHERE     _inner_example_author.id = example_author.id
        //                 AND       _inner_example_author_books_chapters.title IN (:books_chapters_title_eb0ebc84_0,
        //                                                                          :books_chapters_title_eb0ebc84_1))
        String inClause = predicate.getParameters().stream().map(p -> p.getPlaceholder())
                .collect(Collectors.joining(","));

        return String.format("%s(SELECT COUNT(DISTINCT %s) FROM %s WHERE %s = %s)"
                + " = (SELECT COUNT(DISTINCT %s) FROM %s WHERE %s = %s AND %s IN (%s))",
                notSubset,
                getInnerFilterFieldReference(path),
                getFromClause(path),
                getInnerQueryIdField(path),
                getOuterQueryIdField(path, aliasGenerator),
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
