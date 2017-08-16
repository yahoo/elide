/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.security.checks.prefab.Common;
import com.yahoo.elide.security.checks.prefab.Role;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    protected final ConcurrentHashMap<Class<?>, List<Class<?>>> subclassingEntities = new ConcurrentHashMap<>();
    protected final BiMap<String, Class<? extends Check>> checkNames;

    /**
     * Instantiate a new EntityDictionary with the provided set of checks. In addition all of the checks
     * in {@link com.yahoo.elide.security.checks.prefab} are mapped to {@code Prefab.CONTAINER.CHECK}
     * (e.g. {@code @ReadPermission(expression="Prefab.Role.All")}
     * or {@code @ReadPermission(expression="Prefab.Common.UpdateOnCreate")})
     * @param checks a map that links the identifiers used in the permission expression strings
     *               to their implementing classes
     */
    public EntityDictionary(Map<String, Class<? extends Check>> checks) {
        checkNames = Maps.synchronizedBiMap(HashBiMap.create(checks));

        addPrefabCheck("Prefab.Role.All", Role.ALL.class);
        addPrefabCheck("Prefab.Role.None", Role.NONE.class);
        addPrefabCheck("Prefab.Collections.AppendOnly", AppendOnly.class);
        addPrefabCheck("Prefab.Collections.RemoveOnly", RemoveOnly.class);
        addPrefabCheck("Prefab.Common.UpdateOnCreate", Common.UpdateOnCreate.class);
    }

    private void addPrefabCheck(String alias, Class<? extends Check> checkClass) {
        if (checkNames.containsKey(alias) || checkNames.inverse().containsKey(checkClass)) {
            return;
        }

        checkNames.put(alias, checkClass);
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
                || m.isAnnotationPresent(Transient.class)
                && !m.isAnnotationPresent(ComputedAttribute.class)
                && !m.isAnnotationPresent(ComputedRelationship.class)) {
            throw new NoSuchMethodException(name);
        }
        return m;
    }

    protected EntityBinding getEntityBinding(Class<?> entityClass) {
        return entityBindings.getOrDefault(lookupEntityClass(entityClass), EntityBinding.EMPTY_BINDING);
    }

    /**
     * Returns the binding class for a given entity name.
     *
     * @param entityName entity name
     * @return binding class
     */
    public Class<?> getEntityClass(String entityName) {
        return bindJsonApiToEntity.get(entityName);
    }

    /**
     * Returns the entity name for a given binding class.
     *
     * @param entityClass the entity class
     * @return binding class
     */
    public String getJsonAliasFor(Class<?> entityClass) {
        return getEntityBinding(entityClass).jsonApiType;
    }

    /**
     * Determine if a given (entity class, permission) pair have any permissions defined.
     *
     * @param resourceClass the entity class
     * @param annotationClass the permission annotation
     * @return {@code true} if that permission is defined anywhere within the class
     */
    public boolean entityHasChecksForPermission(Class<?> resourceClass, Class<? extends Annotation> annotationClass) {
        EntityBinding binding = getEntityBinding(resourceClass);
        return binding.entityPermissions.hasChecksForPermission(annotationClass);
    }

    /**
     * Gets the specified permission definition (if any) at the class level.
     *
     * @param resourceClass the entity to check
     * @param annotationClass the permission to look for
     * @return a {@code ParseTree} expressing the permissions, if one exists
     *         or {@code null} if the permission is not specified at a class level
     */
    public ParseTree getPermissionsForClass(Class<?> resourceClass,
                                                       Class<? extends Annotation> annotationClass) {
        EntityBinding binding = getEntityBinding(resourceClass);
        return binding.entityPermissions.getClassChecksForPermission(annotationClass);
    }

    /**
     * Gets the specified permission definition (if any) at the class level.
     *
     * @param resourceClass the entity to check
     * @param field the field to inspect
     * @param annotationClass the permission to look for
     * @return a {@code ParseTree} expressing the permissions, if one exists
     *         or {@code null} if the permission is not specified on that field
     */
    public ParseTree getPermissionsForField(Class<?> resourceClass,
                                                       String field,
                                                       Class<? extends Annotation> annotationClass) {
        EntityBinding binding = getEntityBinding(resourceClass);
        return binding.entityPermissions.getFieldChecksForPermission(field, annotationClass);
    }

    /**
     * Returns the check mapped to a particular identifier.
     *
     * @param checkIdentifier the name from the expression string
     * @return the {@link Check} mapped to the identifier or {@code null} if the given identifer is unmapped
     */
    public Class<? extends Check> getCheck(String checkIdentifier) {
        Class<? extends Check> checkCls = checkNames.get(checkIdentifier);

        if (checkCls == null) {
            try {
                checkCls = (Class<? extends Check>) Class.forName(checkIdentifier);
                try {
                    checkNames.putIfAbsent(checkIdentifier, checkCls);
                } catch (IllegalArgumentException e) {
                    log.error("HELP! {} {} {}", checkIdentifier, checkCls, checkNames.inverse().get(checkCls));
                    throw e;
                }
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IllegalArgumentException(
                        "Could not instantiate specified check '" + checkIdentifier + "'.", e);
            }
        }

        return checkCls;
    }

    /**
     * Get inherited entity names for a particular entity.
     *
     * @param entityName Json alias name for entity
     * @return  List of all inherited entity type names
     */
    public List<String> getSubclassingEntityNames(String entityName) {
        return getSubclassingEntityNames(getEntityClass(entityName));
    }

    /**
     * Get inherited entity names for a particular entity.
     *
     * @param entityClass Entity class
     * @return  List of all inherited entity type names
     */
    public List<String> getSubclassingEntityNames(Class entityClass) {
        List<Class<?>> entities = getSubclassingEntities(entityClass);
        return entities.stream().map(this::getJsonAliasFor).collect(Collectors.toList());
    }

    /**
     * Get a list of inherited entities from a particular entity.
     * Namely, the list of entities inheriting from the provided class.
     *
     * @param entityName Json alias name for entity
     * @return  List of all inherited entity types
     */
    public List<Class<?>> getSubclassingEntities(String entityName) {
        return getSubclassingEntities(getEntityClass(entityName));
    }

    /**
     * Get a list of inherited entities from a particular entity.
     * Namely, the list of entities inheriting from the provided class.
     *
     * @param entityClass Entity class
     * @return  List of all inherited entity types
     */
    public List<Class<?>> getSubclassingEntities(Class entityClass) {
        return subclassingEntities.computeIfAbsent(entityClass, (unused) -> {
            return entityBindings.keySet().stream()
                    .filter(c -> c != entityClass && entityClass.isAssignableFrom(c))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Fetch all entity names that the provided entity inherits from (i.e. all superclass entities down to,
     * but excluding Object).
     *
     * @param entityName Json alias name for entity
     * @return  List of all super class entity json names
     */
    public List<String> getSuperClassEntityNames(String entityName) {
        return getSuperClassEntityNames(getEntityClass(entityName));
    }

    /**
     * Fetch all entity names that the provided entity inherits from (i.e. all superclass entities down to,
     * but excluding Object).
     *
     * @param entityClass Entity class
     * @return  List of all super class entity json names
     */
    public List<String> getSuperClassEntityNames(Class entityClass) {
        return getSuperClassEntities(entityClass).stream()
                .map(this::getJsonAliasFor)
                .collect(Collectors.toList());
    }

    /**
     * Fetch all entity classes that the provided entity inherits from (i.e. all superclass entities down to,
     * but excluding Object).
     *
     * @param entityName Json alias name for entity
     * @return  List of all super class entity classes
     */
    public List<Class<?>> getSuperClassEntities(String entityName) {
        return getSuperClassEntities(getEntityClass(entityName));
    }

    /**
     * Fetch all entity classes that provided entity inherits from (i.e. all superclass entities down to,
     * but excluding Object).
     *
     * @param entityClass Entity class
     * @return  List of all super class entity classes
     */
    public List<Class<?>> getSuperClassEntities(Class entityClass) {
        return getEntityBinding(entityClass).inheritedTypes.stream()
                .filter(entityBindings::containsKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns the friendly named mapped to this given check.
     * @param checkClass The class to lookup
     * @return the friendly name of the check.
     */
    public String getCheckIdentifier(Class<? extends Check> checkClass) {
        String identifier = checkNames.inverse().get(checkClass);

        if (identifier == null) {
            return checkClass.getName();
        }
        return identifier;
    }

    /**
     * Returns the name of the id field.
     *
     * @param entityClass Entity class
     * @return id field name
     */
    public String getIdFieldName(Class<?> entityClass) {
        return getEntityBinding(entityClass).getIdFieldName();
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
     * Get the check mappings.
     * @return a map of check mappings this dictionary knows about
     */
    public Map<String, Class<? extends Check>> getCheckMappings() {
        return checkNames;
    }

    /**
     * Get the list of attribute names for an entity.
     *
     * @param entityClass entity name
     * @return List of attribute names for entity
     */
    public List<String> getAttributes(Class<?> entityClass) {
        return getEntityBinding(entityClass).attributes;
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
        return getEntityBinding(entityClass).relationships;
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
     * Determine whether or not a method is request scopeable.
     *
     * @param entity  Entity instance
     * @param method  Method on entity to check
     * @return True if method accepts a RequestScope, false otherwise.
     */
    public boolean isMethodRequestScopeable(Object entity, Method method) {
        return isMethodRequestScopeable(entity.getClass(), method);
    }

    /**
     * Determine whether or not a method is request scopeable.
     *
     * @param entityClass  Entity to check
     * @param method  Method on entity to check
     * @return True if method accepts a RequestScope, false otherwise.
     */
    public boolean isMethodRequestScopeable(Class<?> entityClass, Method method) {
        return getEntityBinding(entityClass).requestScopeableMethods.getOrDefault(method, false);
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
        final ConcurrentHashMap<String, RelationshipType> types = getEntityBinding(cls).relationshipTypes;
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
        final EntityBinding clsBinding = getEntityBinding(cls);
        final ConcurrentHashMap<String, String> mappings = clsBinding.relationshipToInverse;
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
                getEntityBinding(inverseType).relationshipToInverse;

        for (Map.Entry<String, String> inverseMapping : inverseMappings.entrySet()) {
            String inverseRelationName = inverseMapping.getKey();
            String inverseMappedBy = inverseMapping.getValue();

            if (relation.equals(inverseMappedBy)
                    && getParameterizedType(inverseType, inverseRelationName).equals(clsBinding.entityClass)) {
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
        ConcurrentHashMap<String, Class<?>> fieldTypes = getEntityBinding(entityClass).fieldsToTypes;
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
        ConcurrentHashMap<String, AccessibleObject> fieldOrMethods = getEntityBinding(entityClass).fieldsToValues;
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
        ConcurrentHashMap<String, String> map = getEntityBinding(entityClass).aliasesToFields;
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
            Initializer<T> initializer = getEntityBinding(entity.getClass()).getInitializer();
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
        getEntityBinding(cls).setInitializer(initializer);
    }

    /**
     * Returns whether or not an entity is shareable.
     *
     * @param entityClass the entity type to check for the shareable permissions
     * @return true if entityClass is shareable.  False otherwise.
     */
    public boolean isShareable(Class<?> entityClass) {
        return getAnnotation(entityClass, SharePermission.class) != null;
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
            type = StringUtils.uncapitalize(cls.getSimpleName());
        } else {
            type = include.type();
        }

        Class<?> duplicate = bindJsonApiToEntity.put(type, cls);
        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
        }

        entityBindings.putIfAbsent(lookupEntityClass(cls), new EntityBinding(this, cls, type));
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
        return getEntityBinding(recordClass).getAnnotation(annotationClass);
    }

    public <A extends Annotation> Collection<Method> getTriggers(Class<?> cls,
                                                                 Class<A> annotationClass,
                                                                 String fieldName) {
        return getEntityBinding(cls).getTriggers(annotationClass, fieldName);
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
        AccessibleObject fieldOrMethod = getEntityBinding(entityClass).fieldsToValues.get(identifier);
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
        AccessibleObject fieldOrMethod = getEntityBinding(entityClass).fieldsToValues.get(identifier);
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
    public static Annotation getFirstAnnotation(Class<?> entityClass,
                                                List<Class<? extends Annotation>> annotationClassList) {
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
                idField = getEntityBinding(cls).getIdField();
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
        return getEntityBinding(entityClass).getIdType();
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

        AccessibleObject idField = getEntityBinding(value.getClass()).getIdField();
        if (idField != null) {
            return Arrays.asList(idField.getDeclaredAnnotations());
        }

        return Collections.emptyList();
    }

    /**
     * Returns the parameterized type or the binding class for the given {@param identifier}.
     * @param entityClass the entity class
     * @param identifier the identifier
     * @return the binding class
     */
    public Class<?> getLoadClass(Class<?> entityClass, String identifier) {
        if(entityClass != null) {
            return getParameterizedType(entityClass, identifier);
        } else {
            return getEntityClass(identifier);
        }
    }

    /**
     * Follow for this class or super-class for Entity annotation.
     *
     * @param objClass provided class
     * @return class with Entity annotation
     */
    public Class<?> lookupEntityClass(Class<?> objClass) {
        for (Class<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
            if (entityBindings.containsKey(cls) || cls.isAnnotationPresent(Entity.class)) {
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
        return getAccessibleObject(target.getClass(), fieldName);
    }

    /**
     * Retrieve the accessible object for a field.
     *
     * @param targetClass the object to get
     * @param fieldName   the field name to get or invoke equivalent get method
     * @return the value
     */
    public AccessibleObject getAccessibleObject(Class<?> targetClass, String fieldName) {
        return getEntityBinding(targetClass).fieldsToValues.get(fieldName);
    }

    /**
     * Retrieve fields from an object containing a particular type.
     *
     * @param targetClass Class to search for fields
     * @param targetType Type of fields to find
     * @return Set containing field names
     */
    public Set<String> getFieldsOfType(Class<?> targetClass, Class<?> targetType) {
        HashSet<String> fields = new HashSet<>();
        for (String field : getAllFields(targetClass)) {
            if (getParameterizedType(targetClass, field).equals(targetType)) {
                fields.add(field);
            }
        }
        return fields;
    }

    public boolean isRelation(Class<?> entityClass, String relationName) {
        return getEntityBinding(entityClass).relationships.contains(relationName);
    }

    public boolean isAttribute(Class<?> entityClass, String attributeName) {
        return getEntityBinding(entityClass).attributes.contains(attributeName);
    }

    /**
     * Returns true if the relationship cascades deletes and false otherwise.
     * @param targetClass The class which owns the relationship.
     * @param fieldName The relationship
     * @return true or false
     */
    public boolean cascadeDeletes(Class<?> targetClass, String fieldName) {
        CascadeType [] cascadeTypes =
                getEntityBinding(targetClass).relationshipToCascadeTypes.getOrDefault(fieldName, new CascadeType[0]);

        for (CascadeType cascadeType : cascadeTypes) {
            if (cascadeType == CascadeType.ALL || cascadeType == CascadeType.REMOVE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the entity graph and performs a transform function on each element.
     * @param entities The roots of the entity graph.
     * @param transform The function to transform each entity class into a result.
     * @param <T> The result type.
     * @return The collection of results.
     */
    public <T> List<T> walkEntityGraph(Set<Class<?>> entities,  Function<Class<?>, T> transform) {
        ArrayList<T> results = new ArrayList<>();
        Queue<Class<?>> toVisit = new ArrayDeque<>(entities);
        Set<Class<?>> visited = new HashSet<>();
        while (! toVisit.isEmpty()) {
            Class<?> clazz = toVisit.remove();
            results.add(transform.apply(clazz));
            visited.add(clazz);

            for (String relationship : getRelationships(clazz)) {
                Class<?> relationshipClass = getParameterizedType(clazz, relationship);

                try {
                    lookupEntityClass(relationshipClass);
                } catch (IllegalArgumentException e) {

                    /* The relationship hasn't been bound */
                    continue;
                }

                if (!visited.contains(relationshipClass)) {
                    toVisit.add(relationshipClass);
                }
            }
        }
        return results;
    }

    /**
     * Returns whether or not a class is already bound.
     * @param cls
     * @return
     */
    public boolean hasBinding(Class<?> cls) {
        return bindJsonApiToEntity.contains(cls);
    }
}
