/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.OnCreate;
import com.yahoo.elide.annotation.OnDelete;
import com.yahoo.elide.annotation.OnUpdate;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.audit.InvalidSyntaxException;
import com.yahoo.elide.audit.LogMessage;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.core.filter.expression.Expression;
import com.yahoo.elide.core.filter.expression.PredicateExtractionVisitor;
import com.yahoo.elide.extensions.PatchRequestScope;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PermissionExecutor;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.text.WordUtils;

import javax.persistence.GeneratedValue;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Resource wrapper around Entity bean.
 *
 * @param <T> type of resource
 */
@ToString
public class PersistentResource<T> implements com.yahoo.elide.security.PersistentResource<T> {
    private final String type;
    protected T obj;
    private final ResourceLineage lineage;
    private final Optional<String> uuid;
    private final User user;
    private final ObjectEntityCache entityCache;

    @Override
    public String toString() {
        String id = (uuid.isPresent()) ? uuid.get() : getId();
        return String.format("PersistentResource { type=%s, id=%s }", type, id);
    }

    private final DataStoreTransaction transaction;
    @NonNull private final RequestScope requestScope;
    private final Optional<PersistentResource<?>> parent;
    private int hashCode = 0;

    /**
     * The Dictionary.
     */
    protected final EntityDictionary dictionary;

    /* Sort strings first by length then contents */
    private final Comparator<String> comparator = (string1, string2) -> {
        int diff = string1.length() - string2.length();
        return diff == 0 ? string1.compareTo(string2) : diff;
    };

    /**
     * Create a resource in the database.
     * @param parent - The immediate ancestor in the lineage or null if this is a root.
     * @param entityClass the entity class
     * @param requestScope the request scope
     * @param uuid the uuid
     * @param <T> object type
     * @return persistent resource
     */
    @SuppressWarnings("resource")
    public static <T> PersistentResource<T> createObject(
            PersistentResource<?> parent,
            Class<T> entityClass,
            RequestScope requestScope,
            String uuid) {
        DataStoreTransaction tx = requestScope.getTransaction();

        T obj = tx.createObject(entityClass);
        PersistentResource<T> newResource = new PersistentResource<>(obj, parent, uuid, requestScope);
        checkPermission(CreatePermission.class, newResource);
        newResource.auditClass(Audit.Action.CREATE, new ChangeSpec(newResource, null, null, newResource.getObject()));
        newResource.runTriggers(OnCreate.class);
        requestScope.queueCommitTrigger(newResource);

        String type = newResource.getType();
        requestScope.getObjectEntityCache().put(type, uuid, newResource.getObject());

        // Initialize null ToMany collections
        requestScope.getDictionary().getRelationships(entityClass).stream()
                .filter(relationName -> {
                    return newResource.getRelationshipType(relationName).isToMany()
                            && newResource.getValueUnchecked(relationName) == null;
                })
                .forEach(relationName -> newResource.setValue(relationName, new LinkedHashSet<>()));

        // Keep track of new resources for non shareable resources
        requestScope.getNewPersistentResources().add(newResource);
        newResource.markDirty();
        return newResource;
    }

    /**
     * Create a resource in the database.
     * @param entityClass the entity class
     * @param requestScope the request scope
     * @param uuid the uuid
     * @param <T> type of resource
     * @return persistent resource
     */
    public static <T> PersistentResource<T> createObject(Class<T> entityClass, RequestScope requestScope, String uuid) {
        return createObject(null, entityClass, requestScope, uuid);
    }

    /**
     * Constructor.
     *
     * @param parent the parent
     * @param obj the obj
     * @param requestScope the request scope
     */
    public PersistentResource(PersistentResource<?> parent, T obj, RequestScope requestScope) {
        this(obj, parent, requestScope);
    }

    /**
     * Constructor.
     *
     * @param obj the obj
     * @param requestScope the request scope
     */
    public PersistentResource(T obj, RequestScope requestScope) {
        this(obj, null, requestScope);
    }

    /**
     * Construct a new resource from the ID provided.
     *
     * @param obj the obj
     * @param parent the parent
     * @param id the id
     * @param requestScope the request scope
     */
    protected PersistentResource(@NonNull T obj, PersistentResource<?> parent, String id, RequestScope requestScope) {
        this.obj = obj;
        this.uuid = Optional.ofNullable(id);
        // TODO Use Bind annotation
        this.parent = Optional.ofNullable(parent);
        this.lineage = this.parent.isPresent() ? new ResourceLineage(parent.lineage, parent) : new ResourceLineage();
        this.dictionary = requestScope.getDictionary();
        this.type = dictionary.getJsonAliasFor(obj.getClass());
        this.user = requestScope.getUser();
        this.entityCache = requestScope.getObjectEntityCache();
        this.transaction = requestScope.getTransaction();
        this.requestScope = requestScope;
        dictionary.initializeEntity(obj);
    }

    /**
     * Constructor for testing.
     *
     * @param obj the obj
     * @param parent the parent
     * @param requestScope the request scope
     */
    protected PersistentResource(T obj, PersistentResource<?> parent, RequestScope requestScope) {
        this(obj, parent, requestScope.getObjectEntityCache().getUUID(obj), requestScope);
    }

    /**
     * Check whether an id matches for this persistent resource.
     *
     * @param checkId the check id
     * @return True if matches false otherwise
     */
    public boolean matchesId(String checkId) {
        if (checkId == null) {
            return false;
        } else if (uuid.isPresent() && checkId.equals(uuid.get())) {
            return true;
        }
        String id = getId();
        return !id.equals("0") && checkId.equals(id);
    }

    /**
     * Load an single entity from the DB.
     *
     * @param loadClass resource type
     * @param id the id
     * @param requestScope the request scope
     * @param <T> type of resource
     * @return resource persistent resource
     * @throws InvalidObjectIdentifierException the invalid object identifier exception
     */
    @SuppressWarnings("resource")
    @NonNull public static <T> PersistentResource<T> loadRecord(
            Class<T> loadClass, String id, RequestScope requestScope)
            throws InvalidObjectIdentifierException {
        Preconditions.checkNotNull(loadClass);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(requestScope);

        DataStoreTransaction tx = requestScope.getTransaction();
        EntityDictionary dictionary = requestScope.getDictionary();
        ObjectEntityCache cache = requestScope.getObjectEntityCache();

        // Check the resource cache if exists
        @SuppressWarnings("unchecked")
        T obj = (T) cache.get(dictionary.getJsonAliasFor(loadClass), id);
        if (obj == null) {
            // try to load object
            Class<?> idType = dictionary.getIdType(loadClass);
            obj = tx.loadObject(loadClass, (Serializable) CoerceUtil.coerce(id, idType));
            if (obj == null) {
                throw new InvalidObjectIdentifierException(id, loadClass.getSimpleName());
            }
        }

        PersistentResource<T> resource = new PersistentResource<>(obj, requestScope);
        // No need to have read access for a newly created object
        if (!requestScope.getNewResources().contains(resource)) {
            resource.checkFieldAwarePermissions(ReadPermission.class);
        }
        requestScope.queueCommitTrigger(resource);

        return resource;
    }

    /**
     * Load a collection from the datastore.
     *
     * @param <T> the type parameter
     * @param loadClass the load class
     * @param requestScope the request scope
     * @return a filtered collection of resources loaded from the datastore.
     */
    @NonNull public static <T> Set<PersistentResource<T>> loadRecords(Class<T> loadClass, RequestScope requestScope) {
        DataStoreTransaction tx = requestScope.getTransaction();

        if (shouldSkipCollection(loadClass, ReadPermission.class, requestScope)) {
            return Collections.emptySet();
        }

        Iterable<T> list;
        FilterScope filterScope = new FilterScope(requestScope, loadClass);
        list = tx.loadObjects(loadClass, filterScope);
        Set<PersistentResource<T>> resources = new PersistentResourceSet(list, requestScope);
        resources = filter(ReadPermission.class, resources);
        for (PersistentResource<T> resource : resources) {
            requestScope.queueCommitTrigger(resource);
        }
        return resources;
    }

    /**
     * Load a collection from the datastore.
     *
     * @param <T> the type parameter
     * @param loadClass the load class
     * @param requestScope the request scope
     * @return a filtered collection of resources loaded from the datastore.
     */
    @NonNull public static <T> Set<PersistentResource<T>> loadRecordsWithSortingAndPagination(
            Class<T> loadClass, RequestScope requestScope) {
        DataStoreTransaction tx = requestScope.getTransaction();

        if (shouldSkipCollection(loadClass, ReadPermission.class, requestScope)) {
            return Collections.emptySet();
        }

        Iterable<T> list;
        FilterScope filterScope = new FilterScope(requestScope, loadClass);
        list = tx.loadObjectsWithSortingAndPagination(loadClass, filterScope);
        Set<PersistentResource<T>> resources = new PersistentResourceSet(list, requestScope);
        resources = filter(ReadPermission.class, resources);
        for (PersistentResource<T> resource : resources) {
            requestScope.queueCommitTrigger(resource);
        }
        return resources;
    }

    /**
     * Update attribute in existing resource.
     *
     * @param fieldName the field name
     * @param newVal the new val
     * @return true if object updated, false otherwise
     */
    public boolean updateAttribute(String fieldName, Object newVal) {
        Object val = getValueUnchecked(fieldName);
        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, newVal, val);
        if (val != newVal && (val == null || !val.equals(newVal))) {
            this.setValueChecked(fieldName, newVal);
            this.markDirty();
            return true;
        }
        return false;
    }

    /**
     * Perform a full replacement on relationships.
     * Here is an example:
     * The following are ids for a hypothetical relationship.
     * GIVEN:
     * all (all the ids in the DB) = 1,2,3,4,5
     * mine (everything the current user has access to) = 1,2,3
     * requested (what the user wants to change to) = 3,6
     * THEN:
     * deleted (what gets removed from the DB) = 1,2
     * final (what get stored in the relationship) = 3,4,5,6
     * BECAUSE:
     * notMine = all - mine
     * updated = (requested UNION mine) - (requested INTERSECT mine)
     * deleted = (mine - requested)
     * final = (notMine) UNION requested
     * @param fieldName the field name
     * @param resourceIdentifiers the resource identifiers
     * @return True if object updated, false otherwise
     */
    public boolean updateRelation(String fieldName, Set<PersistentResource> resourceIdentifiers) {
        RelationshipType type = getRelationshipType(fieldName);
        Set<PersistentResource> resources = filter(
                ReadPermission.class,
                (Set) getRelationUncheckedUnfiltered(fieldName)
        );
        if (type.isToMany()) {
            checkFieldAwareDeferPermissions(
                    UpdatePermission.class,
                    fieldName,
                    resourceIdentifiers.stream().map(PersistentResource::getObject).collect(Collectors.toList()),
                    resources.stream().map(PersistentResource::getObject).collect(Collectors.toList())
            );
            return updateToManyRelation(fieldName, resourceIdentifiers, resources);
        } else { // To One Relationship
            PersistentResource resource = (resources.isEmpty()) ? null : resources.iterator().next();
            Object original = (resource == null) ? null : resource.getObject();
            PersistentResource modifiedResource =
                    (resourceIdentifiers == null || resourceIdentifiers.isEmpty()) ? null
                            : resourceIdentifiers.iterator().next();
            Object modified = (modifiedResource == null) ? null : modifiedResource.getObject();
            checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, modified, original);
            return updateToOneRelation(fieldName, resourceIdentifiers, resources);
        }
    }

    /**
     * Updates a to-many relationship.
     * @param fieldName the field name
     * @param resourceIdentifiers the resource identifiers
     * @param mine Existing, filtered relationships for field name
     * @return true if updated. false otherwise
     */
    protected boolean updateToManyRelation(String fieldName,
                                           Set<PersistentResource> resourceIdentifiers,
                                           Set<PersistentResource> mine) {

        Set<PersistentResource> requested;
        Set<PersistentResource> updated;
        Set<PersistentResource> deleted;
        Set<PersistentResource> added;

        if (resourceIdentifiers == null) {
            throw new InvalidEntityBodyException("Bad relation data");
        }
        if (resourceIdentifiers.isEmpty()) {
            requested = new LinkedHashSet<>();
        } else {

            // TODO - this resource does not include a lineage. This could cause issues for audit.
            requested = resourceIdentifiers;
        }

        // deleted = mine - requested
        deleted = Sets.difference(mine, requested);

        // updated = (requested UNION mine) - (requested INTERSECT mine)
        updated = Sets.difference(
                Sets.union(mine, requested),
                Sets.intersection(mine, requested)
        );

        added = Sets.difference(updated, deleted);

        checkSharePermission(added);

        Collection collection = (Collection) this.getValueUnchecked(fieldName);

        if (collection == null) {
            this.setValue(fieldName, mine);
        }

        deleted
                .stream()
                .forEach(toDelete -> {
                    delFromCollection(collection, fieldName, toDelete);
                    deleteInverseRelation(fieldName, toDelete.getObject());
                });

        added
                .stream()
                .forEach(toAdd -> {
                    addToCollection(collection, fieldName, toAdd);
                    addInverseRelation(fieldName, toAdd.getObject());
                });


        if (!updated.isEmpty()) {
            this.markDirty();
        }

        return !updated.isEmpty();
    }

    /**
     * Update a 2-one relationship.
     *
     * @param fieldName the field name
     * @param resourceIdentifiers the resource identifiers
     * @param mine Existing, filtered relationships for field name
     * @return true if updated. false otherwise
     */
    protected boolean updateToOneRelation(String fieldName,
                                          Set<PersistentResource> resourceIdentifiers,
                                          Set<PersistentResource> mine) {
        Object newValue = null;
        PersistentResource newResource = null;
        if (resourceIdentifiers != null && !resourceIdentifiers.isEmpty()) {
            newResource = resourceIdentifiers.iterator().next();
            newValue = newResource.getObject();
        }

        PersistentResource oldResource = !mine.isEmpty() ? mine.iterator().next() : null;

        if (oldResource == null) {
            if (newValue == null) {
                return false;
            }
            checkSharePermission(resourceIdentifiers);
        } else if (oldResource.getObject().equals(newValue)) {
            return false;
        } else {
            checkSharePermission(resourceIdentifiers);
            if (hasInverseRelation(fieldName)) {
                deleteInverseRelation(fieldName, oldResource.getObject());
                oldResource.markDirty();
            }
        }

        if (newValue != null) {
            if (hasInverseRelation(fieldName)) {
                addInverseRelation(fieldName, newValue);
                newResource.markDirty();
            }
        }

        this.setValueChecked(fieldName, newValue);

        this.markDirty();
        return true;
    }

    /**
     * Clear all elements from a relation.
     *
     * @param relationName Name of relation to clear
     * @return True if object updated, false otherwise
     */
    public boolean clearRelation(String relationName) {
        Set<PersistentResource> mine = filter(ReadPermission.class, (Set) getRelationUncheckedUnfiltered(relationName));
        checkFieldAwarePermissions(UpdatePermission.class, relationName, Collections.emptySet(),
                mine.stream().map(PersistentResource::getObject).collect(Collectors.toSet()));

        if (mine.isEmpty()) {
            return false;
        }

        RelationshipType type = getRelationshipType(relationName);

        mine.stream()
                .forEach(toDelete -> {
                    if (hasInverseRelation(relationName)) {
                        deleteInverseRelation(relationName, toDelete.getObject());
                        toDelete.markDirty();
                    }
                });

        if (type.isToOne()) {
            PersistentResource oldValue = mine.iterator().next();
            if (oldValue != null && oldValue.getObject() != null) {
                this.nullValue(relationName, oldValue);
                oldValue.markDirty();
                this.markDirty();
            }
        } else {
            Collection collection = (Collection) getValueUnchecked(relationName);
            if (collection != null && !collection.isEmpty()) {
                mine.stream()
                        .forEach(toDelete -> {
                            delFromCollection(collection, relationName, toDelete);
                            if (hasInverseRelation(relationName)) {
                                toDelete.markDirty();
                            }
                        });
                this.markDirty();
            }
        }

        return true;
    }

    /**
     * Remove a relationship.
     *
     * @param fieldName the field name
     * @param removeResource the remove resource
     */
    public void removeRelation(String fieldName, PersistentResource removeResource) {
        Object relation = getValueUnchecked(fieldName);
        Object original = relation;
        Object modified = null;

        if (relation instanceof Collection) {
            original = copyCollection((Collection) relation);
        }

        if (relation instanceof Collection && removeResource != null) {
            modified = CollectionUtils.disjunction(
                    (Collection) relation,
                    Collections.singleton(removeResource.getObject())
            );
        }

        checkFieldAwarePermissions(UpdatePermission.class, fieldName, modified, original);

        if (relation instanceof Collection) {
            if (!((Collection) relation).contains(removeResource.getObject())) {

                //Nothing to do
                return;
            }
            delFromCollection((Collection) relation, fieldName, removeResource);
        } else {
            if (relation == null || !relation.equals(removeResource.getObject())) {
                //Nothing to do
                return;
            }
            this.nullValue(fieldName, removeResource);
        }

        if (hasInverseRelation(fieldName)) {
            deleteInverseRelation(fieldName, removeResource.getObject());
            removeResource.markDirty();
        }

        if (original != modified && original != null && !original.equals(modified)) {
            this.markDirty();
        }
    }

    /**
     * Add relation link from a given parent resource to a child resource.
     *
     * @param fieldName which relation link
     * @param newRelation the new relation
     */
    public void addRelation(String fieldName, PersistentResource newRelation) {
        checkSharePermission(Collections.singleton(newRelation));
        Object relation = this.getValueUnchecked(fieldName);

        if (relation instanceof Collection) {
            if (addToCollection((Collection) relation, fieldName, newRelation)) {
                this.markDirty();
            }
            addInverseRelation(fieldName, newRelation.getObject());
        } else {
            // Not a collection, but may be trying to create a ToOne relationship.
            // NOTE: updateRelation marks dirty.
            updateRelation(fieldName, Collections.singleton(newRelation));
            return;
        }
    }

    /**
     * Check if adding or updating a relation is allowed.
     *
     * @param resourceIdentifiers The persistent resources that are being added
     */
    protected void checkSharePermission(Set<PersistentResource> resourceIdentifiers) {
        if (resourceIdentifiers == null) {
            return;
        }

        final Set<PersistentResource> newResources = getRequestScope().getNewPersistentResources();

        for (PersistentResource persistentResource : resourceIdentifiers) {
            if (!newResources.contains(persistentResource)
                    && !lineage.getRecord(persistentResource.getType()).contains(persistentResource)) {
                checkPermission(SharePermission.class, persistentResource);
            }
        }
    }

    /**
     * Delete an existing entity.
     *
     * @throws ForbiddenAccessException the forbidden access exception
     */
    public void deleteResource() throws ForbiddenAccessException {
        checkPermission(DeletePermission.class, this);

        /*
         * Search for bidirectional relationships.  For each bidirectional relationship,
         * we need to remove ourselves from that relationship
         */
        Map<String, Relationship> relations = getRelationships();
        for (Map.Entry<String, Relationship> entry : relations.entrySet()) {
            String relationName = entry.getKey();
            String inverseRelationName = dictionary.getRelationInverse(getResourceClass(), relationName);
            if (!inverseRelationName.equals("")) {
                for (PersistentResource inverseResource : getRelationCheckedFiltered(relationName)) {
                    if (hasInverseRelation(relationName)) {
                        deleteInverseRelation(relationName, inverseResource.getObject());
                        inverseResource.markDirty();
                    }
                }
            }
        }

        transaction.delete(getObject());
        auditClass(Audit.Action.DELETE, new ChangeSpec(this, null, getObject(), null));
        runTriggers(OnDelete.class);
    }

    /**
     * Get resource ID.
     *
     * @return ID id
     */
    public String getId() {
        return dictionary.getId(getObject());
    }

    /**
     * Set resource ID.
     *
     * @param id resource id
     */
    public void setId(String id) {
        this.setValue(dictionary.getIdFieldName(getResourceClass()), id);
    }

    /**
     * Indicates if the ID is generated or not.
     *
     * @return Boolean
     */
    public boolean isIdGenerated() {
        return getIdAnnotations().stream().anyMatch(a -> a.annotationType().equals(GeneratedValue.class));
    }

    /**
     * Returns annotations applied to the ID field.
     *
     * @return Collection of Annotations
     */
    private Collection<Annotation> getIdAnnotations() {
        return dictionary.getIdAnnotations(getObject());
    }

    /**
     * Gets UUID.
     * @return the UUID
     */
    public Optional<String> getUUID() {
        return uuid;
    }


    /**
     * Load a single entity relation from the PersistentResource.
     *
     * @param relation the relation
     * @param id the id
     * @return PersistentResource relation
     */
    public PersistentResource getRelation(String relation, String id) {

        Optional<Expression> filterExpression;
        boolean skipNew = false;
        // Criteria filtering not supported in Patch extension
        if (requestScope instanceof PatchRequestScope) {
            filterExpression = Optional.empty();
            // NOTE: We can safely _skip_ tests here since we are only skipping READ checks on
            // NEWLY created objects. We assume a user can READ their object in the midst of creation.
            // Imposing a constraint to the contrary-- at this moment-- seems arbitrary and does not
            // reflect reality (i.e. if a user is creating an object with values, he/she knows those values
            // already).
            skipNew = true;
        } else {
            Class<?> entityType = dictionary.getParameterizedType(getResourceClass(), relation);
            if (entityType == null) {
                throw new InvalidAttributeException(relation, type);
            }
            Class<?> idType = dictionary.getIdType(entityType);
            Object idVal = CoerceUtil.coerce(id, idType);
            String idField = dictionary.getIdFieldName(entityType);

            List<Predicate.PathElement> path = Lists.newArrayList(
                new Predicate.PathElement(
                    getResourceClass(),
                    getType(),
                    entityType,
                    relation
                ),
                new Predicate.PathElement(
                    entityType,
                    relation,
                    idType,
                    idField
                )
            );

            filterExpression = Optional.of(new Predicate(
                    path,
                    Operator.IN,
                    Collections.singletonList(idVal)));
        }

        Set<PersistentResource> resources =
                filter(ReadPermission.class,
                (Set) getRelationChecked(relation, filterExpression),
                skipNew);

        for (PersistentResource childResource : resources) {
            if (childResource.matchesId(id)) {
                return childResource;
            }
        }
        throw new InvalidObjectIdentifierException(id, relation);
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @return collection relation
     */
    public Set<PersistentResource> getRelationCheckedFiltered(String relationName) {
        return filter(ReadPermission.class, (Set) getRelation(relationName, true));
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @return collection relation
     */
    public Set<PersistentResource> getRelationCheckedFilteredWithSortingAndPagination(String relationName) {
        return filter(ReadPermission.class, (Set) getRelationWithSortingAndPagination(relationName, true));
    }

    private Set<PersistentResource> getRelationUncheckedUnfiltered(String relationName) {
        return getRelation(relationName, false);
    }

    private Set<PersistentResource> getRelation(String relationName, boolean checked) {

        if (checked && !checkRelation(relationName)) {
            return Collections.emptySet();
        }

        Optional<Expression> expression = getExpressionForRelation(relationName);

        return getRelationUnchecked(relationName, expression);
    }

    private Optional<Expression> getExpressionForRelation(String relationName) {
        final Class<?> entityClass = dictionary.getParameterizedType(obj, relationName);
        if (entityClass == null) {
            throw new InvalidAttributeException(relationName, type);
        }
        final String valType = dictionary.getJsonAliasFor(entityClass);
        return requestScope.getFilterExpressionByType(valType);
    }

    /**
     * Gets the relational entities to a entity (author/1/books) - books would be fetched here.
     * @param relationName The relationship name - eg. books
     * @param checked The flag to denote if we are doing security checks on this relationship
     * @return The resulting records from underlying data store
     */
    private Set<PersistentResource> getRelationWithSortingAndPagination(String relationName, boolean checked) {
        if (checked && !checkRelation(relationName)) {
            return Collections.emptySet();
        }

        Optional<Expression> filterExpression = getExpressionForRelation(relationName);
        final boolean hasSortingRules = !requestScope.getSorting().isDefaultInstance();
        final boolean hasPagination = !requestScope.getPagination().isDefaultInstance();
        return (hasSortingRules || hasPagination)
                ? getRelationUncheckedWithSortingAndPagination(relationName, filterExpression)
                : getRelationUnchecked(relationName, filterExpression);
    }

    /**
     * Check the permissions of the relationship, and return true or false.
     * @param relationName The relationship to the entity
     * @return True if the relationship to the entity has valid permissions for the user
     */
    protected boolean checkRelation(String relationName) {
        List<String> relations = dictionary.getRelationships(obj);

        String realName = dictionary.getNameFromAlias(obj, relationName);
        relationName = (realName == null) ? relationName : realName;

        if (relationName == null || relations == null || !relations.contains(relationName)) {
            throw new InvalidAttributeException(relationName, type);
        }

        checkFieldAwareDeferPermissions(ReadPermission.class, relationName, null, null);

        // Check for permission to the relationship and to the underlying type to avoid iterating a lazy collection
        if (shouldSkipCollection(ReadPermission.class, relationName)) {
            return false;
        }

        try {
            // If we cannot read any element of this type, don't try to filter
            requestScope.getPermissionExecutor().checkUserPermissions(
                    dictionary.getParameterizedType(obj, relationName),
                    ReadPermission.class);
        } catch (ForbiddenAccessException e) {
            return false;
        }
        return true;
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @param filterExpression An optional filter expression
     * @return collection relation
     */
    protected Set<PersistentResource> getRelationChecked(String relationName, Optional<Expression> filterExpression) {
        if (!checkRelation(relationName)) {
            return Collections.emptySet();
        }
        return getRelationUnchecked(relationName, filterExpression);

    }

    /**
     * Retrieve an uncheck set of relations.
     *
     * @param relationName field
     * @param filterExpression An optional filter expression
     * @return the resources in the relationship
     */
    private Set<PersistentResource> getRelationUnchecked(String relationName, Optional<Expression> filterExpression) {
        RelationshipType type = getRelationshipType(relationName);
        final Class<?> entityClass = dictionary.getParameterizedType(obj, relationName);

        Object val;

        /* If elide was configured for Elide 3.0 data store interface */
        if (requestScope.useFilterExpressions()) {
            val = requestScope.getTransaction()
                    .getRelation(obj, type, relationName, entityClass, dictionary, filterExpression);

        /* Otherwise use the Elide 2.0 interface */
        } else {

            /* Convert the expression to a set of predicates */
            Set<Predicate> filters;
            PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
            if (filterExpression.isPresent()) {
                filters = filterExpression.get().accept(visitor);
            } else {
                filters = Collections.emptySet();
            }
            val = requestScope.getTransaction()
                    .getRelation(obj, type, relationName, entityClass, dictionary, filters);
        }

        if (val == null) {
            return Collections.emptySet();
        }

        Set<PersistentResource<Object>> resources = Sets.newLinkedHashSet();
        if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;
            resources = new PersistentResourceSet(this, filteredVal, requestScope);
        } else if (type.isToOne()) {
            resources = new SingleElementSet(new PersistentResource(this, val, requestScope));
        } else {
            resources.add(new PersistentResource(this, val, requestScope));
        }

        return (Set) resources;
    }

    /**
     * Fetches the relationship entities with sorting and pagination support via HQLTransaction.
     * @param relationName The entity whose relationships we want to fetch
     * @param filterExpression An optional filter expression
     * @return The persistent resources
     */
    private Set<PersistentResource> getRelationUncheckedWithSortingAndPagination(
            String relationName,
            Optional<Expression> filterExpression) {
        RelationshipType type = getRelationshipType(relationName);
        final Class<?> entityClass = dictionary.getParameterizedType(obj, relationName);

        Object val;

        /* If elide was configured for Elide 3.0 data store interface */
        if (requestScope.useFilterExpressions()) {
            val = requestScope.getTransaction()
                    .getRelationWithSortingAndPagination(obj, type, relationName, entityClass, dictionary,
                            filterExpression, requestScope.getSorting(), requestScope.getPagination());

        /* Otherwise use the Elide 2.0 interface */
        } else {
            /* Convert the expression to a set of predicates */
            Set<Predicate> filters;
            PredicateExtractionVisitor visitor = new PredicateExtractionVisitor();
            if (filterExpression.isPresent()) {
                filters = filterExpression.get().accept(visitor);
            } else {
                filters = Collections.emptySet();
            }
            val = requestScope.getTransaction()
                    .getRelationWithSortingAndPagination(obj, type, relationName, entityClass, dictionary, filters,
                            requestScope.getSorting(), requestScope.getPagination());
        }

        if (val == null) {
            return Collections.emptySet();
        }

        Set<PersistentResource<Object>> resources = Sets.newLinkedHashSet();
        if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;
            resources = new PersistentResourceSet(this, filteredVal, requestScope);
        } else if (type.isToOne()) {
            resources = new SingleElementSet(new PersistentResource(this, val, requestScope));
        } else {
            resources.add(new PersistentResource(this, val, requestScope));
        }

        return (Set) resources;
    }

    /**
     * Determine whether the user has permissions to a collection. Prevents lazy collections from
     * being instantiated for no reason.
     *
     * @param annotationClass Annotation class
     * @param relationName Field
     * @param <A> type parameter
     * @return True if collection should be skipped (i.e. denied access), false otherwise
     */
    private <A extends Annotation> boolean shouldSkipCollection(Class<A> annotationClass, String relationName) {
        final PermissionExecutor executor = requestScope.getPermissionExecutor();
        try {
            executor.checkUserPermissions(this, annotationClass, relationName);
            executor.checkUserPermissions(dictionary.getParameterizedType(obj, relationName), ReadPermission.class);
        } catch (ForbiddenAccessException e) {
            return true;
        }
        return false;
    }

    /**
     * Determine whether or not to skip loading a collection.
     *
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param requestScope Request scope
     * @param <A> type parameter
     * @return True if collection should be skipped (i.e. denied access), false otherwise
     */
    private static <A extends Annotation> boolean shouldSkipCollection(Class<?> resourceClass,
                                                                       Class<A> annotationClass,
                                                                       RequestScope requestScope) {
        try {
            requestScope.getPermissionExecutor().checkUserPermissions(resourceClass, annotationClass);
        } catch (ForbiddenAccessException e) {
            return true;
        }
        return false;
    }

    /**
     * Get a relationship type.
     *
     * @param relation Name of relationship
     * @return Relationship type. RelationshipType.NONE if not found.
     */
    public RelationshipType getRelationshipType(String relation) {
        return dictionary.getRelationshipType(obj, relation);
    }

    /**
     * Get the value for a particular attribute (i.e. non-relational field)
     * @param attr Attribute name
     * @return Object value for attribute
     */
    public Object getAttribute(String attr) {
        return this.getValueChecked(attr);
    }

    /**
     * Wrapped Entity bean.
     *
     * @return bean object
     */
    public T getObject() {
        return obj;
    }

    /**
     * Sets object.
     * @param obj the obj
     */
    public void setObject(T obj) {
        this.obj = obj;
    }

    /**
     * Entity type.
     *
     * @return type resource class
     */
    @JsonIgnore
    public Class<T> getResourceClass() {
        return (Class) dictionary.lookupEntityClass(obj.getClass());
    }

    /**
     * Gets type.
     * @return the type
     */
    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            final int prime = 31;
            int result = 1;
            // NOTE: UUID's are only present in the case of newly created objects.
            // Consequently, a known ID will never be present during processing (only after commit
            // assigned by the DB) and so we can assume that any newly created object can be fully
            // addressed by its UUID. It is possible for UUID and id to be unset upon a POST or PATCH
            // ext request, but it is safe to ignore these edge cases.
            //     (1) In a POST request, you would not be referencing this newly created object in any way
            //         so this is not an issue.
            //     (2) In a PATCH ext request, this is also acceptable (assuming request is accepted) in the way
            //         that it is acceptable in a POST. If you do not specify a UUID, there is no way to reference
            //         that newly created object within the context of the request. Thus, if any such action was
            //         required, the user would be forced to provide a UUID anyway.
            String id = dictionary.getId(getObject());
            if (uuid.isPresent() && "0".equals(id)) {
                result = prime * result + uuid.hashCode();
            } else {
                result = prime * result + (id == null ? 0 : id.hashCode());
            }
            result = prime * result + (type == null ? 0 : type.hashCode());
            hashCode = result;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PersistentResource) {
            PersistentResource that = (PersistentResource) obj;
            if (this.getObject() == that.getObject()) {
                return true;
            }
            String theirId = dictionary.getId(that.getObject());
            return this.matchesId(theirId) && Objects.equals(this.type, that.type);
        }
        return false;
    }

    /**
     * Gets lineage.
     * @return the lineage
     */
    public ResourceLineage getLineage() {
        return this.lineage;
    }

    /**
     * Gets dictionary.
     * @return the dictionary
     */
    public EntityDictionary getDictionary() {
        return dictionary;
    }

    /**
     * Gets request scope.
     * @return the request scope
     */
    public RequestScope getRequestScope() {
        return requestScope;
    }

    /**
     * Convert a persistent resource to a resource.
     *
     * @return a resource
     */
    public Resource toResource() {
        return toResource(this::getRelationships, this::getAttributes);
    }

    /**
     * Fetch a resource with support for lambda function for getting relationships and attributes.
     * @return The Resource
     */
    public Resource toResourceWithSortingAndPagination() {
        return toResource(this::getRelationshipsWithSortingAndPagination, this::getAttributes);
    }

    /**
     * Fetch a resource with support for lambda function for getting relationships and attributes.
     * @param relationshipSupplier The relationship supplier (getRelationships())
     * @return The Resource
     */
    public Resource toResource(final Supplier<Map<String, Relationship>> relationshipSupplier,
                               final Supplier<Map<String, Object>> attributeSupplier) {
        final Resource resource = new Resource(type, (obj == null)
                ? uuid.orElseThrow(
                () -> new InvalidEntityBodyException("No id found on object"))
                : dictionary.getId(obj));
        resource.setRelationships(relationshipSupplier.get());
        resource.setAttributes(attributeSupplier.get());
        return resource;
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationships() {
        return getRelationshipsWithRelationshipFunction(this::getRelationCheckedFiltered);
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationshipsWithSortingAndPagination() {
        return getRelationshipsWithRelationshipFunction(this::getRelationCheckedFilteredWithSortingAndPagination);
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationshipsWithRelationshipFunction(
            final Function<String, Set<PersistentResource>> relationshipFunction) {
        final Map<String, Relationship> relationshipMap = new LinkedHashMap<>();
        final Set<String> relationshipFields = filterFields(dictionary.getRelationships(obj));

        for (String field : relationshipFields) {
            TreeMap<String, Resource> orderedById = new TreeMap<>(comparator);
            for (PersistentResource relationship : relationshipFunction.apply(field)) {
                orderedById.put(relationship.getId(),
                        new ResourceIdentifier(relationship.getType(), relationship.getId()).castToResource());

            }
            Collection<Resource> resources = orderedById.values();

            Data<Resource> data;
            RelationshipType relationshipType = getRelationshipType(field);
            if (relationshipType.isToOne()) {
                data = resources.isEmpty() ? new Data<>((Resource) null) : new Data<>(resources.iterator().next());
            } else {
                data = new Data<>(resources);
            }
            // TODO - links
            relationshipMap.put(field, new Relationship(null, data));
        }

        return relationshipMap;
    }

    /**
     * Get attributes mapping from entity.
     *
     * @return Mapping of attributes to objects
     */
    protected Map<String, Object> getAttributes() {
        final Map<String, Object> attributes = new LinkedHashMap<>();

        final Set<String> attrFields = filterFields(dictionary.getAttributes(obj));
        for (String field : attrFields) {
            Object val = getAttribute(field);
            attributes.put(field, val);
        }
        return attributes;
    }

    /**
     * Sets value.
     * @param fieldName the field name
     * @param newValue the new value
     */
    protected void setValueChecked(String fieldName, Object newValue) {
        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, newValue, getValueUnchecked(fieldName));
        setValue(fieldName, newValue);
    }

    /**
     * Nulls the relationship or attribute and checks update permissions.
     * Invokes the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param fieldName the field name to set or invoke equivalent set method
     * @param oldValue the old value
     */
    protected void nullValue(String fieldName, PersistentResource oldValue) {
        if (oldValue == null) {
            return;
        }
        String inverseField = getInverseRelationField(fieldName);
        if (!inverseField.isEmpty()) {
            oldValue.checkFieldAwarePermissions(UpdatePermission.class, inverseField, null, getObject());
        }
        this.setValueChecked(fieldName, null);
    }

    /**
     * Gets a value from an entity and checks read permissions.
     * @param fieldName the field name
     * @return value value
     */
    protected Object getValueChecked(String fieldName) {
        checkFieldAwareDeferPermissions(ReadPermission.class, fieldName, (Object) null, (Object) null);
        return getValue(getObject(), fieldName, dictionary);
    }

    /**
     * Retrieve an object without checking read permissions (i.e. value is used internally and not sent to others)
     *
     * @param fieldName the field name
     * @return Value
     */
    protected Object getValueUnchecked(String fieldName) {
        return getValue(getObject(), fieldName, dictionary);
    }

    /**
     * Adds a new element to a collection and tests update permission.
     * @param collection the collection
     * @param collectionName the collection name
     * @param toAdd the to add
     * @return True if added to collection false otherwise (i.e. element already in collection)
     */
    protected boolean addToCollection(Collection collection, String collectionName, PersistentResource toAdd) {
        final Collection singleton = Collections.singleton(toAdd.getObject());
        final Collection original = copyCollection(collection);
        checkFieldAwareDeferPermissions(
                UpdatePermission.class,
                collectionName,
                CollectionUtils.union(CollectionUtils.emptyIfNull(collection), singleton),
                original);
        if (collection == null) {
            collection = Collections.singleton(toAdd.getObject());
            Object value = getValueUnchecked(collectionName);
            if ((value == null && toAdd.getObject() != null) || (value != null && !value.equals(toAdd.getObject()))) {
                this.setValueChecked(collectionName, collection);
                return true;
            }
        } else {
            if (!collection.contains(toAdd.getObject())) {
                collection.add(toAdd.getObject());
                auditField(new ChangeSpec(this, collectionName, original, collection));
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes an existing element in a collection and tests update and delete permissions.
     * @param collection the collection
     * @param collectionName the collection name
     * @param toDelete the to delete
     */
    protected void delFromCollection(Collection collection, String collectionName, PersistentResource toDelete) {
        final Collection original = copyCollection(collection);
        checkFieldAwareDeferPermissions(
                UpdatePermission.class,
                collectionName,
                CollectionUtils.disjunction(collection, Collections.singleton(toDelete.getObject())),
                original
        );

        String inverseField = getInverseRelationField(collectionName);
        if (!inverseField.isEmpty()) {
            // Compute the ChangeSpec for the inverse relation and check whether or not we have access
            // to apply this change to that field.
            final Object originalValue = toDelete.getValueUnchecked(inverseField);
            final Collection originalBidirectional;

            if (originalValue instanceof Collection) {
                originalBidirectional = copyCollection((Collection) originalValue);
            } else {
                originalBidirectional = Collections.singleton(originalValue);
            }

            final Collection removedBidrectional = CollectionUtils
                    .disjunction(Collections.singleton(this.getObject()), originalBidirectional);

            toDelete.checkFieldAwareDeferPermissions(
                    UpdatePermission.class,
                    inverseField,
                    removedBidrectional,
                    originalBidirectional
            );
        }

        if (collection == null) {
            return;
        }

        collection.remove(toDelete.getObject());
        auditField(new ChangeSpec(this, collectionName, original, collection));
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value the value to set
     */
    protected void setValue(String fieldName, Object value) {
        Class<?> targetClass = obj.getClass();
        final Object original = getValueUnchecked(fieldName);
        try {
            Class<?> fieldClass = dictionary.getType(targetClass, fieldName);
            String realName = dictionary.getNameFromAlias(obj, fieldName);
            fieldName = (realName != null) ? realName : fieldName;
            String setMethod = "set" + WordUtils.capitalize(fieldName);
            Method method = EntityDictionary.findMethod(targetClass, setMethod, fieldClass);
            method.invoke(obj, coerce(value, fieldName, fieldClass));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidAttributeException(fieldName, type);
        } catch (IllegalArgumentException | NoSuchMethodException noMethod) {
            try {
                Field field = targetClass.getField(fieldName);
                field.set(obj, coerce(value, fieldName, field.getType()));
            } catch (NoSuchFieldException | IllegalAccessException noField) {
                throw new InvalidAttributeException(fieldName, type);
            }
        }

        runTriggers(OnUpdate.class, fieldName);
        this.requestScope.queueCommitTrigger(this, fieldName);
        auditField(new ChangeSpec(this, fieldName, original, value));
    }

    <A extends Annotation> void runTriggers(Class<A> annotationClass) {
        runTriggers(annotationClass, "");
    }

    <A extends Annotation> void runTriggers(Class<A> annotationClass, String fieldName) {
        Class<?> targetClass = obj.getClass();

        Collection<Method> methods = dictionary.getTriggers(targetClass, annotationClass, fieldName);
        for (Method method : methods) {
            try {
                method.invoke(obj);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Coerce provided value into expected class type.
     *
     * @param value provided value
     * @param fieldClass expected class type
     * @return coerced value
     */
    private Object coerce(Object value, String fieldName, Class<?> fieldClass) {
        if (fieldClass != null && Collection.class.isAssignableFrom(fieldClass) && value instanceof Collection) {
            return coerceCollection((Collection) value, fieldName, fieldClass);
        }

        if (fieldClass != null && Map.class.isAssignableFrom(fieldClass) && value instanceof Map) {
            return coerceMap((Map<?, ?>) value, fieldName, fieldClass);
        }

        return CoerceUtil.coerce(value, fieldClass);
    }

    private Collection coerceCollection(Collection<?> values, String fieldName, Class<?> fieldClass) {
        Class<?> providedType = dictionary.getParameterizedType(obj, fieldName);

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

    private Map coerceMap(Map<?, ?> values, String fieldName, Class<?> fieldClass) {
        Class<?> keyType = dictionary.getParameterizedType(obj, fieldName, 0);
        Class<?> valueType = dictionary.getParameterizedType(obj, fieldName, 1);

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
     * Invoke the get[fieldName] method on the target object OR get the field with the corresponding name.
     * @param target the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @param dictionary the dictionary
     * @return the value
     */
    public static Object getValue(Object target, String fieldName, EntityDictionary dictionary) {
        AccessibleObject accessor = dictionary.getAccessibleObject(target, fieldName);
        try {
            if (accessor instanceof Method) {
                return ((Method) accessor).invoke(target);
            } else if (accessor instanceof Field) {
                return ((Field) accessor).get(target);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidAttributeException(fieldName, dictionary.getJsonAliasFor(target.getClass()));
        }
        throw new InvalidAttributeException(fieldName, dictionary.getJsonAliasFor(target.getClass()));
    }

    /**
     * If a bidirectional relationship exists, attempts to delete itself from the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param inverseEntity The value (B) which has been deleted from this object.
     */
    protected void deleteInverseRelation(String relationName, Object inverseEntity) {
        String inverseRelationName = getInverseRelationField(relationName);

        if (!inverseRelationName.equals("")) {
            Class<?> inverseRelationType = dictionary.getType(inverseEntity.getClass(), inverseRelationName);

            PersistentResource inverseResource = new PersistentResource(this, inverseEntity, getRequestScope());
            Object inverseRelation = inverseResource.getValueUnchecked(inverseRelationName);

            if (inverseRelation == null) {
                return;
            }

            if (inverseRelation instanceof Collection) {
                inverseResource.delFromCollection((Collection) inverseRelation, inverseRelationName, this);
            } else if (inverseRelationType.equals(this.getResourceClass())) {
                inverseResource.nullValue(inverseRelationName, this);
            } else {
                throw new InternalServerErrorException("Relationship type mismatch");
            }
            inverseResource.markDirty();
        }
    }

    private boolean hasInverseRelation(String relationName) {
        String inverseField = getInverseRelationField(relationName);
        return inverseField != null && !inverseField.isEmpty();
    }

    private String getInverseRelationField(String relationName) {
        return dictionary.getRelationInverse(obj.getClass(), relationName);
    }

    /**
     * If a bidirectional relationship exists, attempts to add itself to the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param relationValue The value (B) which has been added to this object.
     */
    protected void addInverseRelation(String relationName, Object relationValue) {
        Object inverseEntity = relationValue; // Assigned to improve readability.
        String inverseRelationName = dictionary.getRelationInverse(obj.getClass(), relationName);

        if (!inverseRelationName.equals("")) {
            Class<?> inverseRelationType = dictionary.getType(inverseEntity.getClass(), inverseRelationName);

            PersistentResource inverseResource = new PersistentResource(this, inverseEntity, getRequestScope());
            Object inverseRelation = inverseResource.getValueUnchecked(inverseRelationName);

            if (Collection.class.isAssignableFrom(inverseRelationType)) {
                if (inverseRelation != null) {
                    inverseResource.addToCollection((Collection) inverseRelation, inverseRelationName, this);
                } else {
                    inverseResource.setValueChecked(inverseRelationName, Collections.singleton(this.getObject()));
                }
            } else if (inverseRelationType.equals(this.getResourceClass())) {
                inverseResource.setValueChecked(inverseRelationName, this.getObject());
            } else {
                throw new InternalServerErrorException("Relationship type mismatch");
            }
            inverseResource.markDirty();
        }
    }

    /**
     * Filter a set of PersistentResources.
     *
     * @param <A> the type parameter
     * @param <T> the type parameter
     * @param permission the permission
     * @param resources  the resources
     * @return Filtered set of resources
     */
    protected static <A extends Annotation, T> Set<PersistentResource<T>> filter(Class<A> permission,
                                                                                 Set<PersistentResource<T>> resources) {
        return filter(permission, resources, false);
    }

    /**
     * Filter a set of PersistentResources.
     *
     * @param <A> the type parameter
     * @param <T> the type parameter
     * @param permission the permission
     * @param resources  the resources
     * @param skipNew
     * @return Filtered set of resources
     */
    protected static <A extends Annotation, T> Set<PersistentResource<T>> filter(Class<A> permission,
                                                                                 Set<PersistentResource<T>> resources,
                                                                                 boolean skipNew) {
        Set<PersistentResource<T>> filteredSet = new LinkedHashSet<>();
        for (PersistentResource<T> resource : resources) {
            try {
                if (!(skipNew && resource.getRequestScope().getNewResources().contains(resource))) {
                    resource.checkFieldAwarePermissions(permission);
                }
                filteredSet.add(resource);
            } catch (ForbiddenAccessException e) {
                // Do nothing. Filter from set.
            }
        }
        // keep original SingleElementSet
        if (resources instanceof SingleElementSet && resources.equals(filteredSet)) {
            return resources;
        }
        return filteredSet;
    }

    /**
     * Filter a set of fields.
     *
     * @param fields the fields
     * @return Filtered set of fields
     */
    protected Set<String> filterFields(Collection<String> fields) {
        Set<String> filteredSet = new LinkedHashSet<>();
        for (String field : fields) {
            try {
                if (checkIncludeSparseField(requestScope.getSparseFields(), type, field)) {
                    checkFieldAwarePermissions(ReadPermission.class, field, (Object) null, (Object) null);
                    filteredSet.add(field);
                }
            } catch (ForbiddenAccessException e) {
                // Do nothing. Filter from set.
            }
        }
        return filteredSet;
    }

    private static <A extends Annotation> void checkPermission(Class<A> annotationClass, PersistentResource resource) {
        resource.requestScope.getPermissionExecutor().checkPermission(annotationClass, resource);
    }

    private <A extends Annotation> void checkFieldAwarePermissions(Class<A> annotationClass) {
        requestScope.getPermissionExecutor().checkPermission(annotationClass, this);
    }

    private <A extends Annotation> void checkFieldAwarePermissions(Class<A> annotationClass,
                                                                   String fieldName,
                                                                   Object modified,
                                                                   Object original) {
        ChangeSpec changeSpec = (UpdatePermission.class.isAssignableFrom(annotationClass))
                ? new ChangeSpec(this, fieldName, original, modified)
                : null;

        requestScope.getPermissionExecutor()
                .checkSpecificFieldPermissions(this, changeSpec, annotationClass, fieldName);
    }

    private <A extends Annotation> void checkFieldAwareDeferPermissions(Class<A> annotationClass,
                                                                        String fieldName,
                                                                        Object modified,
                                                                        Object original) {
        ChangeSpec changeSpec = (UpdatePermission.class.isAssignableFrom(annotationClass))
                ? new ChangeSpec(this, fieldName, original, modified)
                : null;
        // Defer checks for newly created objects if:
        //   1. This is a patch extension request
        //   2. This is an update request (note: changeSpec != null is a faster change check than rechecking permission)
        if (requestScope.getNewResources().contains(this)
                && ((requestScope instanceof PatchRequestScope)
                || changeSpec != null)) {
            requestScope
                    .getPermissionExecutor()
                    .checkSpecificFieldPermissionsDeferred(this, changeSpec, annotationClass, fieldName);
            return;
        }
        requestScope
                .getPermissionExecutor()
                .checkSpecificFieldPermissions(this, changeSpec, annotationClass, fieldName);
    }

    protected static boolean checkIncludeSparseField(Map<String, Set<String>> sparseFields, String type,
                                                     String fieldName) {
        if (!sparseFields.isEmpty()) {
            if (!sparseFields.containsKey(type)) {
                return false;
            }

            if (!sparseFields.get(type).contains(fieldName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Audit an action on field.
     *
     * @param changeSpec Change spec for audit
     */
    protected void auditField(final ChangeSpec changeSpec) {
        final String fieldName = changeSpec.getFieldName();
        Audit[] annotations = dictionary.getAttributeOrRelationAnnotations(getResourceClass(),
                Audit.class,
                fieldName
        );

        if (annotations == null || annotations.length == 0) {
            // Default to class-level annotation for action
            auditClass(Audit.Action.UPDATE, changeSpec);
            return;
        }
        for (Audit annotation : annotations) {
            if (annotation.action().length == 1 && annotation.action()[0] == Audit.Action.UPDATE) {
                LogMessage message = new LogMessage(annotation, this, Optional.of(changeSpec));
                getRequestScope().getAuditLogger().log(message);
            } else {
                throw new InvalidSyntaxException("Only Audit.Action.UPDATE is allowed on fields.");
            }
        }
    }

    /**
     * Audit an action on an entity.
     *
     * @param action the action
     */
    protected void auditClass(Audit.Action action, ChangeSpec changeSpec) {
        Audit[] annotations = getResourceClass().getAnnotationsByType(Audit.class);

        if (annotations == null) {
            return;
        }
        for (Audit annotation : annotations) {
            for (Audit.Action auditAction : annotation.action()) {
                if (auditAction == action) {
                    LogMessage message = new LogMessage(annotation, this, Optional.ofNullable(changeSpec));
                    getRequestScope().getAuditLogger().log(message);
                }
            }
        }
    }

    /**
     * Shallow copy a collection.
     *
     * @param collection Collection to copy
     * @return New copy of collection
     */
    private Collection copyCollection(final Collection collection) {
        final ArrayList newCollection = new ArrayList();
        if (collection == null || collection.isEmpty()) {
            return newCollection;
        }
        collection.iterator().forEachRemaining(newCollection::add);
        return newCollection;
    }

    /**
     * Mark this object as dirty.
     */
    private void markDirty() {
        requestScope.getDirtyResources().add(this);
    }
}
