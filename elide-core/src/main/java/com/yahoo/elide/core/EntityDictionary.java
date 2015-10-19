/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.WordUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.Entity;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see Include#type
 */
@Slf4j
@SuppressWarnings("static-method")
public class EntityDictionary {

    private final static List<Method> OBJ_METHODS = Arrays.asList(Object.class.getMethods());

    protected final ConcurrentHashMap<String, Class<?>> bindJsonApiToEntity = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Class<?>, EntityBinding> entityBindings = new ConcurrentHashMap<>();
    protected final CopyOnWriteArrayList<Class<?>>  bindEntityRoots = new CopyOnWriteArrayList<>();

    /**
     * Instantiates a new Entity dictionary.
     */
    public EntityDictionary() {
        // Do nothing
    }

    public EntityBinding entity(Class<?> entityClass) {
        EntityBinding entityBinding = entityBindings.get(lookupEntityClass(entityClass));
        return entityBinding == null ? EntityBinding.EMPTY_BINDING : entityBinding;
    }

    /**
     * Returns the binding class for a given entity name
     *
     * @param entityName entity name
     * @return binding class
     */
    public Class<?> getBinding(String entityName) {
        return bindJsonApiToEntity.get(entityName);
    }

    /**
     * Returns the entity name for a given binding class
     *
     * @param entityClass the entity class
     * @return binding class
     */
    public String getBinding(Class<?> entityClass) {
        return entity(entityClass).jsonApi;
    }

    /**
     * Get the list of attribute names for an entity
     *
     * @param entityClass entity name
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Class<?> entityClass) {
        return entity(entityClass).attrs;
    }

    /**
     * Get the list of attribute names for an entity
     *
     * @param entity entity instance
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Object entity) {
        return getAttributes(entity.getClass());
    }

    /**
     * Get the list of relationship names for an entity
     *
     * @param entityClass entity name
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Class<?> entityClass) {
        return entity(entityClass).relationships;
    }

    /**
     * Get the list of relationship names for an entity
     *
     * @param entity entity instnace
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Object entity) {
        return getRelationships(entity.getClass());
    }

    /**
     * Get the type of relationship from a relation
     *
     * @param cls Entity class
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Class<?> cls, String relation) {
        ConcurrentHashMap<String, RelationshipType> types = entity(cls).relationshipTypes;
        if (types == null) {
            return RelationshipType.NONE;
        }
        RelationshipType type = types.get(relation);
        return (type == null) ? RelationshipType.NONE : type;
    }

    /**
     * If a relationship is bidirectional, returns the name of the peer relationship in the peer entity.
     * @param cls the cls
     * @param relation the relation
     * @return relation inverse
     */
    public String getRelationInverse(Class<?> cls, String relation) {
        ConcurrentHashMap<String, String> mappings = entity(cls).relationshipToInverse;
        if (mappings != null) {
            String mapping = mappings.get(relation);

            if (mapping != null && !mapping.equals("")) {
                return mapping;
            }
        }

        /*
         * This could be the owning side of the relation.  Let's see if the entity referenced in the relation
         * has a bidirectional reference that is mapped to the given relation.
         */
        Class<?> inverseType = getParameterizedType(cls, relation);
        ConcurrentHashMap<String, String> inverseMappings =
                entity(inverseType).relationshipToInverse;

        for (Map.Entry<String, String> inverseMapping: inverseMappings.entrySet()) {
            String inverseRelationName = inverseMapping.getKey();
            String inverseMappedBy = inverseMapping.getValue();

            if (relation.equals(inverseMappedBy)
                    && getParameterizedType(inverseType, inverseRelationName).equals(lookupEntityClass(cls))) {
                return inverseRelationName;
            }

        }
        return "";
    }

    /**
     * Get the type of relationship from a relation
     *
     * @param entity Entity instance
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Object entity, String relation) {
        return getRelationshipType(entity.getClass(), relation);
    }

    /**
     * Get a type for a field on an entity
     *
     * @param entityClass Entity class
     * @param identifier Field to lookup type
     * @return Type of entity
     */
    public Class<?> getType(Class<?> entityClass, String identifier) {
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods = entity(entityClass).fieldsToValues;
        if (fieldOrMethods == null) {
            return null;
        }
        AccessibleObject fieldOrMethod = fieldOrMethods.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        if (fieldOrMethod instanceof Method) {
            return ((Method) fieldOrMethod).getReturnType();
        }
        return ((Field) fieldOrMethod).getType();
    }

    /**
     * Get a type for a field on an entity
     *
     * @param entity Entity instance
     * @param identifier Field to lookup type
     * @return Type of entity
     */
    public Class<?> getType(Object entity, String identifier) {
        return getType(entity.getClass(), identifier);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entityClass the entity class
     * @param identifier the identifier
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Class<?> entityClass, String identifier) {
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods = entity(entityClass).fieldsToValues;
        if (fieldOrMethods == null) {
            return null;
        }
        AccessibleObject fieldOrMethod = fieldOrMethods.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }

        Type type;

        if (fieldOrMethod instanceof Method) {
            type = ((Method) fieldOrMethod).getGenericReturnType();
        } else {
            type = ((Field) fieldOrMethod).getGenericType();
        }

        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        }

        return getType(entityClass, identifier);
    }

    /**
     * Retrieve the parameterized type for the given field
     *
     * @param entity Entity instance
     * @param identifier Field to lookup
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Object entity, String identifier) {
        return getParameterizedType(entity.getClass(), identifier);
    }

    /**
     * Get the true field/method name from an alias
     *
     * @param entityClass Entity name
     * @param alias Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Class<?> entityClass, String alias) {
        ConcurrentHashMap<String, String> map = entity(entityClass).aliasesToFields;
        if (map != null) {
            return map.get(alias);
        }
        return null;
    }

    /**
     * Get the true field/method name from an alias
     *
     * @param entity Entity instance
     * @param alias Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Object entity, String alias) {
        return getNameFromAlias(entity.getClass(), alias);
    }

    /**
     * Initialize an entity
     *
     * @param <T>   the type parameter
     * @param entity Entity to initialize
     */
    public <T> void initializeEntity(T value) {
        if (value != null) {
            try {
                AccessibleObject initializer = entity(value.getClass()).getInitializer();
                if (initializer instanceof Field) {
                    ((Field) initializer).get(value);
                }
                if (initializer instanceof Method) {
                    ((Method) initializer).invoke(value, (Object[]) null);
                }
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                log.trace("Unable to initialize {}", e, e);
            }
        }
    }

    /**
     * Add given Entity bean to dictionary
     *
     * @param cls Entity bean class
     * @param databaseManager associated Database Manager
     */
    public void bindEntity(Class<?> cls) {
        Annotation annotation = getFirstAnnotation(cls, Arrays.asList(Include.class, Exclude.class));
        Include include = annotation instanceof Include ? (Include) annotation : null;
        Exclude exclude = annotation instanceof Exclude ? (Exclude) annotation : null;
        if (exclude != null) {
            log.trace("Exclude {}", cls.getName());
            return;
        }
        if (include == null) {
            log.trace("Missing include {}", cls.getName());
            return;
        }
        String type;
        if ("".equals(include.type())) {
            type = WordUtils.uncapitalize(cls.getSimpleName());
        } else {
            type = include.type();
        }
        Class<?> duplicate = bindJsonApiToEntity.put(type, cls);
        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
        }

        entityBindings.putIfAbsent(lookupEntityClass(cls), new EntityBinding(cls, type));
        if (include.rootLevel()) {
            bindEntityRoots.add(cls);
        }
    }

    /**
     * Return annotation from class, parents or package
     *
     * @param record the record
     * @param annotationClass the annotation class
     * @param <A> genericClass
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(PersistentResource record, Class<A> annotationClass) {
        return getAnnotation(record.getResourceClass(), annotationClass);
    }

    /**
     * Return annotation from class, parents or package
     *
     * @param recordClass the record class
     * @param annotationClass the annotation class
     * @param <A> genericClass
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(Class<?> recordClass, Class<A> annotationClass) {
        A annotation = null;
        for (Class<?> cls = recordClass; annotation == null && cls != null; cls = cls.getSuperclass()) {
            annotation = cls.getAnnotation(annotationClass);
        }
        // no class annotation, try packages
        for (Package pkg = recordClass.getPackage(); annotation == null && pkg != null; pkg = getParentPackage(pkg)) {
            annotation = pkg.getAnnotation(annotationClass);
        }
        return annotation;
    }

    private static Package getParentPackage(Package pkg) {
        String name = pkg.getName();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? null : Package.getPackage(name.substring(0, idx));
    }

    /**
     * Return a single annotation from field or accessor method.
     *
     * @param entityClass the entity class
     * @param annotationClass given annotation type
     * @param identifier the identifier
     * @param <A> genericClass
     * @return annotation found
     */
    public <A extends Annotation> A getAttributeOrRelationAnnotation(Class<?> entityClass,
                                                                     Class<A> annotationClass,
                                                                     String identifier) {
        AccessibleObject fieldOrMethod = entity(entityClass).fieldsToValues.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        return fieldOrMethod.getAnnotation(annotationClass);
    }

    /**
     * Return multiple annotations from field or accessor method.
     *
     * @param <A>   the type parameter
     * @param entityClass the entity class
     * @param annotationClass given annotation type
     * @param identifier the identifier
     * @return annotation found or null if none found
     */
    public <A extends Annotation> A[] getAttributeOrRelationAnnotations(Class<?> entityClass,
                                                                        Class<A> annotationClass,
                                                                        String identifier) {
        AccessibleObject fieldOrMethod = entity(entityClass).fieldsToValues.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        return fieldOrMethod.getAnnotationsByType(annotationClass);
    }

    /**
     * Return first matching annotation from class, parents or package
     *
     * @param entityClass Entity class type
     * @param annotationClassList List of sought annotations
     * @return annotation found
     */
    public Annotation getFirstAnnotation(Class<?> entityClass, List<Class<? extends Annotation>> annotationClassList) {
        Annotation annotation = null;
        for (Class<?> cls = entityClass; annotation == null && cls != null; cls = cls.getSuperclass()) {
            for (Class<? extends Annotation> annotationClass : annotationClassList) {
                annotation = cls.getAnnotation(annotationClass);
                if (annotation != null) {
                    break;
                }
            }
        }
        // no class annotation, try packages
        for (Package pkg = entityClass.getPackage(); annotation == null && pkg != null; pkg = getParentPackage(pkg)) {
            for (Class<? extends Annotation> annotationClass : annotationClassList) {
                annotation = pkg.getAnnotation(annotationClass);
                if (annotation != null) {
                    break;
                }
            }
        }
        return annotation;
    }

    /**
     * Is root.
     *
     * @param entityClass the entity class
     * @return the boolean
     */
    public boolean isRoot(Class<?> entityClass) {
        return bindEntityRoots.contains(entityClass);
    }

    /**
     * Gets id.
     *
     * @param value the value
     * @return the id
     */
    public String getId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            AccessibleObject idField = null;
            for (Class<?> cls = value.getClass(); idField == null && cls != null; cls = cls.getSuperclass()) {
                idField = entity(cls).getIdField();
            }
            if (idField instanceof Field) {
                return String.valueOf(((Field) idField).get(value));
            }
            if (idField instanceof Method) {
                return String.valueOf(((Method) idField).invoke(value, (Object[]) null));
            }
            return null;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

    /**
     * Find an arbitrary method
     *
     * @param entityClass the entity class
     * @param name the name
     * @param paramClass the param class
     * @return method method
     * @throws NoSuchMethodException the no such method exception
     */
    public static Method findMethod(Class<?> entityClass, String name, Class<?>... paramClass)
            throws NoSuchMethodException {
        for (Method m : entityClass.getMethods()) {
            int modifiers = m.getModifiers();
            if (!Modifier.isAbstract(modifiers) && !Modifier.isTransient(modifiers) && m.getName().equals(name)) {
                if (paramClass != null) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == paramClass.length) {
                        boolean validMethod = true;
                        for (int i = 0; i < params.length; ++i) {
                            if (paramClass[i] == null) {
                                validMethod &= !params[i].isPrimitive();
                            } else {
                                validMethod &= params[i].isAssignableFrom(paramClass[i]);
                            }
                        }
                        if (validMethod) {
                            return m;
                        }
                    }
                } else {
                    // Finds a method with no arguments matching the specified name.
                    if (m.getParameterCount() == 0) {
                        return m;
                    }
                }
            }
        }
        throw new NoSuchMethodException(name);
    }

    static Class<?> lookupEntityClass(Class<?> objClass) {
        for (Class<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
            if (cls.isAnnotationPresent(Entity.class)) {
                return cls;
            }
        }
        throw new IllegalArgumentException("Unknown Entity " + objClass);
    }
}
