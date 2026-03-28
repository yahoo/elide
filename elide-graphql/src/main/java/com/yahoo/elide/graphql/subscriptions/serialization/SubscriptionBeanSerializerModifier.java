/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql.subscriptions.serialization;

import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ValueSerializerModifier} that only serializes ID fields and fields annotated
 * with SubscriptionField.
 */
public class SubscriptionBeanSerializerModifier extends ValueSerializerModifier {

    private static final long serialVersionUID = 1L;

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> result = new ArrayList<>();
        for (BeanPropertyWriter beanProperty : beanProperties) {
            if (isSubscriptionField(beanProperty)) {
                result.add(beanProperty);
            }
        }
        return result;
    }

    protected boolean isSubscriptionField(BeanPropertyWriter beanProperty) {
        if (beanProperty.getAnnotation(SubscriptionField.class) != null) {
            return true;
        }

        for (Class<? extends Annotation> idAnnotation : EntityBinding.ID_ANNOTATIONS) {
            if (beanProperty.getAnnotation(idAnnotation) != null) {
                return true;
            }
        }
        return false;
    }
}
