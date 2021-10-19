/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql.subscriptions.hooks;

import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import java.lang.annotation.Annotation;

/**
 * Gson exclusion strategy that only serializes ID fields and fields annotated
 * with SubscriptionField.
 */
public class SubscriptionExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {

        if (fieldAttributes.getAnnotation(SubscriptionField.class) != null) {
            return false;
        }

        for (Class<? extends Annotation> idAnnotation : EntityBinding.ID_ANNOTATIONS) {
            if (fieldAttributes.getAnnotation(idAnnotation) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}
