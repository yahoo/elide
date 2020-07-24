/*
 * Copyright 2018, Yahoo Inc.
 * Copyright 2018, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.core.EntityBinding.EMPTY_BINDING;

import com.yahoo.elide.Injector;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.MappedInterface;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.checks.Check;
import com.yahoo.elide.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.security.checks.prefab.Common;
import com.yahoo.elide.security.checks.prefab.Role;
import com.yahoo.elide.utils.ClassScanner;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.Serde;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.ws.rs.WebApplicationException;

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
    protected final Injector injector;

    protected final Function<Class, Serde> serdeLookup ;

    public final static String REGULAR_ID_NAME = "id";
    private final static ConcurrentHashMap<Class, String> SIMPLE_NAMES = new ConcurrentHashMap<>();

    /**
     * Instantiate a new EntityDictionary with the provided set of checks. In addition all of the checks
     * in {@link com.yahoo.elide.security.checks.prefab} are mapped to {@code Prefab.CONTAINER.CHECK}
     * (e.g. {@code @ReadPermission(expression="Prefab.Role.All")}
     * or {@code @ReadPermission(expression="Prefab.Common.UpdateOnCreate")})
     * @param checks a map that links the identifiers used in the permission expression strings
     *               to their implementing classes
     */
    public EntityDictionary(Map<String, Class<? extends Check>> checks) {
        this(checks, null);
    }

    /**
     * Instantiate a new EntityDictionary with the provided set of checks and an injection function.
     * In addition all of the checks * in {@link com.yahoo.elide.security.checks.prefab} are mapped
     * to {@code Prefab.CONTAINER.CHECK} * (e.g. {@code @ReadPermission(expression="Prefab.Role.All")}
     * or {@code @ReadPermission(expression="Prefab.Common.UpdateOnCreate")})
     * @param checks a map that links the identifiers used in the permission expression strings
     *               to their implementing classes
     * @param injector a function typically associated with a dependency injection framework that will
     *                 initialize Elide models.
     */
    public EntityDictionary(Map<String, Class<? extends Check>> checks, Injector injector) {
        this(checks, injector, CoerceUtil::lookup);
    }

    public EntityDictionary(Map<String, Class<? extends Check>> checks,
                            Injector injector,
                            Function<Class, Serde> serdeLookup) {
        this.serdeLookup = serdeLookup;
        checkNames = Maps.synchronizedBiMap(HashBiMap.create(checks));

        addPrefabCheck("Prefab.Role.All", Role.ALL.class);
        addPrefabCheck("Prefab.Role.None", Role.NONE.class);
        addPrefabCheck("Prefab.Collections.AppendOnly", AppendOnly.class);
        addPrefabCheck("Prefab.Collections.RemoveOnly", RemoveOnly.class);
        addPrefabCheck("Prefab.Common.UpdateOnCreate", Common.UpdateOnCreate.class);

        this.injector = injector;
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
     * Cache the simple name of the provided class.
     * @param cls the {@code Class} object to be checked
     * @return simple name
     */
    public static String getSimpleName(Class<?> cls) {
        return SIMPLE_NAMES.computeIfAbsent(cls, key -> cls.getSimpleName());
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

    /**
     * Returns an entity binding if the provided class has been bound in the dictionary.
     * Otherwise the behavior depends on whether the unbound class is an Entity or not.
     * If it is not an Entity, we return an EMPTY_BINDING.  This preserves existing behavior for relationships
     * which are entities but not bound.  Otherwise, we throw an exception - which also preserves behavior
     * for unbound non-entities.
     * @param entityClass
     * @return
     */
    protected EntityBinding getEntityBinding(Class<?> entityClass) {
        if (isMappedInterface(entityClass)) {
            return EMPTY_BINDING;
        }

        //Common case of no inheritance.  This lookup is a performance boost so we don't have to do reflection.
        EntityBinding binding = entityBindings.get(entityClass);
        if (binding != null) {
            return binding;
        }

        Class<?> declaredClass = lookupBoundClass(entityClass);

        if (declaredClass != null) {
            return entityBindings.get(declaredClass);
        }

        //Will throw an exception if entityClass is not an entity.
        lookupEntityClass(entityClass);
        return EMPTY_BINDING;
    }

    public boolean isMappedInterface(Class<?> interfaceClass) {
        return interfaceClass.isInterface() && interfaceClass.isAnnotationPresent(MappedInterface.class);
    }

    /**
     * Returns whether or not the ID field for a given model is generated by the persistence layer.
     * @param entityClass The model to lookup.
     * @return True if the ID field is generated.  False otherwise.
     */
    public boolean isIdGenerated(Class<?> entityClass) {
        return getEntityBinding(entityClass).isIdGenerated();
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
     * Returns the Include name for a given binding class.
     *
     * @param entityClass the entity class
     * @return binding class
     * @see Include
     */
    public String getJsonAliasFor(Class<?> entityClass) {
        return getEntityBinding(entityClass).jsonApiType;
    }

    /**
     * Returns the Entity name for a given binding class.
     *
     * @param entityClass the entity class
     * @return binding class
     * @see Entity
     */
    public String getEntityFor(Class<?> entityClass) {
        return getEntityBinding(entityClass).entityName;
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
    public ParseTree getPermissionsForClass(Class<?> resourceClass, Class<? extends Annotation> annotationClass) {
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
        return checkNames.computeIfAbsent(checkIdentifier, cls -> {
            try {
                return Class.forName(checkIdentifier).asSubclass(Check.class);
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new IllegalArgumentException(
                        "Could not instantiate specified check '" + checkIdentifier + "'.", e);
            }
        });
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
        return subclassingEntities.computeIfAbsent(entityClass, unused -> entityBindings
                .keySet().stream()
                .filter(c -> c != entityClass && entityClass.isAssignableFrom(c))
                .collect(Collectors.toList()));
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
     * Returns whether the entire entity uses Field or Property level access.
     * @param entityClass Entity Class
     * @return The JPA Access Type
     */
    public AccessType getAccessType(Class<?> entityClass) {
        return getEntityBinding(entityClass).getAccessType();
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
     * Get the Injector for this dictionary.
     *
     * @return Injector instance.
     */
    public Injector getInjector() {
        return injector;
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
     * Get a list of elide-bound relationships.
     *
     * @param entityClass Entity class to find relationships for
     * @return List of elide-bound relationship names.
     */
    public List<String> getElideBoundRelationships(Class<?> entityClass) {
        return getRelationships(entityClass).stream()
                .filter(relationName -> getBindings().contains(getParameterizedType(entityClass, relationName)))
                .collect(Collectors.toList());
    }

    /**
     * Get a list of elide-bound relationships.
     *
     * @param entity Entity instance to find relationships for
     * @return List of elide-bound relationship names.
     */
    public List<String> getElideBoundRelationships(Object entity) {
        return getElideBoundRelationships(entity.getClass());
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

            if (mapping != null && !"".equals(mapping)) {
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
     * <p>
     * If this method is called on a bean such as the following
     * <pre>
     * {@code
     * public class Address {
     *     {@literal @}Id
     *     private Long id
     *
     *     private String street1;
     *
     *     private String street2;
     * }
     * }
     * </pre>
     * then
     * <pre>
     * {@code
     * getType(Address.class, "id") = Long.class
     * getType(Address.class, "street1") = String.class
     * getType(Address.class, "street2") = String.class
     * }
     * </pre>
     * But if the ID field is not "id" and there is no such non-ID field called "id", i.e.
     * <pre>
     * {@code
     * public class Address {
     *     {@literal @}Id
     *     private Long surrogateKey
     *
     *     private String street1;
     *
     *     private String street2;
     * }
     * }
     * </pre>
     * then
     * <pre>
     * {@code
     * getType(Address.class, "id") = Long.class
     * getType(Address.class, "surrogateKey") = Long.class
     * getType(Address.class, "street1") = String.class
     * getType(Address.class, "street2") = String.class
     * }
     * </pre>
     * JSON-API spec does not allow "id" as non-ID field name. If, therefore, there is a non-ID field called "id",
     * calling this method has undefined behavior
     *
     * @param entityClass Entity class
     * @param identifier  Identifier/Field to lookup type
     * @return Type of entity
     */
    public Class<?> getType(Class<?> entityClass, String identifier) {
        if (identifier.equals(REGULAR_ID_NAME)) {
            return getEntityBinding(entityClass).getIdType();
        }

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
     * @param identifier  the identifier/field name
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

        return EntityBinding.getFieldType(entityClass, fieldOrMethod, Optional.of(paramIndex));
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
            } else if (injector != null) {
                injector.inject(entity);
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
        bindIfUnbound(cls);
        getEntityBinding(cls).setInitializer(initializer);
    }

    /**
     * Returns whether or not an entity is shareable.
     *
     * @param entityClass the entity type to check for the shareable permissions
     * @return true if entityClass is shareable.  False otherwise.
     */
    public boolean isShareable(Class<?> entityClass) {
        return getAnnotation(entityClass, SharePermission.class) != null
                && getAnnotation(entityClass, SharePermission.class).sharable();
    }

    /**
     * Add given Entity bean to dictionary.
     *
     * @param cls Entity bean class
     */
    public void bindEntity(Class<?> cls) {
        Class<?> declaredClass = lookupIncludeClass(cls);

        if (declaredClass == null) {
            log.trace("Missing include or excluded class {}", cls.getName());
            return;
        }

        if (isClassBound(declaredClass)) {
            //Ignore duplicate bindings.
            return;
        }

        Include include = (Include) getFirstAnnotation(declaredClass, Arrays.asList(Include.class));
        Entity entity = (Entity) getFirstAnnotation(declaredClass, Arrays.asList(Entity.class));

        String name;
        if (entity == null || "".equals(entity.name())) {
            name = StringUtils.uncapitalize(cls.getSimpleName());
        } else {
            name = entity.name();
        }

        String type;
        if ("".equals(include.type())) {
            type = name;
        } else {
            type = include.type();
        }

        bindJsonApiToEntity.put(type, declaredClass);
        entityBindings.put(declaredClass, new EntityBinding(this, declaredClass, type, name));
        if (include.rootLevel()) {
            bindEntityRoots.add(declaredClass);
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

    /**
     * Return annotation from class for provided method.
     * @param recordClass the record class
     * @param method the method
     * @param annotationClass the annotation class
     * @param <A> genericClass
     * @return the annotation
     */
    public <A extends Annotation> A getMethodAnnotation(Class<?> recordClass, String method, Class<A> annotationClass) {
        return getEntityBinding(recordClass).getMethodAnnotation(annotationClass, method);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Class<?> cls,
                                                                        Class<A> annotationClass,
                                                                        String fieldName) {
        return getEntityBinding(cls).getTriggers(annotationClass, fieldName);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Class<?> cls,
                                                                        Class<A> annotationClass) {
        return getEntityBinding(cls).getTriggers(annotationClass);
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
                annotation = cls.getDeclaredAnnotation(annotationClass);
                if (annotation != null) {
                    break;
                }
            }
        }
        // no class annotation, try packages
        for (Package pkg = entityClass.getPackage(); annotation == null && pkg != null; pkg = getParentPackage(pkg)) {
            for (Class<? extends Annotation> annotationClass : annotationClassList) {
                annotation = pkg.getDeclaredAnnotation(annotationClass);
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
            Class<?> valueClass = value.getClass();
            for (; idField == null && valueClass != null; valueClass = valueClass.getSuperclass()) {
                try {
                    idField = getEntityBinding(valueClass).getIdField();
                } catch (NullPointerException e) {
                    log.warn("Class: {} ID Field: {}", valueClass.getSimpleName(), idField);
                }
            }

            Class<?> idClass;
            Object idValue;
            if (idField instanceof Field) {
                idValue = ((Field) idField).get(value);
                idClass = ((Field) idField).getType();
            } else if (idField instanceof Method) {
                idValue = ((Method) idField).invoke(value, (Object[]) null);
                idClass = ((Method) idField).getReturnType();
            } else {
                return null;
            }

            Serde serde = serdeLookup.apply(idClass);
            if (serde != null) {
                return String.valueOf(serde.serialize(idValue));
            }

            return String.valueOf(idValue);
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
     * Follow for this class or super-class for Entity annotation.
     *
     * @param objClass provided class
     * @return class with Entity annotation
     */
    public Class<?> lookupEntityClass(Class<?> objClass) {
        Class<?> declaringClass = lookupAnnotationDeclarationClass(objClass, Entity.class);
        if (declaringClass != null) {
            return declaringClass;
        }
        throw new IllegalArgumentException("Unbound Entity " + objClass);
    }

    /**
     * Follow for this class or super-class for Include annotation.
     *
     * @param objClass provided class
     * @return class with Include annotation or
     */
    public Class<?> lookupIncludeClass(Class<?> objClass) {
        Annotation first = getFirstAnnotation(objClass, Arrays.asList(Exclude.class, Include.class));
        if (first instanceof Include) {
            return objClass;
        }
        return null;
    }

    /**
     * Search a class hierarchy to find the first instance of a declared annotation.
     * @param objClass The class to start searching.
     * @param annotationClass The annotation to search for.
     * @return The class which declares the annotation or null.
     */
    public Class<?> lookupAnnotationDeclarationClass(Class<?> objClass, Class<? extends Annotation> annotationClass) {
        for (Class<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
            if (cls.getDeclaredAnnotation(annotationClass) != null) {
                return cls;
            }
        }
        return null;
    }

    /**
     * Return bound entity or null.
     *
     * @param objClass provided class
     * @return Bound class.
     */
    public Class<?> lookupBoundClass(Class<?> objClass) {
        //Common case - we can avoid reflection by checking the map ...
        EntityBinding binding = entityBindings.getOrDefault(objClass, EMPTY_BINDING);
        if (binding != EMPTY_BINDING) {
            return binding.entityClass;
        }

        Class<?> declaredClass = lookupIncludeClass(objClass);
        if (declaredClass == null) {
            return null;
        }

        binding = entityBindings.getOrDefault(declaredClass, EMPTY_BINDING);
        if (binding != EMPTY_BINDING) {
            return binding.entityClass;
        }

        try {
            //Special Case for ORM proxied objects.  If the class is a proxy,
            //and it is unbound, try the superclass.
            return lookupEntityClass(declaredClass.getSuperclass());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Return if the class has been bound or not.  Only safe to call while binding an entity (does not consider
     * ORM proxy objects).
     *
     * @param objClass provided class
     * @return true if the class is already bound.
     */
    private boolean isClassBound(Class<?> objClass) {
        return (entityBindings.getOrDefault(objClass, EMPTY_BINDING) != EMPTY_BINDING);
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

    public boolean isComputed(Class<?> entityClass, String fieldName) {
        AccessibleObject fieldOrMethod = getAccessibleObject(entityClass, fieldName);

        if (fieldOrMethod == null) {
            return false;
        }

        return (fieldOrMethod.isAnnotationPresent(ComputedAttribute.class)
                || fieldOrMethod.isAnnotationPresent(ComputedRelationship.class));
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
     * Scan for security checks and automatically bind them to the dictionary.
     */
    public void scanForSecurityChecks() {

        // Logic is based on https://github.com/illyasviel/elide-spring-boot/blob/master
        // /elide-spring-boot-autoconfigure/src/main/java/org/illyasviel/elide
        // /spring/boot/autoconfigure/ElideAutoConfiguration.java

        for (Class<?> cls : ClassScanner.getAnnotatedClasses(SecurityCheck.class)) {
            if (Check.class.isAssignableFrom(cls)) {
                SecurityCheck securityCheckMeta = cls.getAnnotation(SecurityCheck.class);
                log.debug("Register Elide Check [{}] with expression [{}]",
                        cls.getCanonicalName(), securityCheckMeta.value());
                checkNames.put(securityCheckMeta.value(), cls.asSubclass(Check.class));
            } else {
                throw new IllegalStateException("Class annotated with SecurityCheck is not a Check");
            }
        }
    }

    /**
     * Binds a lifecycle hook to a particular field or method in an entity.  The hook will be called a
     * single time per request per field READ, CREATE, or UPDATE.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param annotationClass (OnReadPostCommit, OnUpdatePreSecurity, etc)
     * @param fieldOrMethodName The name of the field or method
     * @param callback The callback function to invoke.
     */
    public void bindTrigger(Class<?> entityClass,
                            Class<? extends Annotation> annotationClass,
                            String fieldOrMethodName,
                            LifeCycleHook callback) {

        bindIfUnbound(entityClass);
        getEntityBinding(entityClass).bindTrigger(annotationClass, fieldOrMethodName, callback);
    }

    /**
     * Binds a lifecycle hook to a particular entity class.  The hook will either be called:
     *  - A single time single time per request per class READ, CREATE, UPDATE, or DELETE.
     *  - Multiple times per request per field READ, CREATE, or UPDATE.
     *
     * The behavior is determined by the value of the {@code allowMultipleInvocations} flag.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param annotationClass (OnReadPostCommit, OnUpdatePreSecurity, etc)
     * @param callback The callback function to invoke.
     * @param allowMultipleInvocations Should the same life cycle hook be invoked multiple times for multiple
     *                              CRUD actions on the same model.
     */
    public void bindTrigger(Class<?> entityClass,
                            Class<? extends Annotation> annotationClass,
                            LifeCycleHook callback,
                            boolean allowMultipleInvocations) {
        bindIfUnbound(entityClass);
        if (allowMultipleInvocations) {
            getEntityBinding(entityClass).bindTrigger(annotationClass, callback);
        } else {
            getEntityBinding(entityClass).bindTrigger(annotationClass, PersistentResource.CLASS_NO_FIELD, callback);
        }
    }

    /**
     * Binds a lifecycle hook to a particular entity class.   The hook will be called a single time per request
     * per class READ, CREATE, UPDATE, or DELETE.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param annotationClass (OnReadPostCommit, OnUpdatePreSecurity, etc)
     * @param callback The callback function to invoke.
     */
    public void bindTrigger(Class<?> entityClass,
                            Class<? extends Annotation> annotationClass,
                            LifeCycleHook callback) {
        bindTrigger(entityClass, annotationClass, callback, false);
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

            for (String relationship : getElideBoundRelationships(clazz)) {
                Class<?> relationshipClass = getParameterizedType(clazz, relationship);


                if (lookupBoundClass(relationshipClass) == null) {
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
     * @return true if the class is bound.  False otherwise.
     */
    public boolean hasBinding(Class<?> cls) {
        return bindJsonApiToEntity.contains(cls);
    }

    /**
     * Invoke the get[fieldName] method on the target object OR get the field with the corresponding name.
     * @param target the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @return the value
     */
    public Object getValue(Object target, String fieldName, RequestScope scope) {
        AccessibleObject accessor = getAccessibleObject(target, fieldName);
        try {
            if (accessor instanceof Method) {
                // Pass RequestScope into @Computed fields if requested
                if (isMethodRequestScopeable(target, (Method) accessor)) {
                    return ((Method) accessor).invoke(target, scope);
                }
                return ((Method) accessor).invoke(target);
            }
            if (accessor instanceof Field) {
                return ((Field) accessor).get(target);
            }
        } catch (IllegalAccessException e) {
            throw new InvalidAttributeException(fieldName, getJsonAliasFor(target.getClass()), e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        }
        throw new InvalidAttributeException(fieldName, getJsonAliasFor(target.getClass()));
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value the value to set
     */
    public void setValue(Object target, String fieldName, Object value) {
        Class<?> targetClass = target.getClass();
        String targetType = getJsonAliasFor(targetClass);

        String fieldAlias = fieldName;
        try {
            Class<?> fieldClass = getType(targetClass, fieldName);
            String realName = getNameFromAlias(target, fieldName);
            fieldAlias = (realName != null) ? realName : fieldName;
            String setMethod = "set" + StringUtils.capitalize(fieldAlias);
            Method method = EntityDictionary.findMethod(targetClass, setMethod, fieldClass);
            method.invoke(target, coerce(target, value, fieldAlias, fieldClass));
        } catch (IllegalAccessException e) {
            throw new InvalidAttributeException(fieldAlias, targetType, e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        } catch (IllegalArgumentException | NoSuchMethodException noMethod) {
            AccessibleObject accessor = getAccessibleObject(target, fieldAlias);
            if (accessor != null && accessor instanceof Field) {
                Field field = (Field) accessor;
                try {
                    field.set(target, coerce(target, value, fieldAlias, field.getType()));
                } catch (IllegalAccessException noField) {
                    throw new InvalidAttributeException(fieldAlias, targetType, noField);
                }
            } else {
                throw new InvalidAttributeException(fieldAlias, targetType);
            }
        }
    }

    /**
     * Handle an invocation target exception.
     *
     * @param e Exception the exception encountered while reflecting on an object's field
     * @return Equivalent runtime exception
     */
    private static RuntimeException handleInvocationTargetException(InvocationTargetException e) {
        Throwable exception = e.getTargetException();
        if (exception instanceof HttpStatusException || exception instanceof WebApplicationException) {
            return (RuntimeException) exception;
        }
        log.error("Caught an unexpected exception (rethrowing as internal server error)", e);
        return new InternalServerErrorException("Unexpected exception caught", e);
    }

    /**
     * Coerce provided value into expected class type.
     *
     * @param value provided value
     * @param fieldName the field name
     * @param fieldClass expected class type
     * @return coerced value
     */
    public Object coerce(Object target, Object value, String fieldName, Class<?> fieldClass) {
        if (fieldClass != null && Collection.class.isAssignableFrom(fieldClass) && value instanceof Collection) {
            return coerceCollection(target, (Collection) value, fieldName, fieldClass);
        }

        if (fieldClass != null && Map.class.isAssignableFrom(fieldClass) && value instanceof Map) {
            return coerceMap(target, (Map<?, ?>) value, fieldName);
        }

        return CoerceUtil.coerce(value, fieldClass);
    }

    private Collection coerceCollection(Object target, Collection<?> values, String fieldName, Class<?> fieldClass) {
        Class<?> providedType = getParameterizedType(target, fieldName);

        // check if collection is of and contains the correct types
        if (fieldClass.isAssignableFrom(values.getClass())) {
            boolean valid = true;
            for (Object member : values) {
                if (member != null && !providedType.isAssignableFrom(member.getClass())) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                return values;
            }
        }

        ArrayList<Object> list = new ArrayList<>(values.size());
        for (Object member : values) {
            list.add(CoerceUtil.coerce(member, providedType));
        }

        if (Set.class.isAssignableFrom(fieldClass)) {
            return new LinkedHashSet<>(list);
        }

        return list;
    }

    private Map coerceMap(Object target, Map<?, ?> values, String fieldName) {
        Class<?> keyType = getParameterizedType(target, fieldName, 0);
        Class<?> valueType = getParameterizedType(target, fieldName, 1);

        // Verify the existing Map
        if (isValidParameterizedMap(values, keyType, valueType)) {
            return values;
        }

        LinkedHashMap<Object, Object> result = new LinkedHashMap<>(values.size());
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            result.put(CoerceUtil.coerce(entry.getKey(), keyType), CoerceUtil.coerce(entry.getValue(), valueType));
        }

        return result;
    }

    private boolean isValidParameterizedMap(Map<?, ?> values, Class<?> keyType, Class<?> valueType) {
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ((key != null && !keyType.isAssignableFrom(key.getClass()))
                    || (value != null && !valueType.isAssignableFrom(value.getClass()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Binds the entity class if not yet bound.
     * @param entityClass the class to bind.
     */
    private void bindIfUnbound(Class<?> entityClass) {
        if (! isClassBound(entityClass)) {
            bindEntity(entityClass);
        }
    }
}
