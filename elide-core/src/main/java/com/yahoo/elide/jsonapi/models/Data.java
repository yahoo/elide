/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.jsonapi.serialization.DataDeserializer;
import com.yahoo.elide.jsonapi.serialization.DataSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.ToString;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.Comparator;

/**
 * Container for different representations of top-level data in JSON API.
 *
 * @param <T> type
 */
@JsonSerialize(using = DataSerializer.class)
@JsonDeserialize(using = DataDeserializer.class)
@ToString
public class Data<T> {
    private Flux<T> values;
    private final RelationshipType relationshipType;

    /**
     * Constructor.
     *
     * @param value singleton resource
     */
    public Data(T value) {
        if (value == null) {
            this.values = Flux.empty();
        } else {
            this.values = Flux.just(value);
        }
        this.relationshipType = RelationshipType.MANY_TO_ONE; // Any "toOne"
    }

    /**
     * Constructor.
     *
     * @param values List of resources
     */
    public Data(Flux<T> values) {
        this(values, RelationshipType.MANY_TO_MANY);
    }

    /**
     * Constructor.
     *
     * @param values List of resources
     * @param relationshipType toOne or toMany
     */
    public Data(Flux<T> values, RelationshipType relationshipType) {
        this.values = values;
        this.relationshipType = relationshipType;
    }

    /**
     * Constructor.
     *
     * @param values List of resources
     */
    public Data(Collection<T> values) {
        this(values, RelationshipType.MANY_TO_MANY);
    }

    /**
     * Constructor.
     *
     * @param values List of resources
     * @param relationshipType toOne or toMany
     */
    public Data(Collection<T> values, RelationshipType relationshipType) {
        this.values = Flux.fromIterable(values);
        this.relationshipType = relationshipType;
    }

    /**
     * Sort method using provided sort function.
     *
     * @param sortFunction comparator to sort data with
     */
    public void sort(Comparator<T> sortFunction) {
        this.values = this.values.sort(sortFunction);
    }

    /**
     * Fetches the resources.
     *
     * @return the resources
     */
    public Collection<T> get() {
        return values.collectList().block();
    }

    /**
     * Determine whether or not the contained type is toOne.
     *
     * @return True if toOne, false if toMany
     */
    public boolean isToOne() {
        return relationshipType.isToOne();
    }

    /**
     * Fetch the item if the data is toOne.
     *
     * @return T if toOne
     * @throws IllegalAccessError when the data is not toOne
     */
    public T getSingleValue() {
        if (isToOne()) {
            return values.singleOrEmpty().block();
        }

        throw new IllegalAccessError("Data is not toOne");
    }

    /**
     * Gets the resource identifiers.
     *
     * @return the resource identifiers
     */
    public Collection<ResourceIdentifier> toResourceIdentifiers() {
        return values
                .map(object -> object != null ? ((Resource) object).toResourceIdentifier() : null)
                .collectList().block();
    }
}
