/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.yahoo.elide.core.RelationshipType;
import com.yahoo.elide.jsonapi.serialization.DataDeserializer;
import com.yahoo.elide.jsonapi.serialization.DataSerializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for different representations of top-level data in JSON API.
 *
 * @param <T> type
 */
@JsonSerialize(using = DataSerializer.class)
@JsonDeserialize(using = DataDeserializer.class)
@ToString
public class Data<T> {
    private final Collection<T> values;
    private final RelationshipType relationshipType;

    /**
     * Constructor.
     *
     * @param value singleton resource
     */
    public Data(T value) {
        this.values = new SingleElementSet<>(value);
        this.relationshipType = RelationshipType.MANY_TO_ONE; // Any "toOne"
    }

    /**
     * Constructor.
     *
     * @param values List of resources
     */
    public Data(Collection<T> values) {
        this.values = values;
        this.relationshipType = RelationshipType.MANY_TO_MANY; // Any "toMany"
    }

    /**
     * Sort method using provided sort function.
     *
     * @param sortFunction comparator to sort data with
     */
    public void sort(Comparator<T> sortFunction) {
        if (values instanceof List) {
            ((List<T>) values).sort(sortFunction);
        } else {
            ArrayList<T> sortedList = new ArrayList<>(values);
            sortedList.sort(sortFunction);
            values.clear();
            values.addAll(sortedList);
        }
    }

    public Collection<T> get() {
        return values;
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
            return ((SingleElementSet<T>) values).getValue();
        }

        throw new IllegalAccessError("Data is not toOne");
    }

    @SuppressWarnings("unchecked")
    public Collection<ResourceIdentifier> toResourceIdentifiers() {
        return ((Collection<Resource>) get()).stream()
                                             .map(object -> object != null ? object.toResourceIdentifier() : null)
                                             .collect(Collectors.toList());
    }
}
