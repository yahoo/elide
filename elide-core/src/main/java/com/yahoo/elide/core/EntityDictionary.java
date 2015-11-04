/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.text.WordUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see Include#type
 */
@Slf4j
@SuppressWarnings("static-method")
public class EntityDictionary {

    private final static List<Method> OBJ_METHODS = Arrays.asList(Object.class.getMethods());

    private final ConcurrentHashMap<String, Class<?>> bindJsonApiToEntity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, String> bindEntityToJsonApi = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, AccessibleObject> bindIdField = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Initializer<?>> initializers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentLinkedDeque<String>>
        bindEntityToAttrsDeque = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, List<String>>
        bindEntityToAttrs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentLinkedDeque<String>>
        bindEntityToRelationshipsDeque = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, List<String>>
        bindEntityToRelationships = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, RelationshipType>>
        bindEntityToRelationshipTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, String>>
        bindEntityRelationshipToInverse = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, AccessibleObject>>
        bindEntityFieldsToValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, String>>
        bindEntityAliasesToFields = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Class<?>>  bindEntityRoots = new CopyOnWriteArrayList<>();

    /**
     * Instantiates a new Entity dictionary.
     */
    public EntityDictionary() {
        // Do nothing
    }

    /**
     * Returns the binding class for a given entity name.
     *
     * @param entityName entity name
     * @return binding class
     */
    public Class<?> getBinding(String entityName) {
        return bindJsonApiToEntity.get(entityName);
    }

    /**
     * Returns the entity name for a given binding class.
     *
     * @param entityClass the entity class
     * @return binding class
     */
    public String getBinding(Class<?> entityClass) {
        return bindEntityToJsonApi.get(lookupEntityClass(entityClass));
    }

    /**
     * Get the list of attribute names for an entity.
     *
     * @param entityClass entity name
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Class<?> entityClass) {
        return bindEntityToAttrs.get(lookupEntityClass(entityClass));
    }

    /**
     * Get the list of attribute names for an entity.
     *
     * @param entity entity instance
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Object entity) {
        return getAttributes(entity.getClass());
    }

    /**
     * Get the list of relationship names for an entity.
     *
     * @param entityClass entity name
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Class<?> entityClass) {
        return bindEntityToRelationships.get(lookupEntityClass(entityClass));
    }

    /**
     * Get the list of relationship names for an entity.
     *
     * @param entity entity instance
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Object entity) {
        return getRelationships(entity.getClass());
    }

    /**
     * Get the type of relationship from a relation.
     *
     * @param cls Entity class
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Class<?> cls, String relation) {
        ConcurrentHashMap<String, RelationshipType> types = bindEntityToRelationshipTypes.get(lookupEntityClass(cls));
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
        ConcurrentHashMap<String, String> mappings = bindEntityRelationshipToInverse.get(lookupEntityClass(cls));
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
                bindEntityRelationshipToInverse.get(lookupEntityClass(inverseType));

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
     * Get the type of relationship from a relation.
     *
     * @param entity Entity instance
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Object entity, String relation) {
        return getRelationshipType(entity.getClass(), relation);
    }

    /**
     * Get a type for a field on an entity.
     *
     * @param entityClass Entity class
     * @param identifier Field to lookup type
     * @return Type of entity
     */
    public Class<?> getType(Class<?> entityClass, String identifier) {
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods =
            bindEntityFieldsToValues.get(lookupEntityClass(entityClass));
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
     * Get a type for a field on an entity.
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
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods =
            bindEntityFieldsToValues.get(lookupEntityClass(entityClass));
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
     * Retrieve the parameterized type for the given field.
     *
     * @param entity Entity instance
     * @param identifier Field to lookup
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Object entity, String identifier) {
        return getParameterizedType(entity.getClass(), identifier);
    }

    /**
     * Get the true field/method name from an alias.
     *
     * @param entityClass Entity name
     * @param alias Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Class<?> entityClass, String alias) {
        ConcurrentHashMap<String, String> map = bindEntityAliasesToFields.get(lookupEntityClass(entityClass));
        if (map != null) {
            return map.get(alias);
        }
        return null;
    }

    /**
     * Get the true field/method name from an alias.
     *
     * @param entity Entity instance
     * @param alias Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Object entity, String alias) {
        return getNameFromAlias(entity.getClass(), alias);
    }

    /**
     * Initialize an entity.
     *
     * @param <T>   the type parameter
     * @param entity Entity to initialize
     */
    public <T> void initializeEntity(T entity) {
        if (entity != null) {
            @SuppressWarnings("unchecked")
            Initializer<T> initializer = (Initializer<T>) initializers.get(lookupEntityClass(entity.getClass()));
            if (initializer != null) {
                initializer.initialize(entity);
            }
        }
    }

    /**
     * Bind a particular initializer to a class.
     *
     * @param <T>   the type parameter
     * @param initializer Initializer to use for class
     * @param cls Class to bind initialization
     */
    public <T> void bindInitializer(Initializer<T> initializer, Class<T> cls) {
        initializers.put(cls, initializer);
    }

    /**
     * Add given Entity bean to dictionary.
     *
     * @param cls Entity bean class
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
        bindEntityToJsonApi.put(cls, type);
        if (include.rootLevel()) {
            bindEntityRoots.add(cls);
        }

        // Map id's, attributes, and relationships
        @SuppressWarnings("unchecked")
        Collection<AccessibleObject> fieldOrMethodList = CollectionUtils.union(
            Arrays.asList(cls.getFields()),
            Arrays.asList(cls.getMethods()));

        // Initialize our maps for this entity. Duplicates are checked above.
        bindEntityToAttrsDeque.put(cls, new ConcurrentLinkedDeque<>());
        bindEntityToRelationshipsDeque.put(cls, new ConcurrentLinkedDeque<>());
        bindEntityToRelationshipTypes.put(cls, new ConcurrentHashMap<>());
        bindEntityRelationshipToInverse.put(cls, new ConcurrentHashMap<>());
        bindEntityFieldsToValues.put(cls, new ConcurrentHashMap<>());
        bindEntityAliasesToFields.put(cls, new ConcurrentHashMap<>());
        bindEntityFields(cls, type, fieldOrMethodList);

        bindEntityToAttrs.put(cls, dequeToList(bindEntityToAttrsDeque.get(cls)));
        bindEntityToRelationships.put(cls, dequeToList(bindEntityToRelationshipsDeque.get(cls)));
    }

    /**
     * Return annotation from class, parents or package.
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
     * Return annotation from class, parents or package.
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
        AccessibleObject fieldOrMethod = bindEntityFieldsToValues.get(lookupEntityClass(entityClass)).get(identifier);
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
        AccessibleObject fieldOrMethod = bindEntityFieldsToValues.get(lookupEntityClass(entityClass)).get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        return fieldOrMethod.getAnnotationsByType(annotationClass);
    }

    /**
     * Return first matching annotation from class, parents or package.
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
                idField = bindIdField.get(cls);
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
     * Find an arbitrary method.
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

    /**
     * Bind fields of an entity including the Id field, attributes, and relationships.
     *
     * @param cls Class type to bind fields
     * @param type JSON API type identifier
     * @param fieldOrMethodList List of fields and methods on entity
     */
    private void bindEntityFields(Class<?> cls, String type, Collection<AccessibleObject> fieldOrMethodList) {
        for (AccessibleObject fieldOrMethod : fieldOrMethodList) {
            if (fieldOrMethod.isAnnotationPresent(Id.class)) {
                bindEntityId(cls, type, fieldOrMethod);
            } else if (fieldOrMethod.isAnnotationPresent(Transient.class)
                    && !fieldOrMethod.isAnnotationPresent(ComputedAttribute.class)) {
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
            fieldList = bindEntityToRelationshipsDeque.get(cls);
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
            bindEntityToRelationshipTypes.get(cls).put(name, type);
            bindEntityRelationshipToInverse.get(cls).put(name, mappedBy);
        } else {
            fieldList = bindEntityToAttrsDeque.get(cls);
        }

        fieldList.push(name);
        log.trace("{} {}", name, fieldOrMethod);

        bindEntityFieldsToValues.get(cls).put(name, fieldOrMethod);
    }

    /**
     * Bind an id field to an entity.
     *
     * @param cls Class type to bind fields
     * @param type JSON API type identifier
     * @param fieldOrMethod Field or method to bind
     */
    private void bindEntityId(Class<?> cls, String type, AccessibleObject fieldOrMethod) {
        AccessibleObject dup = bindIdField.put(cls, fieldOrMethod);
        if (dup != null && !fieldOrMethod.equals(dup)) {
            String name;
            if (fieldOrMethod instanceof Field) {
                name = ((Field) fieldOrMethod).getName();
            } else {
                name = ((Method) fieldOrMethod).getName();
            }
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + name);
        }
    }

    /**
     * Convert a deque to a list.
     *
     * @param deque Deque to convert
     * @return Deque as a list
     */
    private List<String> dequeToList(final Deque<String> deque) {
        ArrayList<String> result = new ArrayList<>();
        deque.stream().forEachOrdered(result::add);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(result);
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
