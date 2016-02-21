/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.text.WordUtils;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entity Dictionary maps JSON API Entity beans to/from Entity type names.
 *
 * @see Include#type
 */
@Slf4j
@SuppressWarnings("static-method")
public class EntityDictionary {

    protected final ConcurrentHashMap<String, Class<?>> bindJsonApiToEntity = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Class<?>, EntityBinding> entityBindings = new ConcurrentHashMap<>();
    protected final CopyOnWriteArrayList<Class<?>> bindEntityRoots = new CopyOnWriteArrayList<>();

    /**
     * Instantiates a new Entity dictionary.
     */
    public EntityDictionary() {
        // Do nothing
    }

    private static Package getParentPackage(Package pkg) {
        String name = pkg.getName();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? null : Package.getPackage(name.substring(0, idx));
    }

    /**
     * Find an arbitrary method.
     *
     * @param entityClass the entity class
     * @param name        the name
     * @param paramClass  the param class
     * @return method method
     * @throws NoSuchMethodException the no such method exception
     */
    public static Method findMethod(Class<?> entityClass, String name, Class<?>... paramClass)
            throws NoSuchMethodException {
        Method m = entityClass.getMethod(name, paramClass);
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers)
                || (m.isAnnotationPresent(Transient.class) && !m.isAnnotationPresent(ComputedAttribute.class))) {
            throw new NoSuchMethodException(name);
        }
        return m;
    }

    protected EntityBinding entityBinding(Class<?> entityClass) {
        EntityBinding entityBinding = entityBindings.get(lookupEntityClass(entityClass));
        return entityBinding == null ? EntityBinding.EMPTY_BINDING : entityBinding;
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
        return entityBinding(entityClass).jsonApi;
    }

    public ParseTree getEntityParseTree(Class<?> entityClass, Class annotationClass) {
        return entityBinding(entityClass).annotationToParseTree.get(annotationClass);
    }

    /**
     * Returns the name of the id field.
     *
     * @param entityClass Entity class
     * @return id field name
     */
    public String getIdFieldName(Class<?> entityClass) {
        return entityBinding(entityClass).getIdFieldName();
    }

    /**
     * Get all bindings.
     *
     * @return the bindings
     */
    public Set<Class<?>> getBindings() {
        return entityBindings.keySet();
    }

    /**
     * Get the list of attribute names for an entity.
     *
     * @param entityClass entity name
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Class<?> entityClass) {
        return entityBinding(entityClass).attrs;
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
        return entityBinding(entityClass).relationships;
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
     * Get a list of all fields including both relationships and attributes.
     *
     * @param entityClass entity name
     * @return List of all fields.
     */
    public List<String> getAllFields(Class<?> entityClass) {
        List<String> fields = new ArrayList<>();

        List<String> attrs = getAttributes(entityClass);
        List<String> rels = getRelationships(entityClass);

        if (attrs != null) {
            fields.addAll(attrs);
        }

        if (rels != null) {
            fields.addAll(rels);
        }

        return fields;
    }

    /**
     * Get a list of all fields including both relationships and attributes.
     *
     * @param entity entity
     * @return List of all fields.
     */
    public List<String> getAllFields(Object entity) {
        return getAllFields(entity.getClass());
    }

    /**
     * Get the type of relationship from a relation.
     *
     * @param cls      Entity class
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Class<?> cls, String relation) {
        final ConcurrentHashMap<String, RelationshipType> types = entityBinding(cls).relationshipTypes;
        if (types == null) {
            return RelationshipType.NONE;
        }
        final RelationshipType type = types.get(relation);
        return (type == null) ? RelationshipType.NONE : type;
    }

    /**
     * If a relationship is bidirectional, returns the name of the peer relationship in the peer entity.
     *
     * @param cls      the cls
     * @param relation the relation
     * @return relation inverse
     */
    public String getRelationInverse(Class<?> cls, String relation) {
        final ConcurrentHashMap<String, String> mappings = entityBinding(cls).relationshipToInverse;
        if (mappings != null) {
            final String mapping = mappings.get(relation);

            if (mapping != null && !mapping.equals("")) {
                return mapping;
            }
        }

        /*
         * This could be the owning side of the relation.  Let's see if the entity referenced in the relation
         * has a bidirectional reference that is mapped to the given relation.
         */
        final Class<?> inverseType = getParameterizedType(cls, relation);
        final ConcurrentHashMap<String, String> inverseMappings =
                entityBinding(inverseType).relationshipToInverse;

        for (Map.Entry<String, String> inverseMapping : inverseMappings.entrySet()) {
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
     * @param entity   Entity instance
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
     * @param identifier  Field to lookup type
     * @return Type of entity
     */
    public Class<?> getType(Class<?> entityClass, String identifier) {
        ConcurrentHashMap<String, Class<?>> fieldTypes = entityBinding(entityClass).fieldsToTypes;
        return fieldTypes == null ? null : fieldTypes.get(identifier);
    }

    /**
     * Get a type for a field on an entity.
     *
     * @param entity     Entity instance
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
     * @param identifier  the identifier
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Class<?> entityClass, String identifier) {
        return getParameterizedType(entityClass, identifier, 0);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entityClass the entity class
     * @param identifier  the identifier
     * @param paramIndex  the index of the parameterization
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Class<?> entityClass, String identifier, int paramIndex) {
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods = entityBinding(entityClass).fieldsToValues;
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
            return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[paramIndex];
        }

        return getType(entityClass, identifier);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entity     Entity instance
     * @param identifier Field to lookup
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Object entity, String identifier) {
        return getParameterizedType(entity.getClass(), identifier);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entity     Entity instance
     * @param identifier Field to lookup
     * @param paramIndex the index of the parameterization
     * @return Entity type for field otherwise null.
     */
    public Class<?> getParameterizedType(Object entity, String identifier, int paramIndex) {
        return getParameterizedType(entity.getClass(), identifier, paramIndex);
    }

    /**
     * Get the true field/method name from an alias.
     *
     * @param entityClass Entity name
     * @param alias       Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Class<?> entityClass, String alias) {
        ConcurrentHashMap<String, String> map = entityBinding(entityClass).aliasesToFields;
        if (map != null) {
            return map.get(alias);
        }
        return null;
    }

    /**
     * Get the true field/method name from an alias.
     *
     * @param entity Entity instance
     * @param alias  Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Object entity, String alias) {
        return getNameFromAlias(entity.getClass(), alias);
    }

    /**
     * Initialize an entity.
     *
     * @param <T>    the type parameter
     * @param entity Entity to initialize
     */
    public <T> void initializeEntity(T entity) {
        if (entity != null) {
            @SuppressWarnings("unchecked")
            Initializer<T> initializer = entityBinding(entity.getClass()).getInitializer();
            if (initializer != null) {
                initializer.initialize(entity);
            }
        }
    }

    /**
     * Bind a particular initializer to a class.
     *
     * @param <T>         the type parameter
     * @param initializer Initializer to use for class
     * @param cls         Class to bind initialization
     */
    public <T> void bindInitializer(Initializer<T> initializer, Class<T> cls) {
        entityBinding(cls).setInitializer(initializer);
    }

    /**
     * Returns whether or not an entity is shareable.
     *
     * @param entityClass the entity type to check for the shareable permissions
     * @return true if entityClass is shareable.  False otherwise.
     */
    public boolean isShareable(Class<?> entityClass) {
        SharePermission share = (SharePermission) getFirstAnnotation(entityClass,
                Collections.singletonList(SharePermission.class));
        return share != null;
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

        entityBindings.putIfAbsent(lookupEntityClass(cls), new EntityBinding(cls, type));
        if (include.rootLevel()) {
            bindEntityRoots.add(cls);
        }
    }

    /**
     * Return annotation from class, parents or package.
     *
     * @param record          the record
     * @param annotationClass the annotation class
     * @param <A>             genericClass
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(PersistentResource record, Class<A> annotationClass) {
        return getAnnotation(record.getResourceClass(), annotationClass);
    }

    /**
     * Return annotation from class, parents or package.
     *
     * @param recordClass     the record class
     * @param annotationClass the annotation class
     * @param <A>             genericClass
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

    public <A extends Annotation> Collection<Method> getTriggers(Class<?> cls,
                                                                 Class<A> annotationClass,
                                                                 String fieldName) {
        return entityBinding(cls).getTriggers(annotationClass, fieldName);
    }

    /**
     * Return a single annotation from field or accessor method.
     *
     * @param entityClass     the entity class
     * @param annotationClass given annotation type
     * @param identifier      the identifier
     * @param <A>             genericClass
     * @return annotation found
     */
    public <A extends Annotation> A getAttributeOrRelationAnnotation(Class<?> entityClass,
                                                                     Class<A> annotationClass,
                                                                     String identifier) {
        AccessibleObject fieldOrMethod = entityBinding(entityClass).fieldsToValues.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        return fieldOrMethod.getAnnotation(annotationClass);
    }

    /**
     * Return multiple annotations from field or accessor method.
     *
     * @param <A>             the type parameter
     * @param entityClass     the entity class
     * @param annotationClass given annotation type
     * @param identifier      the identifier
     * @return annotation found or null if none found
     */
    public <A extends Annotation> A[] getAttributeOrRelationAnnotations(Class<?> entityClass,
                                                                        Class<A> annotationClass,
                                                                        String identifier) {
        AccessibleObject fieldOrMethod = entityBinding(entityClass).fieldsToValues.get(identifier);
        if (fieldOrMethod == null) {
            return null;
        }
        return fieldOrMethod.getAnnotationsByType(annotationClass);
    }

    /**
     * Return first matching annotation from class, parents or package.
     *
     * @param entityClass         Entity class type
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
                idField = entityBinding(cls).getIdField();
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
     * Returns type of id field.
     *
     * @param entityClass the entity class
     * @return ID type
     */
    public Class<?> getIdType(Class<?> entityClass) {
        return entityBinding(entityClass).getIdType();
    }

    /**
     * Returns annotations applied to the ID field.
     *
     * @param value the value
     * @return Collection of Annotations
     */
    public Collection<Annotation> getIdAnnotations(Object value) {
        if (value == null) {
            return null;
        }

        AccessibleObject idField = entityBinding(value.getClass()).getIdField();
        if (idField != null) {
            return Arrays.asList(idField.getDeclaredAnnotations());
        }

        return Collections.emptyList();
    }

    /**
     * Follow for this class or super-class for Entity annotation.
     *
     * @param objClass provided class
     * @return class with Entity annotation
     */
    public Class<?> lookupEntityClass(Class<?> objClass) {
        for (Class<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
            if (cls.isAnnotationPresent(Entity.class)) {
                return cls;
            }
        }
        throw new IllegalArgumentException("Unknown Entity " + objClass);
    }

    /**
     * Retrieve the accessible object for a field from a target object.
     *
     * @param target    the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @return the value
     */
    public AccessibleObject getAccessibleObject(Object target, String fieldName) {
        Class<?> targetClass = lookupEntityClass(target.getClass());
        return getAccessibleObject(targetClass, fieldName);
    }

    /**
     * Retrieve the accessible object for a field.
     *
     * @param targetClass the object to get
     * @param fieldName   the field name to get or invoke equivalent get method
     * @return the value
     */
    public AccessibleObject getAccessibleObject(Class<?> targetClass, String fieldName) {
        ConcurrentHashMap<String, AccessibleObject> map = entityBinding(targetClass).accessibleObject;
        return map.get(fieldName);
    }
}
