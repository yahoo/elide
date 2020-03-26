/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.EntityDictionary.REGULAR_ID_NAME;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.ToMany;
import com.yahoo.elide.annotation.ToOne;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.functions.LifeCycleHook;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.Getter;
import lombok.Setter;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see com.yahoo.elide.annotation.Include#type
 */
public class EntityBinding {

    private static final List<Method> OBJ_METHODS = ImmutableList.copyOf(Object.class.getMethods());
    private static final List<Class<? extends Annotation>> RELATIONSHIP_TYPES =
            Arrays.asList(ManyToMany.class, ManyToOne.class, OneToMany.class, OneToOne.class,
                    ToOne.class, ToMany.class);

    public final Class<?> entityClass;
    public final String jsonApiType;
    public final String entityName;
    @Getter
    public boolean idGenerated;
    @Getter
    private AccessibleObject idField;
    @Getter
    private String idFieldName;
    @Getter
    private Class<?> idType;
    @Getter
    @Setter
    private Initializer initializer;
    @Getter
    private AccessType accessType;

    public final EntityPermissions entityPermissions;
    public final List<String> attributes;
    public final List<String> relationships;
    public final List<Class<?>> inheritedTypes;
    public final ConcurrentLinkedDeque<String> attributesDeque = new ConcurrentLinkedDeque<>();
    public final ConcurrentLinkedDeque<String> relationshipsDeque = new ConcurrentLinkedDeque<>();

    public final ConcurrentHashMap<String, RelationshipType> relationshipTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> relationshipToInverse = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, CascadeType[]> relationshipToCascadeTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, AccessibleObject> fieldsToValues = new ConcurrentHashMap<>();
    public final MultiValuedMap<Pair<Class, String>, LifeCycleHook> fieldsToTriggers = new HashSetValuedHashMap<>();
    public final MultiValuedMap<Class, LifeCycleHook> classToTriggers = new HashSetValuedHashMap<>();
    public final ConcurrentHashMap<String, Class<?>> fieldsToTypes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, String> aliasesToFields = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Method, Boolean> requestScopeableMethods = new ConcurrentHashMap<>();

    public final ConcurrentHashMap<Class<? extends Annotation>, Annotation> annotations = new ConcurrentHashMap<>();

    public static final EntityBinding EMPTY_BINDING = new EntityBinding();
    private static final String ALL_FIELDS = "*";

    /* empty binding constructor */
    private EntityBinding() {
        jsonApiType = null;
        entityName = null;
        attributes = new ArrayList<>();
        relationships = new ArrayList<>();
        inheritedTypes = new ArrayList<>();
        idField = null;
        idType = null;
        entityClass = null;
        entityPermissions = EntityPermissions.EMPTY_PERMISSIONS;
        idGenerated = false;
    }

    public EntityBinding(EntityDictionary dictionary, Class<?> cls, String type, String name) {
        entityClass = cls;
        jsonApiType = type;
        entityName = name;
        inheritedTypes = getInheritedTypes(cls);

        // Map id's, attributes, and relationships
        List<AccessibleObject> fieldOrMethodList = getAllFields();

        if (fieldOrMethodList.stream().anyMatch(field -> field.isAnnotationPresent(Id.class))) {
            accessType = AccessType.FIELD;

            /* Add all public methods that are computed OR life cycle hooks */
            fieldOrMethodList.addAll(
                    getInstanceMembers(cls.getMethods(),
                            (method) -> method.isAnnotationPresent(ComputedAttribute.class)
                                    || method.isAnnotationPresent(ComputedRelationship.class)
                                    || method.isAnnotationPresent(OnReadPreSecurity.class)
                                    || method.isAnnotationPresent(OnReadPreCommit.class)
                                    || method.isAnnotationPresent(OnReadPostCommit.class)
                                    || method.isAnnotationPresent(OnUpdatePreSecurity.class)
                                    || method.isAnnotationPresent(OnUpdatePreCommit.class)
                                    || method.isAnnotationPresent(OnUpdatePostCommit.class)
                                    || method.isAnnotationPresent(OnCreatePreSecurity.class)
                                    || method.isAnnotationPresent(OnCreatePreCommit.class)
                                    || method.isAnnotationPresent(OnCreatePostCommit.class)
                                    || method.isAnnotationPresent(OnDeletePreSecurity.class)
                                    || method.isAnnotationPresent(OnDeletePreCommit.class)
                                    || method.isAnnotationPresent(OnDeletePostCommit.class)
                    )
            );

            //Elide needs to manipulate private fields that are exposed.
            fieldOrMethodList.forEach(field -> field.setAccessible(true));
        } else {
            /* Preserve the behavior of Elide 4.2.6 and earlier */
            accessType = AccessType.PROPERTY;

            fieldOrMethodList.clear();

            /* Add all public fields */
            fieldOrMethodList.addAll(getInstanceMembers(cls.getFields()));

            /* Add all public methods */
            fieldOrMethodList.addAll(getInstanceMembers(cls.getMethods()));
        }

        bindEntityFields(cls, type, fieldOrMethodList);

        attributes = dequeToList(attributesDeque);
        relationships = dequeToList(relationshipsDeque);
        entityPermissions = new EntityPermissions(dictionary, cls, fieldOrMethodList);
    }

    /**
     * Filters a list of class Members to instance methods & fields.
     *
     * @param objects
     * @param <T>
     * @return A list of the filtered members
     */
    private <T extends Member> List<T> getInstanceMembers(T[] objects) {
        return getInstanceMembers(objects, o -> true);
    }

    /**
     * Filters a list of class Members to instance methods & fields.
     *
     * @param objects    The list of Members to filter
     * @param <T>        Concrete Member Type
     * @param filteredBy An additional filter predicate to apply
     * @return A list of the filtered members
     */
    private <T extends Member> List<T> getInstanceMembers(T[] objects, Predicate<T> filteredBy) {
        return Arrays.stream(objects)
                .filter(o -> !Modifier.isStatic(o.getModifiers()))
                .filter(filteredBy)
                .collect(Collectors.toList());
    }

    /**
     * Get all fields of the entity class, including fields of superclasses (excluding Object).
     * @return All fields of the EntityBindings entity class and all superclasses (excluding Object)
     */
    public List<AccessibleObject> getAllFields() {
        List<AccessibleObject> fields = new ArrayList<>();

        fields.addAll(getInstanceMembers(entityClass.getDeclaredFields(), (field) -> !field.isSynthetic()));
        for (Class<?> type : inheritedTypes) {
            fields.addAll(getInstanceMembers(type.getDeclaredFields(), (field) -> !field.isSynthetic()));
        }

        return fields;
    }

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param cls               Class type to bind fields
     * @param type              JSON API type identifier
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
                bindAttrOrRelation(fieldOrMethod);
            }
        }
    }

    /**
     * Bind an id field to an entity.
     *
     * @param cls           Class type to bind fields
     * @param type          JSON API type identifier
     * @param fieldOrMethod Field or method to bind
     */
    private void bindEntityId(Class<?> cls, String type, AccessibleObject fieldOrMethod) {
        String fieldName = getFieldName(fieldOrMethod);
        Class<?> fieldType = getFieldType(cls, fieldOrMethod);

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
        if (fieldOrMethod.isAnnotationPresent(GeneratedValue.class)) {
            idGenerated = true;
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
     * @param fieldOrMethod Field or method to bind
     */
    private void bindAttrOrRelation(AccessibleObject fieldOrMethod) {
        boolean isRelation = RELATIONSHIP_TYPES.stream().anyMatch(fieldOrMethod::isAnnotationPresent);

        String fieldName = getFieldName(fieldOrMethod);
        Class<?> fieldType = getFieldType(entityClass, fieldOrMethod);

        if (fieldName == null || REGULAR_ID_NAME.equals(fieldName) || "class".equals(fieldName)
                || OBJ_METHODS.contains(fieldOrMethod)) {
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

    private void bindRelation(AccessibleObject fieldOrMethod, String fieldName, Class<?> fieldType) {
        boolean manyToMany = fieldOrMethod.isAnnotationPresent(ManyToMany.class);
        boolean manyToOne = fieldOrMethod.isAnnotationPresent(ManyToOne.class);
        boolean oneToMany = fieldOrMethod.isAnnotationPresent(OneToMany.class);
        boolean oneToOne = fieldOrMethod.isAnnotationPresent(OneToOne.class);
        boolean toOne = fieldOrMethod.isAnnotationPresent(ToOne.class);
        boolean toMany = fieldOrMethod.isAnnotationPresent(ToMany.class);
        boolean computedRelationship = fieldOrMethod.isAnnotationPresent(ComputedRelationship.class);

        if (fieldOrMethod.isAnnotationPresent(MapsId.class)) {
            idGenerated = true;
        }

        RelationshipType type;
        String mappedBy = "";
        CascadeType[] cascadeTypes = new CascadeType[0];
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
            cascadeTypes = fieldOrMethod.getAnnotation(ManyToOne.class).cascade();
        } else if (toOne) {
            type = RelationshipType.COMPUTED_ONE_TO_ONE;
        } else if (toMany) {
            type = RelationshipType.COMPUTED_ONE_TO_MANY;
        } else {
            type = computedRelationship ? RelationshipType.COMPUTED_NONE : RelationshipType.NONE;
        }
        relationshipTypes.put(fieldName, type);
        relationshipToInverse.put(fieldName, mappedBy);
        relationshipToCascadeTypes.put(fieldName, cascadeTypes);

        relationshipsDeque.push(fieldName);
        fieldsToValues.put(fieldName, fieldOrMethod);
        fieldsToTypes.put(fieldName, fieldType);
    }

    private void bindAttr(AccessibleObject fieldOrMethod, String fieldName, Class<?> fieldType) {
        attributesDeque.push(fieldName);
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
        }
        Method method = (Method) fieldOrMethod;
        String name = method.getName();
        boolean hasValidParameterCount = method.getParameterCount() == 0 || isRequestScopeableMethod(method);

        if (name.startsWith("get") && hasValidParameterCount) {
            return StringUtils.uncapitalize(name.substring("get".length()));
        }
        if (name.startsWith("is") && hasValidParameterCount) {
            return StringUtils.uncapitalize(name.substring("is".length()));
        }
        return null;
    }

    /**
     * Check whether or not method expects a RequestScope.
     *
     * @param method Method to check
     * @return True if accepts a RequestScope, false otherwise
     */
    public static boolean isRequestScopeableMethod(Method method) {
        return isComputedMethod(method) && method.getParameterCount() == 1
                && com.yahoo.elide.security.RequestScope.class.isAssignableFrom(method.getParameterTypes()[0]);
    }

    /**
     * Check whether or not the provided method described a computed attribute or relationship.
     *
     * @param method Method to check
     * @return True if method is a computed type, false otherwise
     */
    public static boolean isComputedMethod(Method method) {
        return Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .anyMatch(c -> ComputedAttribute.class == c || ComputedRelationship.class == c);
    }

    /**
     * Returns type of field whether member or method.
     *
     * @param parentClass The class which owns the given field or method
     * @param fieldOrMethod field or method
     * @return field type
     */
    public static Class<?> getFieldType(Class<?> parentClass,
                                         AccessibleObject fieldOrMethod) {
        return getFieldType(parentClass, fieldOrMethod, Optional.empty());
    }

    /**
     * Returns type of field whether member or method.
     *
     * @param parentClass The class which owns the given field or method
     * @param fieldOrMethod field or method
     * @param index Optional parameter index for parameterized types that take one or more parameters.  If
     *              an index is provided, the type returned is the parameter type.  Otherwise it is the
     *              parameterized type.
     * @return field type
     */
    public static Class<?> getFieldType(Class<?> parentClass,
                                         AccessibleObject fieldOrMethod,
                                         Optional<Integer> index) {
        Type type;
        if (fieldOrMethod instanceof Field) {
            type = ((Field) fieldOrMethod).getGenericType();
        } else {
            type = ((Method) fieldOrMethod).getGenericReturnType();
        }

        if (type instanceof ParameterizedType && index.isPresent()) {
            type = ((ParameterizedType) type).getActualTypeArguments()[index.get().intValue()];
        }

        return TypeUtils.getRawType(type, parentClass);
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

            Method method = (Method) fieldOrMethod;

            int paramCount = method.getParameterCount();
            Class<?>[] paramTypes = method.getParameterTypes();

            LifeCycleHook callback = (entity, scope, changes) -> {
                try {
                    if (changes.isPresent() && paramCount == 2
                            && paramTypes[0].isInstance(scope)
                            && paramTypes[1].isInstance(changes.get())) {
                        method.invoke(entity, scope, changes.get());
                    } else if (paramCount == 1 && paramTypes[0].isInstance(scope)) {
                        method.invoke(entity, scope);
                    } else if (paramCount == 0) {
                        method.invoke(entity);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } catch (ReflectiveOperationException e) {
                    Throwables.propagateIfPossible(e.getCause());
                    throw new IllegalArgumentException(e);
                }
            };

            if (value.equals(ALL_FIELDS)) {
                bindTrigger(annotationClass, callback);
            } else {
                bindTrigger(annotationClass, value, callback);
            }
        }
    }

    public void bindTrigger(Class<? extends Annotation> annotationClass,
                            String fieldOrMethodName,
                            LifeCycleHook callback) {
        fieldsToTriggers.put(Pair.of(annotationClass, fieldOrMethodName), callback);
    }

    public void bindTrigger(Class<? extends Annotation> annotationClass,
                            LifeCycleHook callback) {
        classToTriggers.put(annotationClass, callback);
    }


    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Class<A> annotationClass, String fieldName) {
        Collection<LifeCycleHook> methods = fieldsToTriggers.get(Pair.of(annotationClass, fieldName));
        return methods == null ? Collections.emptyList() : methods;
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Class<A> annotationClass) {
        Collection<LifeCycleHook> methods = classToTriggers.get(annotationClass);
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
     * @param <A>             annotation type
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        Annotation annotation = annotations.computeIfAbsent(annotationClass, cls -> Optional.ofNullable(
                EntityDictionary.getFirstAnnotation(entityClass, Collections.singletonList(annotationClass)))
                .orElse(NO_ANNOTATION));
        return annotation == NO_ANNOTATION ? null : annotationClass.cast(annotation);
    }

    private List<Class<?>> getInheritedTypes(Class<?> entityClass) {
        ArrayList<Class<?>> results = new ArrayList<>();

        for (Class<?> cls = entityClass.getSuperclass(); cls != Object.class; cls = cls.getSuperclass()) {
            results.add(cls);
        }

        return results;
    }
}
