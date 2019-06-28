/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.filter.FilterPredicate;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constraint on whether or not the filter predicate field is indexed.
 */
@Data
@AllArgsConstructor
public class IndexedFieldConstraint implements SearchConstraint {

    protected EntityDictionary dictionary;

    @Override
    public DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        String fieldName = predicate.getField();

        List<Field> fields = new ArrayList<>();

        Field fieldAnnotation = dictionary.getAttributeOrRelationAnnotation(entityClass, Field.class, fieldName);

        if (fieldAnnotation != null) {
            fields.add(fieldAnnotation);
        } else {
            Fields fieldsAnnotation =
                    dictionary.getAttributeOrRelationAnnotation(entityClass, Fields.class, fieldName);

            if (fieldsAnnotation != null) {
                Arrays.stream(fieldsAnnotation.value()).forEach(fields::add);
            }
        }

        boolean indexed = false;

        for (Field field : fields) {
            if (field.index() == Index.YES && (field.name().equals(fieldName) || field.name().isEmpty())) {
                indexed = true;
            }
        }

        return (indexed ? DataStoreTransaction.FeatureSupport.FULL : DataStoreTransaction.FeatureSupport.NONE);
    }
}
