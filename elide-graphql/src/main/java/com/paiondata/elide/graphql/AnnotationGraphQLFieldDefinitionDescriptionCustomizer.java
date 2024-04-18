/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.type.Type;

import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition.Builder;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * {@link GraphQLFieldDefinitionCustomizer} that uses an annotation to determine
 * the description.
 *
 * @param <A> annotation
 */
public class AnnotationGraphQLFieldDefinitionDescriptionCustomizer<A extends Annotation>
        implements GraphQLFieldDefinitionCustomizer {
    private final Class<A> annotationClass;
    private final Function<A, String> descriptionFunction;

    /**
     * Constructor.
     *
     * @param annotationClass the annotation class to determine the description
     * @param descriptionFunction function to get the description from the annotation
     */
    public AnnotationGraphQLFieldDefinitionDescriptionCustomizer(Class<A> annotationClass,
            Function<A, String> descriptionFunction) {
        this.annotationClass = annotationClass;
        this.descriptionFunction = descriptionFunction;
    }

    @Override
    public void customize(Builder fieldDefinition, Type<?> parentClass, Type<?> attributeClass, String attribute,
            DataFetcher<?> fetcher, EntityDictionary entityDictionary) {
        A annotation = entityDictionary.getAttributeOrRelationAnnotation(parentClass, this.annotationClass, attribute);
        if (annotation != null) {
            String description = descriptionFunction.apply(annotation);
            if (description != null) {
                fieldDefinition.description(description);
            }
        }
    }
}
