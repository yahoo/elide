/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.text.WordUtils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Transient;

/**
 * Entity Dictionary Element Binding.  For use by EntityDictionary.
 */
class EntityBinding {
    private final static List<Method> OBJ_METHODS = Arrays.asList(Object.class.getMethods());

    public final String jsonApi;
    public final ConcurrentLinkedDeque<String> attrsDeque;
    public final List<String> attrs;
    public final ConcurrentLinkedDeque<String> relationshipsDeque;
    public final List<String> relationships;
    public final ConcurrentHashMap<String, RelationshipType> relationshipTypes;
    public final ConcurrentHashMap<String, String> relationshipToInverse;
    public final ConcurrentHashMap<String, AccessibleObject> fieldsToValues;
    public final ConcurrentHashMap<String, String> aliasesToFields;
    @Getter private AccessibleObject idField;
    @Getter private AccessibleObject initializer;

    public final static EntityBinding EMPTY_BINDING = new EntityBinding();

    /* empty binding constructor */
    private EntityBinding() {
        jsonApi = null;
        idField = null;
        attrsDeque = null;
        attrs = null;
        relationshipsDeque = null;
        relationships = null;
        relationshipTypes = null;
        relationshipToInverse = null;
        fieldsToValues = null;
        aliasesToFields = null;
    }

    public EntityBinding(Class<?> cls, String type) {
        // Map id's, attributes, and relationships
        @SuppressWarnings("unchecked")
        Collection<AccessibleObject> fieldOrMethodList = CollectionUtils.union(
                Arrays.asList(cls.getFields()),
                Arrays.asList(cls.getMethods()));

        jsonApi = type;
        // Initialize our maps for this entity. Duplicates are checked above.
        attrsDeque = new ConcurrentLinkedDeque<>();
        relationshipsDeque = new ConcurrentLinkedDeque<>();
        relationshipTypes = new ConcurrentHashMap<>();
        relationshipToInverse = new ConcurrentHashMap<>();
        fieldsToValues = new ConcurrentHashMap<>();
        aliasesToFields = new ConcurrentHashMap<>();
        bindEntityFields(cls, type, fieldOrMethodList);

        attrs = dequeToList(attrsDeque);
        relationships = dequeToList(relationshipsDeque);
    }

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param cls Class type to bind fields
     * @param type JSON API type identifier
     * @param fieldOrMethodList List of fields and methods on entity
     */
    private void bindEntityFields(Class<?> cls, String type, Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            if (fieldOrMethod.isAnnotationPresent(PrePersist.class)) {
                initializer = fieldOrMethod;
            }
            if (fieldOrMethod.isAnnotationPresent(Id.class)) {
                bindEntityId(cls, type, fieldOrMethod);
            } else if (fieldOrMethod.isAnnotationPresent(Transient.class)) {
                continue; // Transient. Don't serialize
            } else if (!fieldOrMethod.isAnnotationPresent(Exclude.class)) {
                if (fieldOrMethod instanceof Field && Modifier.isTransient(((Field) fieldOrMethod).getModifiers())) {
                    continue; // Transient. Don't serialize
                }
                if (fieldOrMethod instanceof Method && Modifier.isTransient(((Method) fieldOrMethod).getModifiers())) {
                    continue; // Transient. Don't serialize
                }
                if (fieldOrMethod instanceof Field
                        && !fieldOrMethod.isAnnotationPresent(Column.class)
                        && Modifier.isStatic(((Field) fieldOrMethod).getModifiers())) {
                    continue; // Field must have Column annotation?
                }
                bindAttrOrRelation(cls, fieldOrMethod);
            }
        }
    }

    /**
     * Bind an id field to an entity
     *
     * @param cls Class type to bind fields
     * @param type JSON API type identifier
     * @param fieldOrMethod Field or method to bind
     */
    private void bindEntityId(Class<?> cls, String type, AccessibleObject fieldOrMethod) {
        if (idField != null && !fieldOrMethod.equals(idField)) {
            String name;
            if (fieldOrMethod instanceof Field) {
                name = ((Field) fieldOrMethod).getName();
            } else {
                name = ((Method) fieldOrMethod).getName();
            }
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + name);
        }
        idField = fieldOrMethod;
    }

    /**
     * Convert a deque to a list
     *
     * @param deque Deque to convert
     * @return Deque as a list
     */
    private static List<String> dequeToList(final Deque<String> deque) {
        ArrayList<String> result = new ArrayList<>();
        deque.stream().forEachOrdered(result::add);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(result);
    }

    /**
     * Bind an attribute or relationship
     *
     * @param cls Class type to bind fields
     * @param fieldOrMethod Field or method to bind
     */
    private void bindAttrOrRelation(Class<?> cls, AccessibleObject fieldOrMethod) {
        boolean manyToMany = fieldOrMethod.isAnnotationPresent(ManyToMany.class);
        boolean manyToOne = fieldOrMethod.isAnnotationPresent(ManyToOne.class);
        boolean oneToMany = fieldOrMethod.isAnnotationPresent(OneToMany.class);
        boolean oneToOne = fieldOrMethod.isAnnotationPresent(OneToOne.class);
        boolean isRelation = manyToMany || manyToOne || oneToMany || oneToOne;

        String name;
        if (fieldOrMethod instanceof Field) {
            name = ((Field) fieldOrMethod).getName();
        } else {
            Method method = ((Method) fieldOrMethod);
            name = method.getName();
            if (name.startsWith("get") && method.getParameterCount() == 0) {
                name = WordUtils.uncapitalize(name.substring("get".length()));
            } else if (name.startsWith("is") && method.getParameterCount() == 0) {
                name = WordUtils.uncapitalize(name.substring("is".length()));
            } else {
                return;
            }
            if (name.equals("id") || name.equals("class") || OBJ_METHODS.contains(fieldOrMethod)) {
                return; // Reserved. Not attributes.
            }
        }

        ConcurrentLinkedDeque<String> fieldList;
        if (isRelation) {
            fieldList = relationshipsDeque;
            RelationshipType type;
            String mappedBy;
            if (oneToMany) {
                type = RelationshipType.ONE_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(OneToMany.class).mappedBy();
            } else if (oneToOne) {
                type = RelationshipType.ONE_TO_ONE;
                mappedBy = fieldOrMethod.getAnnotation(OneToOne.class).mappedBy();
            } else if (manyToMany) {
                type = RelationshipType.MANY_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(ManyToMany.class).mappedBy();
            } else if (manyToOne) {
                type = RelationshipType.MANY_TO_ONE;
                mappedBy = "";
            } else {
                type = RelationshipType.NONE;
                mappedBy = "";
            }
            relationshipTypes.put(name, type);
            relationshipToInverse.put(name, mappedBy);
        } else {
            fieldList = attrsDeque;
        }

        fieldList.push(name);

        fieldsToValues.put(name, fieldOrMethod);
    }
};
