/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
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
import java.util.stream.Stream;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see com.yahoo.elide.annotation.Include#type
 */
class EntityBinding {

    private static final List<Method> OBJ_METHODS = Arrays.asList(Object.class.getMethods());

    public final Class<?> entityClass;
    public final String jsonApiType;
    @Getter private AccessibleObject idField;
    @Getter private String idFieldName;
    @Getter private Class<?> idType;
    @Getter @Setter private Initializer initializer;

    public final EntityPermissions entityPermissions;
    public final List<String> attributes;
    public final List<String> relationships;
    public final ConcurrentLinkedDeque<String> attributesDeque = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<String> relationshipsDeque = new ConcurrentLinkedDeque<>();

    public final ConcurrentHashMap<String, RelationshipType> relationshipTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> relationshipToInverse = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, CascadeType[]> relationshipToCascadeTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, AccessibleObject> fieldsToValues = new ConcurrentHashMap<>();
    public final MultiValuedMap<Pair<Class, String>, Method> fieldsToTriggers = new HashSetValuedHashMap<>();
    public final ConcurrentHashMap<String, Class<?>> fieldsToTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> aliasesToFields = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Method, Boolean> requestScopeableMethods = new ConcurrentHashMap<>();

    public final ConcurrentHashMap<Class<? extends Annotation>, Annotation> annotations = new ConcurrentHashMap<>();

    public static final EntityBinding EMPTY_BINDING = new EntityBinding();

    /* empty binding constructor */
    private EntityBinding() {
        jsonApiType = null;
        idField = null;
        idType = null;
        attributes = null;
        relationships = null;
        entityClass = null;
        entityPermissions = EntityPermissions.EMPTY_PERMISSIONS;
    }

    public EntityBinding(EntityDictionary dictionary, Class<?> cls, String type) {
        entityClass = cls;
        jsonApiType = type;

        // Map id's, attributes, and relationships
        List<AccessibleObject> fieldOrMethodList = new ArrayList<>();
        fieldOrMethodList.addAll(Arrays.asList(cls.getFields()));
        fieldOrMethodList.addAll(Arrays.asList(cls.getMethods()));

        bindEntityFields(cls, type, fieldOrMethodList);

        attributes = dequeToList(attributesDeque);
        relationships = dequeToList(relationshipsDeque);
        entityPermissions = new EntityPermissions(dictionary, cls, fieldOrMethodList);
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
            bindTriggerIfPresent(OnCreatePreSecurity.class, fieldOrMethod);
            bindTriggerIfPresent(OnDeletePreSecurity.class, fieldOrMethod);
            bindTriggerIfPresent(OnUpdatePreSecurity.class, fieldOrMethod);
            bindTriggerIfPresent(OnReadPreSecurity.class, fieldOrMethod);
            bindTriggerIfPresent(OnCreatePreCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnDeletePreCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnUpdatePreCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnReadPreCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnCreatePostCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnDeletePostCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnUpdatePostCommit.class, fieldOrMethod);
            bindTriggerIfPresent(OnReadPostCommit.class, fieldOrMethod);

            if (fieldOrMethod.isAnnotationPresent(Id.class)) {
                bindEntityId(cls, type, fieldOrMethod);
            } else if (fieldOrMethod.isAnnotationPresent(Transient.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedAttribute.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedRelationship.class)) {
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
     * Bind an id field to an entity.
     *
     * @param cls Class type to bind fields
     * @param type JSON API type identifier
     * @param fieldOrMethod Field or method to bind
     */
    private void bindEntityId(Class<?> cls, String type, AccessibleObject fieldOrMethod) {
        String fieldName = getFieldName(fieldOrMethod);
        Class<?> fieldType = getFieldType(fieldOrMethod);

        //Add id field to type map for the entity
        fieldsToTypes.put(fieldName, fieldType);

        //Set id field, type, and name
        idField = fieldOrMethod;
        idType = fieldType;
        idFieldName = fieldName;

        fieldsToValues.put(fieldName, fieldOrMethod);

        if (idField != null && !fieldOrMethod.equals(idField)) {
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + fieldName);
        }
    }

    /**
     * Convert a deque to a list.
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
     * Bind an attribute or relationship.
     *
     * @param cls Class type to bind fields
     * @param fieldOrMethod Field or method to bind
     */
    private void bindAttrOrRelation(Class<?> cls, AccessibleObject fieldOrMethod) {
        boolean manyToMany = fieldOrMethod.isAnnotationPresent(ManyToMany.class);
        boolean manyToOne = fieldOrMethod.isAnnotationPresent(ManyToOne.class);
        boolean oneToMany = fieldOrMethod.isAnnotationPresent(OneToMany.class);
        boolean oneToOne = fieldOrMethod.isAnnotationPresent(OneToOne.class);
        boolean computedRelationship = fieldOrMethod.isAnnotationPresent(ComputedRelationship.class);
        boolean isRelation = manyToMany || manyToOne || oneToMany || oneToOne;

        String fieldName = getFieldName(fieldOrMethod);

        if (fieldName == null || fieldName.equals("id")
                || fieldName.equals("class") || OBJ_METHODS.contains(fieldOrMethod)) {
            return; // Reserved. Not attributes.
        }

        if (fieldOrMethod instanceof Method) {
            Method method = (Method) fieldOrMethod;
            requestScopeableMethods.put(method, isRequestScopeableMethod(method));
        }

        Class<?> fieldType = getFieldType(fieldOrMethod);

        ConcurrentLinkedDeque<String> fieldList;
        if (isRelation) {
            fieldList = relationshipsDeque;
            RelationshipType type;
            String mappedBy;
            CascadeType [] cascadeTypes;
            if (oneToMany) {
                type = computedRelationship ? RelationshipType.COMPUTED_ONE_TO_MANY : RelationshipType.ONE_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(OneToMany.class).mappedBy();
                cascadeTypes = fieldOrMethod.getAnnotation(OneToMany.class).cascade();
            } else if (oneToOne) {
                type = computedRelationship ? RelationshipType.COMPUTED_ONE_TO_ONE : RelationshipType.ONE_TO_ONE;
                mappedBy = fieldOrMethod.getAnnotation(OneToOne.class).mappedBy();
                cascadeTypes = fieldOrMethod.getAnnotation(OneToOne.class).cascade();
            } else if (manyToMany) {
                type = computedRelationship ? RelationshipType.COMPUTED_MANY_TO_MANY : RelationshipType.MANY_TO_MANY;
                mappedBy = fieldOrMethod.getAnnotation(ManyToMany.class).mappedBy();
                cascadeTypes = fieldOrMethod.getAnnotation(ManyToMany.class).cascade();
            } else if (manyToOne) {
                type = computedRelationship ? RelationshipType.COMPUTED_MANY_TO_ONE : RelationshipType.MANY_TO_ONE;
                mappedBy = "";
                cascadeTypes = fieldOrMethod.getAnnotation(ManyToOne.class).cascade();
            } else {
                type = computedRelationship ? RelationshipType.COMPUTED_NONE : RelationshipType.NONE;
                mappedBy = "";
                cascadeTypes = new CascadeType[0];
            }
            relationshipTypes.put(fieldName, type);
            relationshipToInverse.put(fieldName, mappedBy);
            relationshipToCascadeTypes.put(fieldName, cascadeTypes);
        } else {
            fieldList = attributesDeque;
        }

        fieldList.push(fieldName);
        fieldsToValues.put(fieldName, fieldOrMethod);
        fieldsToTypes.put(fieldName, fieldType);
    }

    /**
     * Returns name of field whether public member or method.
     *
     * @param fieldOrMethod field or method
     * @return field or method name
     */
    public static String getFieldName(AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Field) {
            return ((Field) fieldOrMethod).getName();
        } else {
            Method method = (Method) fieldOrMethod;
            String name = method.getName();
            boolean hasValidParameterCount = method.getParameterCount() == 0
                    || isRequestScopeableMethod((Method) fieldOrMethod);

            if (name.startsWith("get") && hasValidParameterCount) {
                name = WordUtils.uncapitalize(name.substring("get".length()));
            } else if (name.startsWith("is") && hasValidParameterCount) {
                name = WordUtils.uncapitalize(name.substring("is".length()));
            } else {
                return null;
            }
            return name;
        }
    }

    /**
     * Check whether or not method expects a RequestScope.
     *
     * @param method  Method to check
     * @return True if accepts a RequestScope, false otherwise
     */
    public static boolean isRequestScopeableMethod(Method method) {
        return isComputedMethod(method) && method.getParameterCount() == 1
                && com.yahoo.elide.security.RequestScope.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    /**
     * Check whether or not the provided method described a computed attribute or relationship.
     *
     * @param method  Method to check
     * @return True if method is a computed type, false otherwise
     */
    public static boolean isComputedMethod(Method method) {
        return Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(c -> ComputedAttribute.class == c || ComputedRelationship.class == c);
    }

    /**
     * Returns type of field whether public member or method.
     *
     * @param fieldOrMethod field or method
     * @return field type
     */
    private static Class<?> getFieldType(AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Field) {
            return ((Field) fieldOrMethod).getType();
        } else {
            return ((Method) fieldOrMethod).getReturnType();
        }
    }

    private void bindTriggerIfPresent(Class<? extends Annotation> annotationClass, AccessibleObject fieldOrMethod) {
        if (fieldOrMethod instanceof Method && fieldOrMethod.isAnnotationPresent(annotationClass)) {
            Annotation trigger = fieldOrMethod.getAnnotation(annotationClass);
            String value;
            try {
                value = (String) annotationClass.getMethod("value").invoke(trigger);
            } catch (ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
                value = "";
            }
            fieldsToTriggers.put(Pair.of(annotationClass, value), (Method) fieldOrMethod);
        }
    }

    public <A extends Annotation> Collection<Method> getTriggers(Class<A> annotationClass, String fieldName) {
        Collection<Method> methods = fieldsToTriggers.get(Pair.of(annotationClass, fieldName));
        return methods == null ? Collections.emptyList() : methods;
    }

    /**
     * Cache placeholder for no annotation.
     */
    private static final Annotation NO_ANNOTATION = new Annotation() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    };

    /**
     * Return annotation from class, parents or package.
     *
     * @param annotationClass the annotation class
     * @param <A> annotation type
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Annotation annotation = annotations.get(annotationClass);
        if (annotation == null) {
            annotation = EntityDictionary.getFirstAnnotation(entityClass, Collections.singletonList(annotationClass));
            if (annotation == null) {
                annotation = NO_ANNOTATION;
            }
            annotations.putIfAbsent(annotationClass, annotation);
        }
        return annotation == NO_ANNOTATION ? null : (A) annotation;
    }
}
