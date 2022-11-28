/*
 * Copyright 2022, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.type;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.EqualsAndHashCode;

import java.util.Optional;

/**
 * Elide field that wraps a Java field for a JPA entity.  If the field is a relationship with targetEntity set,
 * the type of the field is the targetEntity.
 */

@EqualsAndHashCode(callSuper = true)
public class EntityFieldType extends FieldType {

    Type<?> targetEntity = null;
    boolean toMany = false;

    public EntityFieldType(java.lang.reflect.Field field) {
        super(field);

        Class<?> entityType = null;
        if (field.isAnnotationPresent(ManyToMany.class)) {
            entityType = field.getAnnotation(ManyToMany.class).targetEntity();
            toMany = true;
        } else if (field.isAnnotationPresent(OneToMany.class)) {
            entityType = field.getAnnotation(OneToMany.class).targetEntity();
            toMany = true;
        } else if (field.isAnnotationPresent(OneToOne.class)) {
            entityType = field.getAnnotation(OneToOne.class).targetEntity();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            entityType = field.getAnnotation(ManyToOne.class).targetEntity();
        }
        targetEntity = entityType == null || entityType.equals(void.class) ? null : ClassType.of(entityType);
    }

    @Override
    public Type<?> getParameterizedType(Type<?> parentType, Optional<Integer> index) {
        if (targetEntity != null) {
                return targetEntity;
        }
        return super.getParameterizedType(parentType, index);
    }
}
