/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.core;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.core.EntityBinding;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * A binding for a view to store all fields and relationship in that view.
 */
public class ViewBinding extends EntityBinding {
    public ViewBinding(Class<?> cls, String name) {
        entityClass = cls;
        entityName = name;

        // Map id's, attributes, and relationships
        List<AccessibleObject> fieldOrMethodList = getAllFields();

        bindViewFields(fieldOrMethodList);

        attributes = dequeToList(attributesDeque);
        relationships = dequeToList(relationshipsDeque);
    }

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param fieldOrMethodList List of fields and methods on entity
     */
    private void bindViewFields(Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            if (!fieldOrMethod.isAnnotationPresent(Exclude.class)) {
                bindAttrOrRelation(fieldOrMethod);
            }
        }
    }

    /**
     * Bind an attribute or relationship.
     *
     * @param fieldOrMethod Field or method to bind
     */
    @Override
    protected void bindAttrOrRelation(AccessibleObject fieldOrMethod) {
        boolean isRelation = RELATIONSHIP_TYPES.stream().anyMatch(fieldOrMethod::isAnnotationPresent);

        String fieldName = getFieldName(fieldOrMethod);
        Class<?> fieldType = getFieldType(entityClass, fieldOrMethod);

        if (fieldName == null || "class".equals(fieldName) || OBJ_METHODS.contains(fieldOrMethod)) {
            return; // Reserved
        }

        if (fieldOrMethod instanceof Method) {
            Method method = (Method) fieldOrMethod;
            requestScopeableMethods.put(method, isRequestScopeableMethod(method));
        }

        if (isRelation) {
            bindRelation(fieldOrMethod, fieldName, fieldType);
        } else {
            bindAttr(fieldOrMethod, fieldName, fieldType);
        }
    }

    public ViewBinding() {

    }
}
