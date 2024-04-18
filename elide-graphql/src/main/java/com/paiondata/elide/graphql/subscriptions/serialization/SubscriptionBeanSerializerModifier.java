/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.subscriptions.serialization;

import com.paiondata.elide.core.dictionary.EntityBinding;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link BeanSerializerModifier} that only serializes ID fields and fields annotated
 * with SubscriptionField.
 */
public class SubscriptionBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
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
