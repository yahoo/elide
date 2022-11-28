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
 * Elide Method that wraps a Java Method for a JPA entity.  If the method is a relationship with targetEntity set,
 * the type of the method is the targetEntity.
 */
@EqualsAndHashCode(callSuper = true)
public class EntityMethodType extends MethodType {

    Type<?> targetEntity = null;
    boolean toMany = false;

    public EntityMethodType(java.lang.reflect.Executable method) {
        super(method);

        Class<?> entityType = null;
        if (method.isAnnotationPresent(ManyToMany.class)) {
            entityType = method.getAnnotation(ManyToMany.class).targetEntity();
            toMany = true;
        } else if (method.isAnnotationPresent(OneToMany.class)) {
            entityType = method.getAnnotation(OneToMany.class).targetEntity();
            toMany = true;
        } else if (method.isAnnotationPresent(OneToOne.class)) {
            entityType = method.getAnnotation(OneToOne.class).targetEntity();
        } else if (method.isAnnotationPresent(ManyToOne.class)) {
            entityType = method.getAnnotation(ManyToOne.class).targetEntity();
        }

        targetEntity = entityType == null || entityType.equals(void.class) ? null : ClassType.of(entityType);
    }

    @Override
    public Type<?> getParameterizedReturnType(Type<?> parentType, Optional<Integer> index) {
        if (targetEntity != null) {
            return targetEntity;
        }

        return super.getParameterizedReturnType(parentType, index);
    }
}
