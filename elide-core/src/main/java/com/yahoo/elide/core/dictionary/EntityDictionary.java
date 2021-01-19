/*
 * Copyright 2018, Yahoo Inc.
 * Copyright 2018, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.dictionary;

import static com.yahoo.elide.core.dictionary.EntityBinding.EMPTY_BINDING;
import static com.yahoo.elide.core.type.ClassType.COLLECTION_TYPE;
import static com.yahoo.elide.core.type.ClassType.MAP_TYPE;
import com.yahoo.elide.annotation.ApiVersion;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.HttpStatusException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.checks.Check;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.prefab.Collections.AppendOnly;
import com.yahoo.elide.core.security.checks.prefab.Collections.RemoveOnly;
import com.yahoo.elide.core.security.checks.prefab.Role;
import com.yahoo.elide.core.type.AccessibleObject;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.DynamicType;
import com.yahoo.elide.core.type.Field;
import com.yahoo.elide.core.type.Method;
import com.yahoo.elide.core.type.Package;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
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

    public static final String ELIDE_PACKAGE_PREFIX = "com.yahoo.elide";
    public static final String NO_VERSION = "";

    protected final ConcurrentHashMap<Pair<String, String>, Type<?>> bindJsonApiToEntity = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<Type<?>, EntityBinding> entityBindings = new ConcurrentHashMap<>();
    protected final CopyOnWriteArrayList<Type<?>> bindEntityRoots = new CopyOnWriteArrayList<>();
    protected final ConcurrentHashMap<Type<?>, List<Type<?>>> subclassingEntities = new ConcurrentHashMap<>();
    protected final BiMap<String, Class<? extends Check>> checkNames;

    @Getter
    protected final Set<String> apiVersions;

    @Getter
    protected final Injector injector;

    @Getter
    protected final Function<Class, Serde> serdeLookup ;

    public final static String REGULAR_ID_NAME = "id";
    private final static ConcurrentHashMap<Type, String> SIMPLE_NAMES = new ConcurrentHashMap<>();

    /**
     * Instantiate a new EntityDictionary with the provided set of checks. In addition all of the checks
     * in {@link com.yahoo.elide.core.security.checks.prefab} are mapped to {@code Prefab.CONTAINER.CHECK}
     * (e.g. {@code @ReadPermission(expression="Prefab.Role.All")}
     * or {@code @ReadPermission(expression="Prefab.Common.UpdateOnCreate")})
     * @param checks a map that links the identifiers used in the permission expression strings
     *               to their implementing classes
     */
    public EntityDictionary(Map<String, Class<? extends Check>> checks) {
        this.checkNames = Maps.synchronizedBiMap(HashBiMap.create(checks));
        this.apiVersions = new HashSet<>();
        this.serdeLookup = CoerceUtil::lookup;
        initializeChecks();

        //Default injector only injects Elide internals.
        this.injector = new Injector() {
            @Override
            public void inject(Object entity) {
                if (entity instanceof FilterExpressionCheck) {
                    try {
                        java.lang.reflect.Field field =
                                FilterExpressionCheck.class.getDeclaredField("dictionary");
                        field.setAccessible(true);
                        field.set(entity, EntityDictionary.this);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };
    }

    /**
     * Instantiate a new EntityDictionary with the provided set of checks and an injection function.
     * In addition all of the checks * in {@link com.yahoo.elide.core.security.checks.prefab} are mapped
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
        this.checkNames = Maps.synchronizedBiMap(HashBiMap.create(checks));
        this.apiVersions = new HashSet<>();
        initializeChecks();
        this.injector = injector;
    }

    private void initializeChecks() {
        addPrefabCheck("Prefab.Role.All", Role.ALL.class);
        addPrefabCheck("Prefab.Role.None", Role.NONE.class);
        addPrefabCheck("Prefab.Collections.AppendOnly", AppendOnly.class);
        addPrefabCheck("Prefab.Collections.RemoveOnly", RemoveOnly.class);
    }


    private void addPrefabCheck(String alias, Class<? extends Check> checkClass) {
        if (checkNames.containsKey(alias) || checkNames.inverse().containsKey(checkClass)) {
            return;
        }

        checkNames.put(alias, checkClass);
    }

    private static Package getParentPackage(Package pkg) {
        return pkg.getParentPackage();
    }

    /**
     * Cache the simple name of the provided class.
     * @param cls the {@code Class} object to be checked
     * @return simple name
     */
    public static String getSimpleName(Type<?> cls) {
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
    public static Method findMethod(Type<?> entityClass, String name, Type<?>... paramClass)
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
    public EntityBinding getEntityBinding(Type<?> entityClass) {

        //Common case of no inheritance.  This lookup is a performance boost so we don't have to do reflection.
        EntityBinding binding = entityBindings.get(entityClass);
        if (binding != null) {
            return binding;
        }

        Type<?> declaredClass = lookupBoundClass(entityClass);

        if (declaredClass != null) {
            return entityBindings.get(declaredClass);
        }

        //Will throw an exception if entityClass is not an entity.
        lookupEntityClass(entityClass);
        return EMPTY_BINDING;
    }

    /**
     * Returns whether or not the ID field for a given model is generated by the persistence layer.
     * @param entityClass The model to lookup.
     * @return True if the ID field is generated.  False otherwise.
     */
    public boolean isIdGenerated(Type<?> entityClass) {
        return getEntityBinding(entityClass).isIdGenerated();
    }

    /**
     * Returns the binding class for a given entity name.
     *
     * @param entityName entity name
     * @return binding class
     */
    public Type<?> getEntityClass(String entityName, String version) {
        Type<?> lookup = bindJsonApiToEntity.getOrDefault(Pair.of(entityName, version), null);

        if (lookup == null) {
            //Elide standard models transcend API versions.
            return entityBindings.values().stream()
                    .filter(binding -> binding.entityClass.getName().startsWith(ELIDE_PACKAGE_PREFIX))
                    .filter(binding -> binding.jsonApiType.equals(entityName))
                    .map(EntityBinding::getEntityClass)
                    .findFirst()
                    .orElse(null);
        }
        return lookup;
    }

    /**
     * Returns the Include name for a given binding class.
     *
     * @param entityClass the entity class
     * @return binding class
     * @see Include
     */
    public String getJsonAliasFor(Type<?> entityClass) {
        return getEntityBinding(entityClass).jsonApiType;
    }

    /**
     * Determine if a given (entity class, permission) pair have any permissions defined.
     *
     * @param resourceClass the entity class
     * @param annotationClass the permission annotation
     * @return {@code true} if that permission is defined anywhere within the class
     */
    public boolean entityHasChecksForPermission(Type<?> resourceClass, Class<? extends Annotation> annotationClass) {
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
    public ParseTree getPermissionsForClass(Type<?> resourceClass, Class<? extends Annotation> annotationClass) {
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
    public ParseTree getPermissionsForField(Type<?> resourceClass,
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
     * Fetch all entity classes that provided entity inherits from (i.e. all superclass entities down to,
     * but excluding Object).
     *
     * @param entityClass Entity class
     * @return  List of all super class entity classes
     */
    public List<Type<?>> getSuperClassEntities(Type<?> entityClass) {
        return getEntityBinding(entityClass).inheritedTypes.stream()
                .filter(entityBindings::containsKey)
                .collect(Collectors.toList());
    }

    /**
     * Get a list of inherited entities from a particular entity.
     * Namely, the list of entities inheriting from the provided class.
     *
     * @param entityClass Entity class
     * @return  List of all inherited entity types
     */
     public List<Type<?>> getSubclassingEntities(Type entityClass) {
         return subclassingEntities.computeIfAbsent(entityClass, unused -> entityBindings
                            .keySet().stream()
                            .filter(c -> c != entityClass && entityClass.isAssignableFrom(c))
                            .collect(Collectors.toList()));
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
    public String getIdFieldName(Type<?> entityClass) {
        return getEntityBinding(entityClass).getIdFieldName();
    }

    /**
     * Returns whether the entire entity uses Field or Property level access.
     * @param entityClass Entity Class
     * @return The JPA Access Type
     */
    public AccessType getAccessType(Type<?> entityClass) {
        return getEntityBinding(entityClass).getAccessType();
    }

    /**
     * Get all bound classes.
     *
     * @return the bound classes
     */
    public Set<Type<?>> getBoundClasses() {
        return entityBindings.keySet();
    }

    /**
     * Get all bound classes for a particular API version.
     *
     * @return the bound classes
     */
    public Set<Type<?>> getBoundClassesByVersion(String apiVersion) {
        return entityBindings.values().stream()
                .filter(binding ->
                        binding.getApiVersion().equals(apiVersion)
                                || binding.entityClass.getName().startsWith(ELIDE_PACKAGE_PREFIX)
                )
                .map(EntityBinding::getEntityClass)
                .collect(Collectors.toSet());
    }

    /**
     * Get all bindings.
     *
     * @return the bindings
     */
    public Set<EntityBinding> getBindings() {
        return new HashSet<>(entityBindings.values());
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
    public List<String> getAttributes(Type<?> entityClass) {
        return getEntityBinding(entityClass).apiAttributes;
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
        return getAttributes(getType(entity));
    }

    /**
     * Get the list of relationship names for an entity.
     *
     * @param entityClass entity name
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Type<?> entityClass) {
        return getEntityBinding(entityClass).apiRelationships;
    }

    /**
     * Get the list of relationship names for an entity.
     *
     * @param entity entity instance
     * @return List of relationship names for entity
     */
    public List<String> getRelationships(Object entity) {
        return getRelationships(getType(entity));
    }

    /**
     * Get a list of elide-bound relationships.
     *
     * @param entityClass Entity class to find relationships for
     * @return List of elide-bound relationship names.
     */
    public List<String> getElideBoundRelationships(Class<?> entityClass) {
        return getRelationships(entityClass).stream()
                .filter(relationName -> getBoundClasses().contains(getParameterizedType(entityClass, relationName)))
                .collect(Collectors.toList());
    }

    /**
     * Get a list of elide-bound relationships.
     *
     * @param entity Entity instance to find relationships for
     * @return List of elide-bound relationship names.
     */
    public List<String> getElideBoundRelationships(Object entity) {
        return getElideBoundRelationships(getType(entity));
    }

    /**
     * Determine whether or not a method is request scopeable.
     *
     * @param entity  Entity instance
     * @param method  Method on entity to check
     * @return True if method accepts a RequestScope, false otherwise.
     */
    public boolean isMethodRequestScopeable(Object entity, Method method) {
        return isMethodRequestScopeable(getType(entity), method);
    }

    /**
     * Determine whether or not a method is request scopeable.
     *
     * @param entityClass  Entity to check
     * @param method  Method on entity to check
     * @return True if method accepts a RequestScope, false otherwise.
     */
    public boolean isMethodRequestScopeable(Type<?> entityClass, Method method) {
        return getEntityBinding(entityClass).requestScopeableMethods.getOrDefault(method, false);
    }

    /**
     * Get a list of all fields including both relationships and attributes.
     *
     * @param entityClass entity name
     * @return List of all fields.
     */
    public List<String> getAllFields(Type<?> entityClass) {
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
        return getAllFields(getType(entity));
    }

    /**
     * Get the type of relationship from a relation.
     *
     * @param cls      Entity class
     * @param relation Name of relationship field
     * @return Relationship type. RelationshipType.NONE if is none found.
     */
    public RelationshipType getRelationshipType(Type<?> cls, String relation) {
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
    public String getRelationInverse(Type<?> cls, String relation) {
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
        final Type<?> inverseType = getParameterizedType(cls, relation);
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
        return getRelationshipType(getType(entity), relation);
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
    public Type<?> getType(Type<?> entityClass, String identifier) {
        if (identifier.equals(REGULAR_ID_NAME)) {
            return getEntityBinding(entityClass).getIdType();
        }

        ConcurrentHashMap<String, Type<?>> fieldTypes = getEntityBinding(entityClass).fieldsToTypes;
        return fieldTypes == null ? null : fieldTypes.get(identifier);
    }

    /**
     * Get a type for a field on an entity.
     *
     * @param entity     Entity instance
     * @param identifier Field to lookup type
     * @return Type of entity
     */
    public Type<?> getType(Object entity, String identifier) {
        return getType(getType(entity), identifier);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entityClass the entity class
     * @param identifier  the identifier
     * @return Entity type for field otherwise null.
     */
    public Type<?> getParameterizedType(Type<?> entityClass, String identifier) {
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
    public Type<?> getParameterizedType(Type<?> entityClass, String identifier, int paramIndex) {
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
    public Type<?> getParameterizedType(Object entity, String identifier) {
        return getParameterizedType(getType(entity), identifier);
    }

    /**
     * Retrieve the parameterized type for the given field.
     *
     * @param entity     Entity instance
     * @param identifier Field to lookup
     * @param paramIndex the index of the parameterization
     * @return Entity type for field otherwise null.
     */
    public Type<?> getParameterizedType(Object entity, String identifier, int paramIndex) {
        return getParameterizedType(getType(entity), identifier, paramIndex);
    }

    /**
     * Get the true field/method name from an alias.
     *
     * @param entityClass Entity name
     * @param alias       Alias to convert
     * @return Real field/method name as a string. null if not found.
     */
    public String getNameFromAlias(Type<?> entityClass, String alias) {
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
        return getNameFromAlias(getType(entity), alias);
    }

    /**
     * Initialize an entity.
     *
     * @param <T>    the type parameter
     * @param entity Entity to initialize
     */
    public <T> void initializeEntity(T entity) {
        Type type = getType(entity);
        if (entity != null) {
            EntityBinding binding = getEntityBinding(type);

            if (binding.isInjected()) {
                injector.inject(entity);
            }
        }
    }

    /**
     * Returns whether or not an entity is shareable.
     *
     * @param entityClass the entity type to check for the shareable permissions
     * @return true if entityClass is shareable.  False otherwise.
     */
    public boolean isTransferable(Type<?> entityClass) {
        NonTransferable nonTransferable = getAnnotation(entityClass, NonTransferable.class);

        return (nonTransferable == null || !nonTransferable.enabled());
    }

    /**
     * Add given Entity bean to dictionary.
     *
     * @param cls Entity bean class
     */
    public void bindEntity(Class<?> cls) {
        bindEntity(new ClassType(cls));
    }

    /**
     * Add given Entity bean to dictionary.
     *
     * @param cls Entity bean class
     */
    public void bindEntity(Type<?> cls) {
        bindEntity(cls, new HashSet<>());
    }

    /**
     * Add given Entity bean to dictionary.
     *
     * @param cls Entity bean class
     * @param hiddenAnnotations Annotations for hiding a field in API
     */
    public void bindEntity(Type<?> cls, Set<Class<? extends Annotation>> hiddenAnnotations) {
        Type<?> declaredClass = lookupIncludeClass(cls);

        if (declaredClass == null) {
            log.trace("Missing include or excluded class {}", cls.getName());
            return;
        }

        if (isClassBound(declaredClass)) {
            //Ignore duplicate bindings.
            return;
        }

        String type = getEntityName(declaredClass);
        String version = getModelVersion(declaredClass);

        bindJsonApiToEntity.put(Pair.of(type, version), declaredClass);
        apiVersions.add(version);
        entityBindings.put(declaredClass, new EntityBinding(this, declaredClass,
                type, version, hiddenAnnotations));

        Include include = declaredClass.getAnnotation(Include.class);
        if (include.rootLevel()) {
            bindEntityRoots.add(declaredClass);
        }
    }

    /**
     * Add an EntityBinding instance to dictionary.
     *
     * @param entityBinding EntityBinding instance
     */
    public void bindEntity(EntityBinding entityBinding) {
        Type<?> declaredClass = entityBinding.entityClass;

        if (isClassBound(declaredClass)) {
            //Ignore duplicate bindings.
            return;
        }

        Include include = (Include) getFirstAnnotation(declaredClass, Collections.singletonList(Include.class));

        String version = getModelVersion(declaredClass);
        bindJsonApiToEntity.put(Pair.of(entityBinding.jsonApiType, version), declaredClass);
        entityBindings.put(declaredClass, entityBinding);
        apiVersions.add(version);
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
        return getAnnotation(record.getResourceType(), annotationClass);
    }

    /**
     * Return annotation from class, parents or package.
     *
     * @param recordClass     the record class
     * @param annotationClass the annotation class
     * @param <A>             genericClass
     * @return the annotation
     */
    public <A extends Annotation> A getAnnotation(Type<?> recordClass, Class<A> annotationClass) {
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
    public <A extends Annotation> A getMethodAnnotation(Type<?> recordClass, String method, Class<A> annotationClass) {
        return getEntityBinding(recordClass).getMethodAnnotation(annotationClass, method);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Type<?> cls,
                                                                        LifeCycleHookBinding.Operation op,
                                                                        LifeCycleHookBinding.TransactionPhase phase,
                                                                        String fieldName) {
        return getEntityBinding(cls).getTriggers(op, phase, fieldName);
    }

    public <A extends Annotation> Collection<LifeCycleHook> getTriggers(Type<?> cls,
                                                                        LifeCycleHookBinding.Operation op,
                                                                        LifeCycleHookBinding.TransactionPhase phase) {
        return getEntityBinding(cls).getTriggers(op, phase);
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
    public <A extends Annotation> A getAttributeOrRelationAnnotation(Type<?> entityClass,
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
    public <A extends Annotation> A[] getAttributeOrRelationAnnotations(Type<?> entityClass,
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
    public static Annotation getFirstAnnotation(Type<?> entityClass,
                                                List<Class<? extends Annotation>> annotationClassList) {
        Annotation annotation = null;
        for (Type<?> cls = entityClass; annotation == null && cls != null; cls = cls.getSuperclass()) {
            for (Class<? extends Annotation> annotationClass : annotationClassList) {
                annotation = cls.getDeclaredAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
        }

        return getFirstPackageAnnotation(entityClass, annotationClassList);
    }

    /**
     * Return first matching annotation from a package or parent package.
     *
     * @param entityClass         Entity class type
     * @param annotationClassList List of sought annotations
     * @return annotation found
     */
    public static Annotation getFirstPackageAnnotation(Type<?> entityClass,
                                                       List<Class<? extends Annotation>> annotationClassList) {
        Annotation annotation = null;
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
    public boolean isRoot(Type<?> entityClass) {
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

            Type<?> valueClass = getType(value);

            for (; idField == null && valueClass != null; valueClass = valueClass.getSuperclass()) {
                try {
                    idField = getEntityBinding(valueClass).getIdField();
                } catch (NullPointerException e) {
                    log.warn("Class: {} ID Field: {}", valueClass.getSimpleName(), idField);
                }
            }

            Type<?> idClass;
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

            Serde serde = serdeLookup.apply(((ClassType) idClass).getCls());
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
    public Type<?> getIdType(Type<?> entityClass) {
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

        AccessibleObject idField = getEntityBinding(getType(value)).getIdField();
        if (idField != null) {
            return Arrays.asList(idField.getDeclaredAnnotations());
        }

        return Collections.emptyList();
    }

    /**
     * Follow for this class or super-class for JPA {@link Entity} annotation.
     *
     * @param objClass provided class
     * @return class with Entity annotation
     */
    public Type<?> lookupEntityClass(Type<?> objClass) {
        Type<?> declaringClass = lookupAnnotationDeclarationClass(objClass, Entity.class);
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
    public Type<?> lookupIncludeClass(Type<?> objClass) {
        Annotation first = getFirstAnnotation(objClass, Arrays.asList(Exclude.class, Include.class));
        if (first instanceof Include) {
            Type<?> declaringClass = lookupAnnotationDeclarationClass(objClass, Include.class);
            if (declaringClass != null) {
                return declaringClass;
            }

            //If we didn't find Include declared on a class, it must be declared at the package level.
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
    public static Type<?> lookupAnnotationDeclarationClass(Type<?> objClass,
                                                            Class<? extends Annotation> annotationClass) {
        for (Type<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
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
    public  Type<?> lookupBoundClass(Type<?> objClass) {
        //Common case - we can avoid reflection by checking the map ...
        EntityBinding binding = entityBindings.getOrDefault(objClass, EMPTY_BINDING);
        if (binding != EMPTY_BINDING) {
            return binding.entityClass;
        }

        Type<?> declaredClass = lookupIncludeClass(objClass);
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
    private boolean isClassBound(Type<?> objClass) {
        return (entityBindings.getOrDefault(objClass, EMPTY_BINDING) != EMPTY_BINDING);
    }

    /**
     * Check whether a class is a JPA entity.
     *
     * @param objClass class
     * @return True if it is a JPA entity
     */
    public final boolean isJPAEntity(Type<?> objClass) {
        try {
            lookupEntityClass(objClass);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Retrieve the accessible object for a field from a target object.
     *
     * @param target    the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @return the value
     */
    public AccessibleObject getAccessibleObject(Object target, String fieldName) {
        return getAccessibleObject(getType(target), fieldName);
    }

    public boolean isComputed(Type<?> entityClass, String fieldName) {
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
    public AccessibleObject getAccessibleObject(Type<?> targetClass, String fieldName) {
        return getEntityBinding(targetClass).fieldsToValues.get(fieldName);
    }

    /**
     * Retrieve fields from an object containing a particular type.
     *
     * @param targetClass Class to search for fields
     * @param targetType Type of fields to find
     * @return Set containing field names
     */
    public Set<String> getFieldsOfType(Type<?> targetClass, Type<?> targetType) {
        HashSet<String> fields = new HashSet<>();
        for (String field : getAllFields(targetClass)) {
            if (getParameterizedType(targetClass, field).equals(targetType)) {
                fields.add(field);
            }
        }
        return fields;
    }

    public boolean isRelation(Type<?> entityClass, String relationName) {
        return getEntityBinding(entityClass).apiRelationships.contains(relationName);
    }

    public boolean isAttribute(Type<?> entityClass, String attributeName) {
        return getEntityBinding(entityClass).apiAttributes.contains(attributeName);
    }

    /**
     * Scan for security checks and automatically bind them to the dictionary.
     */
    public void scanForSecurityChecks() {

        // Logic is based on https://github.com/illyasviel/elide-spring-boot/blob/master
        // /elide-spring-boot-autoconfigure/src/main/java/org/illyasviel/elide
        // /spring/boot/autoconfigure/ElideAutoConfiguration.java

        Set<Class<?>> classes = ClassScanner.getAnnotatedClasses(SecurityCheck.class);

        addSecurityChecks(classes);
    }

    /**
     * Add security checks and bind them to the dictionary.
     * @param classes Security check classes.
     */
    public void addSecurityChecks(Set<Class<?>> classes) {

        if (classes == null || classes.size() == 0) {
            return;
        }

        classes.forEach(this::addSecurityCheck);
    }

    /**
     * Add security checks and bind them to the dictionary.
     * @param cls Security check class.
     */
    public void addSecurityCheck(Class<?> cls) {
        if (Check.class.isAssignableFrom(cls)) {
            SecurityCheck securityCheckMeta = cls.getAnnotation(SecurityCheck.class);
            log.debug("Register Elide Check [{}] with expression [{}]",
                    cls.getCanonicalName(), securityCheckMeta.value());
            checkNames.put(securityCheckMeta.value(), cls.asSubclass(Check.class));
        } else {
            throw new IllegalStateException("Class annotated with SecurityCheck is not a Check");
        }
    }

    /**
     * Binds a lifecycle hook to a particular field or method in an entity.  The hook will be called a
     * single time per request per field READ, CREATE, or UPDATE.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param fieldOrMethodName The name of the field or method.
     * @param operation CREATE, READ, or UPDATE
     * @param phase PRESECURITY, PRECOMMIT, or POSTCOMMIT
     * @param hook The callback to invoke.
     */
    public void bindTrigger(Class<?> entityClass,
                            String fieldOrMethodName,
                            LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            LifeCycleHook hook) {
        bindTrigger(new ClassType(entityClass), fieldOrMethodName, operation, phase, hook);
    }

    /**
     * Binds a lifecycle hook to a particular field or method in an entity.  The hook will be called a
     * single time per request per field READ, CREATE, or UPDATE.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param fieldOrMethodName The name of the field or method.
     * @param operation CREATE, READ, or UPDATE
     * @param phase PRESECURITY, PRECOMMIT, or POSTCOMMIT
     * @param hook The callback to invoke.
     */
    public void bindTrigger(Type<?> entityClass,
                            String fieldOrMethodName,
                            LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            LifeCycleHook hook) {
        bindIfUnbound(entityClass);

        getEntityBinding(entityClass).bindTrigger(operation, phase, fieldOrMethodName, hook);
    }

    /**
     * Binds a lifecycle hook to a particular entity class.  The hook will either be called:
     *  - A single time single time per request per class READ, CREATE, UPDATE, or DELETE.
     *  - Multiple times per request per field READ, CREATE, or UPDATE.
     *
     * The behavior is determined by the value of the {@code allowMultipleInvocations} flag.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param operation CREATE, READ, or UPDATE
     * @param phase PRESECURITY, PRECOMMIT, or POSTCOMMIT
     * @param hook The callback to invoke.
     * @param allowMultipleInvocations Should the same life cycle hook be invoked multiple times for multiple
     *                                 CRUD actions on the same model.
     */
    public void bindTrigger(Class<?> entityClass,
                            LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            LifeCycleHook hook,
                            boolean allowMultipleInvocations) {
        bindTrigger(new ClassType(entityClass), operation, phase, hook, allowMultipleInvocations);
    }

    /**
     * Binds a lifecycle hook to a particular entity class.  The hook will either be called:
     *  - A single time single time per request per class READ, CREATE, UPDATE, or DELETE.
     *  - Multiple times per request per field READ, CREATE, or UPDATE.
     *
     * The behavior is determined by the value of the {@code allowMultipleInvocations} flag.
     * @param entityClass The entity that triggers the lifecycle hook.
     * @param operation CREATE, READ, or UPDATE
     * @param phase PRESECURITY, PRECOMMIT, or POSTCOMMIT
     * @param hook The callback to invoke.
     * @param allowMultipleInvocations Should the same life cycle hook be invoked multiple times for multiple
     *                                 CRUD actions on the same model.
     */
    public void bindTrigger(Type<?> entityClass,
                            LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            LifeCycleHook hook,
                            boolean allowMultipleInvocations) {
        bindIfUnbound(entityClass);

        if (allowMultipleInvocations) {
            getEntityBinding(entityClass).bindTrigger(operation, phase, hook);
        } else {
            getEntityBinding(entityClass).bindTrigger(operation, phase, PersistentResource.CLASS_NO_FIELD, hook);
        }
    }

    /**
     * Returns true if the relationship cascades deletes and false otherwise.
     * @param targetClass The class which owns the relationship.
     * @param fieldName The relationship
     * @return true or false
     */
    public boolean cascadeDeletes(Type<?> targetClass, String fieldName) {
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
    public <T> List<T> walkEntityGraph(Set<Type<?>> entities,  Function<Type<?>, T> transform) {
        ArrayList<T> results = new ArrayList<>();
        Queue<Type<?>> toVisit = new ArrayDeque<>(entities);
        Set<Type<?>> visited = new HashSet<>();
        while (! toVisit.isEmpty()) {
            Type<?> clazz = toVisit.remove();
            results.add(transform.apply(clazz));
            visited.add(clazz);

            for (String relationship : getElideBoundRelationships(clazz)) {
                Type<?> relationshipClass = getParameterizedType(clazz, relationship);


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
     * @param cls The class to verify.
     * @return true if the class is bound.  False otherwise.
     */
    public boolean hasBinding(Type<?> cls) {
        return entityBindings.values().stream()
                .anyMatch(binding -> binding.entityClass.equals(cls));
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
            throw new InvalidAttributeException(fieldName, getJsonAliasFor(getType(target)), e);
        } catch (InvocationTargetException e) {
            throw handleInvocationTargetException(e);
        }
        throw new InvalidAttributeException(fieldName, getJsonAliasFor(getType(target)));
    }

    /**
     * Sets the ID field of a target object.
     * @param target the object which owns the ID to set.
     * @param id the value to set
     */
    public void setId(Object target, String id) {
        setValue(target, getIdFieldName(lookupBoundClass(getType(target))), id);
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param target The object which owns the field to set
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value the value to set
     */
    public void setValue(Object target, String fieldName, Object value) {
        Type<?> targetClass = getType(target);
        String targetType = getJsonAliasFor(targetClass);

        String fieldAlias = fieldName;
        try {
            Type<?> fieldClass = getType(targetClass, fieldName);
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
     * @param fieldType expected class type
     * @return coerced value
     */
    public Object coerce(Object target, Object value, String fieldName, Type<?> fieldType) {
        Preconditions.checkState(fieldType instanceof ClassType);

        Class<?> fieldClass = ((ClassType) fieldType).getCls();
        if (fieldType != null
                && COLLECTION_TYPE.isAssignableFrom(fieldType)
                && value instanceof Collection) {
            return coerceCollection(target, (Collection) value, fieldName, fieldClass);
        }


        if (fieldType != null && MAP_TYPE.isAssignableFrom(fieldType) && value instanceof Map) {
            return coerceMap(target, (Map<?, ?>) value, fieldName);
        }

        return CoerceUtil.coerce(value, fieldClass);
    }

    private Collection coerceCollection(Object target, Collection<?> values, String fieldName, Class<?> fieldClass) {
        ClassType<?> providedType = (ClassType) getParameterizedType(target, fieldName);

        // check if collection is of and contains the correct types
        if (fieldClass.isAssignableFrom(values.getClass())) {
            boolean valid = true;
            for (Object member : values) {
                if (member != null && !providedType.isAssignableFrom(getType(member))) {
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
            list.add(CoerceUtil.coerce(member, providedType.getCls()));
        }

        if (Set.class.isAssignableFrom(fieldClass)) {
            return new LinkedHashSet<>(list);
        }

        return list;
    }

    private Map coerceMap(Object target, Map<?, ?> values, String fieldName) {
        Class<?> keyType = ((ClassType) getParameterizedType(target, fieldName, 0)).getCls();
        Class<?> valueType = ((ClassType) getParameterizedType(target, fieldName, 1)).getCls();

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

    /**
     * Returns whether or not a specified annotation is present on an entity field or its corresponding method.
     *
     * @param fieldName  The entity field
     * @param annotationClass  The provided annotation class
     *
     * @param <A>  The type of the {@code annotationClass}
     *
     * @return {@code true} if the field is annotated by the {@code annotationClass}
     */
    public <A extends Annotation> boolean attributeOrRelationAnnotationExists(
            Type<?> cls,
            String fieldName,
            Class<A> annotationClass
    ) {
        return getAttributeOrRelationAnnotation(cls, annotationClass, fieldName) != null;
    }

    /**
     * Returns whether or not a specified field exists in an entity.
     *
     * @param cls  The entity
     * @param fieldName  The provided field to check
     *
     * @return {@code true} if the field exists in the entity
     */
    public boolean isValidField(Type<?> cls, String fieldName) {
        return getAllFields(cls).contains(fieldName);
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
    private void bindIfUnbound(Type<?> entityClass) {
        /* This is safe to call with non-proxy objects. Not safe to call with ORM proxy objects. */

        if (! isClassBound(entityClass)) {
            bindEntity(entityClass);
        }
    }

    /**
     * Add a collection of argument to the attributes
     * @param cls The entity
     * @param attributeName attribute name to which argument has to be added
     * @param arguments Set of Argument type containing name and type of each argument.
     */
    public void addArgumentsToAttribute(Type<?> cls, String attributeName, Set<ArgumentType> arguments) {
        getEntityBinding(cls).addArgumentsToAttribute(attributeName, arguments);
    }

    /**
     * Add a single argument to the attribute
     * @param cls The entity
     * @param attributeName attribute name to which argument has to be added
     * @param argument A single argument
     */
    public void addArgumentToAttribute(Type<?> cls, String attributeName, ArgumentType argument) {
        this.addArgumentsToAttribute(cls, attributeName, Sets.newHashSet(argument));
    }

    /**
     * Returns the Collection of all attributes of an argument.
     * @param cls The entity
     * @param attributeName Name of the argument for ehich arguments are to be retrieved.
     * @return A Set of ArgumentType for the given attribute.
     */
    public Set<ArgumentType> getAttributeArguments(Type<?> cls, String attributeName) {
        return entityBindings.getOrDefault(cls, EMPTY_BINDING).getAttributeArguments(attributeName);
    }

    /**
     * Get column name using JPA.
     *
     * @param cls The entity class.
     * @param fieldName The entity attribute.
     * @return The jpa column name.
     */
    public String getAnnotatedColumnName(Type<?> cls, String fieldName) {
        Column[] column = getAttributeOrRelationAnnotations(cls, Column.class, fieldName);

        // this would only be valid for dimension columns
        JoinColumn[] joinColumn = getAttributeOrRelationAnnotations(cls, JoinColumn.class, fieldName);

        if (column == null || column.length == 0) {
            if (joinColumn == null || joinColumn.length == 0) {
                return fieldName;
            } else {
                return joinColumn[0].name();
            }
        } else {
            return column[0].name();
        }
    }

    /**
     * Returns the api version bound to a particular model class.
     * @param modelClass The model class to lookup.
     * @return The api version associated with the model or empty string if there is no association.
     */
    public static String getModelVersion(Type<?> modelClass) {
        ApiVersion apiVersionAnnotation =
                (ApiVersion) getFirstPackageAnnotation(modelClass, Arrays.asList(ApiVersion.class));

        return (apiVersionAnnotation == null) ? NO_VERSION : apiVersionAnnotation.version();
    }

    /**
     * Looks up the API model name for a given class.
     * @param modelClass The model class to lookup.
     * @return the API name for the model class.
     */
    public static String getEntityName(Type<?> modelClass) {
        Type<?> declaringClass = lookupAnnotationDeclarationClass(modelClass, Include.class);

        Preconditions.checkNotNull(declaringClass);
        Include include = declaringClass.getAnnotation(Include.class);

        if (! "".equals(include.type())) {
            return include.type();
        }

        Entity entity = (Entity) getFirstAnnotation(declaringClass, Arrays.asList(Entity.class));
        if (entity == null || "".equals(entity.name())) {
            return StringUtils.uncapitalize(declaringClass.getSimpleName());
        } else {
            return entity.name();
        }
    }

    public static <T> Type<T> getType(T object) {
        return object instanceof DynamicType
                ? ((DynamicType) object).getType()
                : new ClassType(object.getClass());
    }
}
