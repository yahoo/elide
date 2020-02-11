/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
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
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.InPredicate;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.parsers.expression.CanPaginateVisitor;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.permissions.ExpressionResult;
import com.yahoo.elide.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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
public class PersistentResource<T> implements com.yahoo.elide.security.PersistentResource<T> {
    protected T obj;
    private final String type;
    private final ResourceLineage lineage;
    private final Optional<String> uuid;
    private final DataStoreTransaction transaction;
    private final RequestScope requestScope;
    private int hashCode = 0;
    static final String CLASS_NO_FIELD = "";

    /**
     * The Dictionary.
     */
    protected final EntityDictionary dictionary;

    /* Sort strings first by length then contents */
    private final Comparator<String> lengthFirstComparator = (string1, string2) -> {
        int diff = string1.length() - string2.length();
        return diff == 0 ? string1.compareTo(string2) : diff;
    };

    @Override
    public String toString() {
        return String.format("PersistentResource{type=%s, id=%s}", type, uuid.orElse(getId()));
    }

   /**
     * Create a resource in the database.
     * @param parent - The immediate ancestor in the lineage or null if this is a root.
     * @param entityClass the entity class
     * @param requestScope the request scope
     * @param uuid the (optional) uuid
     * @param <T> object type
     * @return persistent resource
     */
    public static <T> PersistentResource<T> createObject(
            PersistentResource<?> parent,
            Class<T> entityClass,
            RequestScope requestScope,
            Optional<String> uuid) {

        //instead of calling transcation.createObject, create the new object here.
        T obj = requestScope.getTransaction().createNewObject(entityClass);

        String id = uuid.orElse(null);

        PersistentResource<T> newResource = new PersistentResource<>(obj, parent, id, requestScope);

        //The ID must be assigned before we add it to the new resources set.  Persistent resource
        //hashcode and equals are only based on the ID/UUID & type.
        assignId(newResource, id);

        // Keep track of new resources for non shareable resources
        requestScope.getNewPersistentResources().add(newResource);
        checkPermission(CreatePermission.class, newResource);

        newResource.auditClass(Audit.Action.CREATE, new ChangeSpec(newResource, null, null, newResource.getObject()));

        requestScope.publishLifecycleEvent(newResource, CRUDEvent.CRUDAction.CREATE);

        String type = newResource.getType();
        requestScope.setUUIDForObject(type, id, newResource.getObject());

        // Initialize null ToMany collections
        requestScope.getDictionary().getRelationships(entityClass).stream()
                .filter(relationName -> newResource.getRelationshipType(relationName).isToMany()
                        && newResource.getValueUnchecked(relationName) == null)
                .forEach(relationName -> newResource.setValue(relationName, new LinkedHashSet<>()));

        newResource.markDirty();
        return newResource;
    }

    /**
     * Construct a new resource from the ID provided.
     *
     * @param obj the obj
     * @param parent the parent
     * @param id the id
     * @param scope the request scope
     */
    public PersistentResource(@NonNull T obj, PersistentResource parent, String id, @NonNull RequestScope scope) {
        this.obj = obj;
        this.uuid = Optional.ofNullable(id);
        this.lineage = parent != null ? new ResourceLineage(parent.lineage, parent) : new ResourceLineage();
        this.dictionary = scope.getDictionary();
        this.type = dictionary.getJsonAliasFor(obj.getClass());
        this.transaction = scope.getTransaction();
        this.requestScope = scope;
        dictionary.initializeEntity(obj);
    }

    /**
     * Check whether an id matches for this persistent resource.
     *
     * @param checkId the check id
     * @return True if matches false otherwise
     */
    @Override
    public boolean matchesId(String checkId) {
        if (checkId == null) {
            return false;
        }
        return uuid
                .map(checkId::equals)
                .orElseGet(() -> {
                    String id = getId();
                    return !"0".equals(id) && !"null".equals(id) && checkId.equals(id);
                });
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

        // Check the resource cache if exists
        Object obj = requestScope.getObjectById(dictionary.getJsonAliasFor(loadClass), id);
        if (obj == null) {
            // try to load object
            Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(loadClass,
                    requestScope);
            Class<?> idType = dictionary.getIdType(loadClass);
            obj = tx.loadObject(loadClass, (Serializable) CoerceUtil.coerce(id, idType),
                    permissionFilter, requestScope);
            if (obj == null) {
                throw new InvalidObjectIdentifierException(id, dictionary.getJsonAliasFor(loadClass));
            }
        }

        PersistentResource<T> resource = new PersistentResource<>(
                loadClass.cast(obj), null, requestScope.getUUIDFor(obj), requestScope);
        // No need to have read access for a newly created object
        if (!requestScope.getNewResources().contains(resource)) {
            resource.checkFieldAwarePermissions(ReadPermission.class);
        }

        return resource;
    }

    /**
     * Get a FilterExpression parsed from FilterExpressionCheck.
     *
     * @param <T> the type parameter
     * @param loadClass the load class
     * @param requestScope the request scope
     * @return a FilterExpression defined by FilterExpressionCheck.
     */
    private static <T> Optional<FilterExpression> getPermissionFilterExpression(Class<T> loadClass,
                                                                      RequestScope requestScope) {
        try {
            return requestScope.getPermissionExecutor().getReadPermissionFilter(loadClass);
        } catch (ForbiddenAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Load a collection from the datastore.
     *
     * @param loadClass the load class
     * @param requestScope the request scope
     * @param ids a list of object identifiers to optionally load.  Can be empty.
     * @return a filtered collection of resources loaded from the datastore.
     */
    public static Set<PersistentResource> loadRecords(
            Class<?> loadClass,
            List<String> ids,
            Optional<FilterExpression> filter,
            Optional<Sorting> sorting,
            Optional<Pagination> pagination,
            RequestScope requestScope) {

        EntityDictionary dictionary = requestScope.getDictionary();
        FilterExpression filterExpression;

        DataStoreTransaction tx = requestScope.getTransaction();


        if (shouldSkipCollection(loadClass, ReadPermission.class, requestScope)) {
            if (ids.isEmpty()) {
                return Collections.emptySet();
            }
            throw new InvalidObjectIdentifierException(ids.toString(), dictionary.getJsonAliasFor(loadClass));
        }


        if (pagination.isPresent() && !pagination.get().isDefaultInstance()
                && !CanPaginateVisitor.canPaginate(loadClass, dictionary, requestScope)) {
            throw new InvalidPredicateException(String.format("Cannot paginate %s",
                    dictionary.getJsonAliasFor(loadClass)));
        }

        Set<PersistentResource> newResources = new LinkedHashSet<>();

        if (!ids.isEmpty()) {
            String typeAlias = dictionary.getJsonAliasFor(loadClass);
            newResources = requestScope.getNewPersistentResources().stream()
                        .filter(resource -> typeAlias.equals(resource.getType())
                                && ids.contains(resource.getUUID().orElse("")))
                        .collect(Collectors.toSet());
            FilterExpression idExpression = buildIdFilterExpression(ids, loadClass, dictionary, requestScope);

            // Combine filters if necessary
            filterExpression = filter
                    .map(fe -> (FilterExpression) new AndFilterExpression(idExpression, fe))
                    .orElse(idExpression);
        } else {
            filterExpression = filter.orElse(null);
        }

        Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(loadClass, requestScope);
        if (permissionFilter.isPresent()) {
            if (filterExpression != null) {
                filterExpression = new AndFilterExpression(filterExpression, permissionFilter.get());
            } else {
                filterExpression = permissionFilter.get();
            }
        }

        Set<PersistentResource> existingResources = filter(ReadPermission.class,
                new PersistentResourceSet(tx.loadObjects(loadClass, Optional.ofNullable(filterExpression), sorting,
                pagination.map(p -> p.evaluate(loadClass)), requestScope), requestScope));

        Set<PersistentResource> allResources = Sets.union(newResources, existingResources);

        Set<String> allExpectedIds = allResources.stream()
                .map(resource -> (String) resource.getUUID().orElseGet(resource::getId))
                .collect(Collectors.toSet());
        Set<String> missedIds = Sets.difference(new HashSet<>(ids), allExpectedIds);

        if (!missedIds.isEmpty()) {
            throw new InvalidObjectIdentifierException(missedIds.toString(), dictionary.getJsonAliasFor(loadClass));
        }

        return allResources;
    }

    /**
     * Update attribute in existing resource.
     *
     * @param fieldName the field name
     * @param newVal the new val
     * @return true if object updated, false otherwise
     */
    public boolean updateAttribute(String fieldName, Object newVal) {
        Class<?> fieldClass = dictionary.getType(getResourceClass(), fieldName);
        newVal =  dictionary.coerce(obj, newVal, fieldName, fieldClass);
        Object val = getValueUnchecked(fieldName);
        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, newVal, val);
        if (!Objects.equals(val, newVal)) {
            this.setValueChecked(fieldName, newVal);
            this.markDirty();
            //Hooks for customize logic for setAttribute/Relation
            if (dictionary.isAttribute(obj.getClass(), fieldName)) {
                transaction.setAttribute(obj, fieldName, newVal, requestScope);
            }
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
        Set<PersistentResource> resources = filter(ReadPermission.class,
                getRelationUncheckedUnfiltered(fieldName));
        boolean isUpdated;
        if (type.isToMany()) {
            List<Object> modifiedResources = CollectionUtils.isEmpty(resourceIdentifiers)
                    ? Collections.emptyList()
                    : resourceIdentifiers.stream().map(PersistentResource::getObject).collect(Collectors.toList());
            checkFieldAwareDeferPermissions(
                    UpdatePermission.class,
                    fieldName,
                    modifiedResources,
                    resources.stream().map(PersistentResource::getObject).collect(Collectors.toList())
            );
            isUpdated = updateToManyRelation(fieldName, resourceIdentifiers, resources);
        } else { // To One Relationship
            PersistentResource resource = firstOrNullIfEmpty(resources);
            Object original = (resource == null) ? null : resource.getObject();
            PersistentResource modifiedResource = firstOrNullIfEmpty(resourceIdentifiers);
            Object modified = (modifiedResource == null) ? null : modifiedResource.getObject();
            checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, modified, original);
            isUpdated = updateToOneRelation(fieldName, resourceIdentifiers, resources);
        }
        return isUpdated;
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

        Set<Object> newRelationships = new LinkedHashSet<>();
        Set<Object> deletedRelationships = new LinkedHashSet<>();

        deleted
                .stream()
                .forEach(toDelete -> {
                    delFromCollection(collection, fieldName, toDelete, false);
                    deleteInverseRelation(fieldName, toDelete.getObject());
                    deletedRelationships.add(toDelete.getObject());
                });

        added
                .stream()
                .forEach(toAdd -> {
                    addToCollection(collection, fieldName, toAdd);
                    addInverseRelation(fieldName, toAdd.getObject());
                    newRelationships.add(toAdd.getObject());
                });

        if (!updated.isEmpty()) {
            this.markDirty();
        }

        //hook for updateRelation
        transaction.updateToManyRelation(transaction, obj, fieldName,
                newRelationships, deletedRelationships, requestScope);

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
        if (CollectionUtils.isNotEmpty(resourceIdentifiers)) {
            newResource = IterableUtils.first(resourceIdentifiers);
            newValue = newResource.getObject();
        }

        PersistentResource oldResource = firstOrNullIfEmpty(mine);

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

        if (newResource != null) {
            if (hasInverseRelation(fieldName)) {
                addInverseRelation(fieldName, newValue);
                newResource.markDirty();
            }
        }

        this.setValueChecked(fieldName, newValue);
        //hook for updateToOneRelation
        transaction.updateToOneRelation(transaction, obj, fieldName, newValue, requestScope);

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
        Set<PersistentResource> mine = filter(ReadPermission.class, getRelationUncheckedUnfiltered(relationName));
        checkFieldAwareDeferPermissions(UpdatePermission.class, relationName, Collections.emptySet(),
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
            PersistentResource oldValue = IterableUtils.first(mine);
            if (oldValue != null && oldValue.getObject() != null) {
                this.nullValue(relationName, oldValue);
                oldValue.markDirty();
                this.markDirty();
                //hook for updateToOneRelation
                transaction.updateToOneRelation(transaction, obj, relationName, null, requestScope);

            }
        } else {
            Collection collection = (Collection) getValueUnchecked(relationName);
            if (CollectionUtils.isNotEmpty(collection)) {
                Set<Object> deletedRelationships = new LinkedHashSet<>();
                mine.stream()
                        .forEach(toDelete -> {
                            delFromCollection(collection, relationName, toDelete, false);
                            if (hasInverseRelation(relationName)) {
                                toDelete.markDirty();
                            }
                            deletedRelationships.add(toDelete.getObject());
                        });
                this.markDirty();
                //hook for updateToManyRelation
                transaction.updateToManyRelation(transaction, obj, relationName,
                        new LinkedHashSet<>(), deletedRelationships, requestScope);

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

        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, modified, original);

        if (relation instanceof Collection) {
            if (removeResource == null || !((Collection) relation).contains(removeResource.getObject())) {

                //Nothing to do
                return;
            }
            delFromCollection((Collection) relation, fieldName, removeResource, false);
        } else {
            if (relation == null || removeResource == null || !relation.equals(removeResource.getObject())) {
                //Nothing to do
                return;
            }
            this.nullValue(fieldName, removeResource);
        }

        if (hasInverseRelation(fieldName)) {
            deleteInverseRelation(fieldName, removeResource.getObject());
            removeResource.markDirty();
        }

        if (!Objects.equals(original, modified)) {
            this.markDirty();
        }

        RelationshipType type = getRelationshipType(fieldName);
        if (type.isToOne()) {
            //hook for updateToOneRelation
            transaction.updateToOneRelation(transaction, obj, fieldName, null, requestScope);
        } else {
            //hook for updateToManyRelation
            transaction.updateToManyRelation(transaction, obj, fieldName,
                    new LinkedHashSet<>(), Sets.newHashSet(removeResource.getObject()), requestScope);
        }
    }

    /**
     * Checks whether the new entity is already a part of the relationship
     * @param fieldName which relation link
     * @param toAdd the new relation
     * @return boolean representing the result of the check
     *
     * This method does not handle the case where the toAdd entity is newly created,
     * since newly created entities have null id there is no easy way to check for identity
     */
    public boolean relationshipAlreadyExists(String fieldName, PersistentResource toAdd) {
        Object relation = this.getValueUnchecked(fieldName);
        String toAddId = toAdd.getId();
        if (toAddId == null) {
            return false;
        }
        if (relation instanceof Collection) {
            return ((Collection) relation).stream().anyMatch((obj -> toAddId.equals(dictionary.getId(obj))));
        }
        return toAddId.equals(dictionary.getId(relation));
    }

    /**
     * Add relation link from a given parent resource to a child resource.
     *
     * @param fieldName which relation link
     * @param newRelation the new relation
     */
    public void addRelation(String fieldName, PersistentResource newRelation) {
        if (!newRelation.isNewlyCreated() && relationshipAlreadyExists(fieldName, newRelation)) {
            return;
        }
        checkSharePermission(Collections.singleton(newRelation));
        Object relation = this.getValueUnchecked(fieldName);

        if (relation instanceof Collection) {
            if (addToCollection((Collection) relation, fieldName, newRelation)) {
                this.markDirty();
            }
            //Hook for updateToManyRelation
            transaction.updateToManyRelation(transaction, obj, fieldName,
                    Sets.newHashSet(newRelation.getObject()), new LinkedHashSet<>(), requestScope);

            addInverseRelation(fieldName, newRelation.getObject());
        } else {
            // Not a collection, but may be trying to create a ToOne relationship.
            // NOTE: updateRelation marks dirty.
            updateRelation(fieldName, Collections.singleton(newRelation));
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

            /* Skip updating inverse relationships for deletes which are cascaded */
            if (dictionary.cascadeDeletes(getResourceClass(), relationName)) {
                continue;
            }
            String inverseRelationName = dictionary.getRelationInverse(getResourceClass(), relationName);
            if (!"".equals(inverseRelationName)) {
                for (PersistentResource inverseResource : getRelationCheckedUnfiltered(relationName)) {
                    if (hasInverseRelation(relationName)) {
                        deleteInverseRelation(relationName, inverseResource.getObject());
                        inverseResource.markDirty();
                    }
                }
            }
        }

        transaction.delete(getObject(), requestScope);
        auditClass(Audit.Action.DELETE, new ChangeSpec(this, null, getObject(), null));
        requestScope.publishLifecycleEvent(this, CRUDEvent.CRUDAction.DELETE);
        requestScope.getDeletedResources().add(this);
    }

    /**
     * Get resource ID.
     *
     * @return ID id
     */
    @Override
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
        return dictionary.getEntityBinding(
            getObject().getClass()
        ).isIdGenerated();
    }


    /**
     * Gets UUID.
     * @return the UUID
     */
    @Override
    public Optional<String> getUUID() {
        return uuid;
    }

    /**
     * Get relation looking for a _single_ id.
     *
     * NOTE: Filter expressions for this type are _not_ applied at this level.
     *
     * @param relation relation name
     * @param id single id to lookup
     * @return The PersistentResource of the sought id or null if does not exist.
     */
    public PersistentResource getRelation(String relation, String id) {
        Set<PersistentResource> resources = getRelation(relation, Collections.singletonList(id),
                Optional.empty(), Optional.empty(), Optional.empty());
      if (resources.isEmpty()) {
            return null;
      }
      // If this is an in-memory object (i.e. UUID being created within tx), datastore may not be able to filter.
      // If we get multiple results back, make sure we find the right id first.
      for (PersistentResource resource : resources) {
          if (resource.matchesId(id)) {
              return resource;
          }
      }
      return null;
    }

    /**
     * Load a single entity relation from the PersistentResource. Example: GET /book/2
     *
     * @param relation the relation
     * @param ids a list of object identifiers to optionally load.  Can be empty.
     * @return PersistentResource relation
     */
    public Set<PersistentResource> getRelation(String relation,
                                          List<String> ids,
                                          Optional<FilterExpression> filter,
                                          Optional<Sorting> sorting,
                                          Optional<Pagination> pagination) {

        FilterExpression filterExpression;

        Class<?> entityType = dictionary.getParameterizedType(getResourceClass(), relation);

        Set<PersistentResource> newResources = new LinkedHashSet<>();

        /* If this is a bulk edit request and the ID we are fetching for is newly created... */
        if (entityType == null) {
            throw new InvalidAttributeException(relation, type);
        }
        if (!ids.isEmpty()) {
            // Fetch our set of new resources that we know about since we can't find them in the datastore
            newResources = requestScope.getNewPersistentResources().stream()
                    .filter(resource -> entityType.isAssignableFrom(resource.getResourceClass())
                            && ids.contains(resource.getUUID().orElse("")))
                    .collect(Collectors.toSet());

            FilterExpression idExpression = buildIdFilterExpression(ids, entityType, dictionary, requestScope);

            // Combine filters if necessary
            filterExpression = filter
                    .map(fe -> (FilterExpression) new AndFilterExpression(idExpression, fe))
                    .orElse(idExpression);
        } else {
            filterExpression = filter.orElse(null);
        }

        // TODO: Filter on new resources?
        // TODO: Update pagination to subtract the number of new resources created?

        Set<PersistentResource> existingResources = filter(ReadPermission.class,
            getRelation(relation, Optional.ofNullable(filterExpression), sorting, pagination, true));

        // TODO: Sort again in memory now that two sets are glommed together?

        Set<PersistentResource> allResources = Sets.union(newResources, existingResources);
        Set<String> allExpectedIds = allResources.stream()
                .map(resource -> (String) resource.getUUID().orElseGet(resource::getId))
                .collect(Collectors.toSet());
        Set<String> missedIds = Sets.difference(new HashSet<>(ids), allExpectedIds);

        if (!missedIds.isEmpty()) {
            throw new InvalidObjectIdentifierException(missedIds.toString(), relation);
        }

        return allResources;
    }

    /**
     * Build an id filter expression for a particular entity type.
     *
     * @param ids Ids to include in the filter expression
     * @param entityType Type of entity
     * @return Filter expression for given ids and type.
     */
    private static FilterExpression buildIdFilterExpression(List<String> ids,
                                                            Class<?> entityType,
                                                            EntityDictionary dictionary,
                                                            RequestScope scope) {
        Class<?> idType = dictionary.getIdType(entityType);
        String idField = dictionary.getIdFieldName(entityType);
        String typeAlias = dictionary.getJsonAliasFor(entityType);

        List<Object> coercedIds = ids.stream()
                .filter(id -> scope.getObjectById(typeAlias, id) == null) // these don't exist yet
                .map(id -> CoerceUtil.coerce(id, idType))
                .collect(Collectors.toList());

        /* construct a new SQL like filter expression, eg: book.id IN [1,2] */
        FilterExpression idFilter = new InPredicate(
                new Path.PathElement(
                        entityType,
                        idType,
                        idField),
                coercedIds);

        return idFilter;
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @return collection relation
     */
    public Set<PersistentResource> getRelationCheckedFiltered(String relationName,
                                                              Optional<FilterExpression> filterExpression,
                                                              Optional<Sorting> sorting,
                                                              Optional<Pagination> pagination) {

        return filter(ReadPermission.class,
                getRelation(relationName, filterExpression, sorting, pagination, true));
    }

    private Set<PersistentResource> getRelationUncheckedUnfiltered(String relationName) {
        return getRelation(relationName, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    private Set<PersistentResource> getRelationCheckedUnfiltered(String relationName) {
        return getRelation(relationName, Optional.empty(), Optional.empty(), Optional.empty(), true);
    }

    private Set<PersistentResource> getRelation(String relationName,
                                                Optional<FilterExpression> filterExpression,
                                                Optional<Sorting> sorting,
                                                Optional<Pagination> pagination,
                                                boolean checked) {

        if (checked && !checkRelation(relationName)) {
            return Collections.emptySet();
        }

        final Class<?> relationClass = dictionary.getParameterizedType(obj, relationName);
        if (pagination.isPresent() && !pagination.get().isDefaultInstance()
                && !CanPaginateVisitor.canPaginate(relationClass, dictionary, requestScope)) {
            throw new InvalidPredicateException(String.format("Cannot paginate %s",
                    dictionary.getJsonAliasFor(relationClass)));
        }

        return getRelationUnchecked(relationName, filterExpression, sorting, pagination);
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

        return !shouldSkipCollection(
                dictionary.getParameterizedType(obj, relationName),
                ReadPermission.class,
                requestScope);
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @param filterExpression An optional filter expression
     * @return collection relation
     */
    protected Set<PersistentResource> getRelationChecked(String relationName,
                                                         Optional<FilterExpression> filterExpression,
                                                         Optional<Sorting> sorting,
                                                         Optional<Pagination> pagination) {
        if (!checkRelation(relationName)) {
            return Collections.emptySet();
        }
        return getRelationUnchecked(relationName, filterExpression, sorting, pagination);

    }

    /**
     * Retrieve an uncheck set of relations.
     *
     * @param relationName field
     * @param filterExpression An optional filter expression
     * @param sorting the sorting clause
     * @param pagination the pagination params
     * @return the resources in the relationship
     */
    private Set<PersistentResource> getRelationUnchecked(String relationName,
                                                         Optional<FilterExpression> filterExpression,
                                                         Optional<Sorting> sorting,
                                                         Optional<Pagination> pagination) {
        RelationshipType type = getRelationshipType(relationName);
        final Class<?> relationClass = dictionary.getParameterizedType(obj, relationName);
        if (relationClass == null) {
            throw new InvalidAttributeException(relationName, this.getType());
        }

        Optional<Pagination> computedPagination = pagination.map(p -> p.evaluate(relationClass));

        //Invoke filterExpressionCheck and then merge with filterExpression.
        Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(relationClass, requestScope);
        Optional<FilterExpression> computedFilters = filterExpression;

        if (permissionFilter.isPresent() && filterExpression.isPresent()) {
            FilterExpression mergedExpression =
                    new AndFilterExpression(filterExpression.get(), permissionFilter.get());
            computedFilters = Optional.of(mergedExpression);
        } else if (permissionFilter.isPresent()) {
            computedFilters = permissionFilter;
        }

        Object val = transaction.getRelation(transaction, obj, relationName,
                    computedFilters, sorting, computedPagination, requestScope);

        if (val == null) {
            return Collections.emptySet();
        }

        Set<PersistentResource> resources = Sets.newLinkedHashSet();
        if (val instanceof Iterable) {
            Iterable filteredVal = (Iterable) val;
            resources = new PersistentResourceSet(this, filteredVal, requestScope);
        } else if (type.isToOne()) {
            resources = new SingleElementSet<>(
                    new PersistentResource<>(val, this, requestScope.getUUIDFor(val), requestScope));
        } else {
            resources.add(new PersistentResource<>(val, this, requestScope.getUUIDFor(val), requestScope));
        }

        return resources;
    }

    /**
     * Determine whether or not to skip loading a collection.
     *
     * @param resourceClass Resource class
     * @param annotationClass Annotation class
     * @param requestScope Request scope
     * @return True if collection should be skipped (i.e. denied access), false otherwise
     */
    private static boolean shouldSkipCollection(Class<?> resourceClass, Class<? extends Annotation> annotationClass,
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
    @Override
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
    @Override
    @JsonIgnore
    public Class<T> getResourceClass() {
        return (Class) dictionary.lookupBoundClass(obj.getClass());
    }

    /**
     * Gets type.
     * @return the type
     */
    @Override
    public String getType() {
        return type;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
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
            if (uuid.isPresent() && ("0".equals(id) || "null".equals(id))) {
                hashCode = Objects.hashCode(uuid);
            } else {
                hashCode = Objects.hashCode(id);
            }
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
    @Override
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
     * @param attributeSupplier The attribute supplier
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
        return getRelationshipsWithRelationshipFunction((relationName) -> {
            Optional<FilterExpression> filterExpression = requestScope.getExpressionForRelation(this, relationName);
            return getRelationCheckedFiltered(relationName, filterExpression, Optional.empty(), Optional.empty());
        });
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationshipsWithSortingAndPagination() {
        return getRelationshipsWithRelationshipFunction((relationName) -> {
            Optional<FilterExpression> filterExpression = requestScope.getExpressionForRelation(this, relationName);
            Optional<Sorting> sorting = Optional.ofNullable(requestScope.getSorting());
            Optional<Pagination> pagination = Optional.ofNullable(requestScope.getPagination());
            return getRelationCheckedFiltered(relationName,
                    filterExpression, sorting, pagination);
        });
    }

    /**
     * Get relationship mappings.
     *
     * @param relationshipFunction a function to load the value of a relationship. Takes a string of the relationship
     * name and returns the relationship's value.
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationshipsWithRelationshipFunction(
            final Function<String, Set<PersistentResource>> relationshipFunction) {
        final Map<String, Relationship> relationshipMap = new LinkedHashMap<>();
        final Set<String> relationshipFields = filterFields(dictionary.getRelationships(obj));

        for (String field : relationshipFields) {
            TreeMap<String, Resource> orderedById = new TreeMap<>(lengthFirstComparator);
            for (PersistentResource relationship : relationshipFunction.apply(field)) {
                orderedById.put(relationship.getId(),
                        new ResourceIdentifier(relationship.getType(), relationship.getId()).castToResource());

            }
            Collection<Resource> resources = orderedById.values();

            Data<Resource> data;
            RelationshipType relationshipType = getRelationshipType(field);
            if (relationshipType.isToOne()) {
                data = new Data<>(firstOrNullIfEmpty(resources));
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
        Object existingValue = getValueUnchecked(fieldName);

        // TODO: Need to refactor this logic. For creates this is properly converted in the executor. This logic
        // should be explicitly encapsulated here, not there.
        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, newValue, existingValue);

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
            oldValue.checkFieldAwareDeferPermissions(UpdatePermission.class, inverseField, null, getObject());
        }
        this.setValueChecked(fieldName, null);
    }

    /**
     * Gets a value from an entity and checks read permissions.
     * @param fieldName the field name
     * @return value value
     */
    protected Object getValueChecked(String fieldName) {
        requestScope.publishLifecycleEvent(this, CRUDEvent.CRUDAction.READ);
        requestScope.publishLifecycleEvent(this, fieldName, CRUDEvent.CRUDAction.READ, Optional.empty());
        checkFieldAwareDeferPermissions(ReadPermission.class, fieldName, (Object) null, (Object) null);
        return getValue(getObject(), fieldName, requestScope);
    }

    /**
     * Retrieve an object without checking read permissions (i.e. value is used internally and not sent to others)
     *
     * @param fieldName the field name
     * @return Value
     */
    protected Object getValueUnchecked(String fieldName) {
        requestScope.publishLifecycleEvent(this, CRUDEvent.CRUDAction.READ);
        requestScope.publishLifecycleEvent(this, fieldName, CRUDEvent.CRUDAction.READ, Optional.empty());
        return getValue(getObject(), fieldName, requestScope);
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
            if (!Objects.equals(value, toAdd.getObject())) {
                this.setValueChecked(collectionName, collection);
                return true;
            }
        } else {
            if (!collection.contains(toAdd.getObject())) {
                collection.add(toAdd.getObject());

                triggerUpdate(collectionName, original, collection);
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
     * @param isInverseCheck Whether or not the deletion is already coming from cleaning up an inverse.
     *                       Without this parameter, we could find ourselves in a loop of checks.
     *                       TODO: This is a band-aid for a quick fix. This should certainly be refactored.
     */
    protected void delFromCollection(
            Collection collection,
            String collectionName,
            PersistentResource toDelete,
            boolean isInverseCheck) {
        final Collection original = copyCollection(collection);
        checkFieldAwareDeferPermissions(
                UpdatePermission.class,
                collectionName,
                CollectionUtils.disjunction(collection, Collections.singleton(toDelete.getObject())),
                original
        );

        String inverseField = getInverseRelationField(collectionName);
        if (!isInverseCheck && !inverseField.isEmpty()) {
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

        triggerUpdate(collectionName, original, collection);
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value the value to set
     */
    protected void setValue(String fieldName, Object value) {
        final Object original = getValueUnchecked(fieldName);
        dictionary.setValue(obj, fieldName, value);
        triggerUpdate(fieldName, original, value);
    }

    /**
     * Invoke the get[fieldName] method on the target object OR get the field with the corresponding name.
     * @param target the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @param requestScope the request scope
     * @return the value
     */
    public static Object getValue(Object target, String fieldName, RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        return dictionary.getValue(target, fieldName, requestScope);
    }

    /**
     * If a bidirectional relationship exists, attempts to delete itself from the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param inverseEntity The value (B) which has been deleted from this object.
     */
    protected void deleteInverseRelation(String relationName, Object inverseEntity) {
        String inverseField = getInverseRelationField(relationName);

        if (!"".equals(inverseField)) {
            Class<?> inverseType = dictionary.getType(inverseEntity.getClass(), inverseField);

            String uuid = requestScope.getUUIDFor(inverseEntity);
            PersistentResource inverseResource = new PersistentResource(inverseEntity, this, uuid, requestScope);
            Object inverseRelation = inverseResource.getValueUnchecked(inverseField);

            if (inverseRelation == null) {
                return;
            }

            if (inverseRelation instanceof Collection) {
                inverseResource.delFromCollection((Collection) inverseRelation, inverseField, this, true);
            } else if (inverseType.isAssignableFrom(this.getResourceClass())) {
                inverseResource.nullValue(inverseField, this);
            } else {
                throw new InternalServerErrorException("Relationship type mismatch");
            }
            inverseResource.markDirty();

            RelationshipType inverseRelationType = inverseResource.getRelationshipType(inverseField);
            if (inverseRelationType.isToOne()) {
                //hook for updateToOneRelation
                transaction.updateToOneRelation(transaction, inverseEntity, inverseField, null, requestScope);
            } else {
                //hook for updateToManyRelation
                assert (inverseRelation instanceof Collection) : inverseField + " not a collection";
                transaction.updateToManyRelation(transaction, inverseEntity, inverseField,
                        new LinkedHashSet<>(), Sets.newHashSet(obj), requestScope);
            }
        }
    }

    private boolean hasInverseRelation(String relationName) {
        String inverseField = getInverseRelationField(relationName);
        return StringUtils.isNotEmpty(inverseField);
    }

    private String getInverseRelationField(String relationName) {
        return dictionary.getRelationInverse(obj.getClass(), relationName);
    }

    /**
     * If a bidirectional relationship exists, attempts to add itself to the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param inverseObj The value (B) which has been added to this object.
     */
    protected void addInverseRelation(String relationName, Object inverseObj) {
        String inverseName = dictionary.getRelationInverse(obj.getClass(), relationName);

        if (!"".equals(inverseName)) {
            Class<?> inverseType = dictionary.getType(inverseObj.getClass(), inverseName);

            String uuid = requestScope.getUUIDFor(inverseObj);
            PersistentResource inverseResource = new PersistentResource(inverseObj, this, uuid, requestScope);
            Object inverseRelation = inverseResource.getValueUnchecked(inverseName);

            if (Collection.class.isAssignableFrom(inverseType)) {
                if (inverseRelation != null) {
                    inverseResource.addToCollection((Collection) inverseRelation, inverseName, this);
                } else {
                    inverseResource.setValueChecked(inverseName, Collections.singleton(this.getObject()));
                }
            } else if (inverseType.isAssignableFrom(this.getResourceClass())) {
                inverseResource.setValueChecked(inverseName, this.getObject());
            } else {
                throw new InternalServerErrorException("Relationship type mismatch");
            }
            inverseResource.markDirty();

            RelationshipType inverseRelationType = inverseResource.getRelationshipType(inverseName);
            if (inverseRelationType.isToOne()) {
                //hook for updateToOneRelation
                transaction.updateToOneRelation(transaction, inverseObj, inverseName,
                        obj, requestScope);
            } else {
                //hook for updateToManyRelation
                assert (inverseRelation == null || inverseRelation instanceof Collection)
                        : inverseName + " not a collection";
                transaction.updateToManyRelation(transaction, inverseObj, inverseName,
                        Sets.newHashSet(obj), new LinkedHashSet<>(), requestScope);
            }
        }
    }

    /**
     * Filter a set of PersistentResources.
     *
     * @param permission the permission
     * @param resources  the resources
     * @return Filtered set of resources
     */
    protected static Set<PersistentResource> filter(Class<? extends Annotation> permission,
                                                    Set<PersistentResource> resources) {
        Set<PersistentResource> filteredSet = new LinkedHashSet<>();
        for (PersistentResource resource : resources) {
            try {
                // NOTE: This is for avoiding filtering on _newly created_ objects within this transaction.
                // Namely-- in a JSONPATCH request or GraphQL request-- we need to read all newly created
                // resources /regardless/ of whether or not we actually have permission to do so; this is to
                // retrieve the object id to return to the caller. If no fields on the object are readable by the caller
                // then they will be filtered out and only the id is returned. Similarly, all future requests to this
                // object will behave as expected.
                if (!resource.getRequestScope().getNewResources().contains(resource)) {
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
                    checkFieldAwareReadPermissions(field);
                    filteredSet.add(field);
                }
            } catch (ForbiddenAccessException e) {
                // Do nothing. Filter from set.
            }
        }
        return filteredSet;
    }

    /**
     * Queue the @*Update triggers iff this is not a newly created object (otherwise we run @*Create)
     */
    private void triggerUpdate(String fieldName, Object original, Object value) {
        ChangeSpec changeSpec = new ChangeSpec(this, fieldName, original, value);
        CRUDEvent.CRUDAction action = isNewlyCreated()
                ? CRUDEvent.CRUDAction.CREATE
                : CRUDEvent.CRUDAction.UPDATE;

        requestScope.publishLifecycleEvent(this, fieldName, action, Optional.of(changeSpec));
        requestScope.publishLifecycleEvent(this, action);
        auditField(new ChangeSpec(this, fieldName, original, value));
    }

    private static <A extends Annotation> ExpressionResult checkPermission(
            Class<A> annotationClass, PersistentResource resource) {
        return resource.requestScope.getPermissionExecutor().checkPermission(annotationClass, resource);
    }

    private <A extends Annotation> ExpressionResult checkFieldAwarePermissions(Class<A> annotationClass) {
        return requestScope.getPermissionExecutor().checkPermission(annotationClass, this);
    }

    private <A extends Annotation> ExpressionResult checkFieldAwareReadPermissions(String fieldName) {
        return requestScope.getPermissionExecutor()
                .checkSpecificFieldPermissions(this, null, ReadPermission.class, fieldName);
    }

    private <A extends Annotation> ExpressionResult checkFieldAwareDeferPermissions(Class<A> annotationClass,
                                                                        String fieldName,
                                                                        Object modified,
                                                                        Object original) {
        ChangeSpec changeSpec = (UpdatePermission.class.isAssignableFrom(annotationClass))
                ? new ChangeSpec(this, fieldName, original, modified)
                : null;

        return requestScope
                .getPermissionExecutor()
                .checkSpecificFieldPermissionsDeferred(this, changeSpec, annotationClass, fieldName);
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
     * @param changeSpec the change that occurred
     */
    protected void auditClass(Audit.Action action, ChangeSpec changeSpec) {
        Audit[] annotations = getResourceClass().getAnnotationsByType(Audit.class);

        if (annotations == null) {
            return;
        }
        for (Audit annotation : annotations) {
            for (Audit.Action auditAction : annotation.action()) {
                if (auditAction == action) { // compare object reference
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
        if (CollectionUtils.isEmpty(collection)) {
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

    /**
     * Assign provided id if id field is not generated.
     *
     * @param persistentResource resource
     * @param id resource id
     */
    private static void assignId(PersistentResource persistentResource, String id) {

       //If id field is not a `@GeneratedValue` or mapped via a `@MapsId` attribute
       //then persist the provided id
       if (!persistentResource.isIdGenerated()) {
            if (StringUtils.isNotEmpty(id)) {
                persistentResource.setId(id);
            } else {
                //If expecting id to persist and id is not present, throw exception
                throw new InvalidValueException(
                        persistentResource.toResource(),
                        "No id provided, cannot persist " + persistentResource.getObject()
                );
            }
        }
    }

    private static <T> T firstOrNullIfEmpty(final Collection<T> coll) {
        return CollectionUtils.isEmpty(coll) ? null : IterableUtils.first(coll);
    }
}
