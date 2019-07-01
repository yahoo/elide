/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.constraints;

import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import lombok.Data;

/**
 * Validates that both the field is indexed and the predicate values
 * fit within the NGram index upper and lower sizes.
 */
@Data
public class NGramConstraint extends IndexedFieldConstraint {

    private int maxNgram;
    private int minNgram;

    public NGramConstraint(int minNgram, int maxNgram, EntityDictionary dictionary) {
        super(dictionary);
        this.maxNgram = maxNgram;
        this.minNgram = minNgram;
    }

    @Override
    public DataStoreTransaction.FeatureSupport canSearch(Class<?> entityClass, FilterPredicate predicate)
            throws HttpStatusException {

        DataStoreTransaction.FeatureSupport fieldIndexSupport = super.canSearch(entityClass, predicate);

        boolean isIndexed = (fieldIndexSupport != DataStoreTransaction.FeatureSupport.NONE);
        Operator op = predicate.getOperator();

        boolean withinNGramBounds = predicate.getValues().stream()
                .map(Object::toString)
                .allMatch((value) -> {
                    return value.length() >= minNgram && value.length() <= maxNgram;
                });

        if (isIndexed && ! withinNGramBounds) {
            if (op == Operator.PREFIX || op == Operator.INFIX
                    || op == Operator.INFIX_CASE_INSENSITIVE || op == Operator.PREFIX_CASE_INSENSITIVE) {
                String message = String.format("Field values for %s on entity %s must be >= %d and <= %d",
                        predicate.getField(), dictionary.getJsonAliasFor(entityClass), minNgram, maxNgram);

                throw new InvalidValueException(predicate.getValues(), message);
            }
            fieldIndexSupport = DataStoreTransaction.FeatureSupport.NONE;
        }

        return fieldIndexSupport;
    }
}
