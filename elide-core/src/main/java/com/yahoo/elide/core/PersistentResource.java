/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
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
import com.yahoo.elide.core.filter.Predicate;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.yahoo.elide.jsonapi.models.SingleElementSet;
import com.yahoo.elide.security.Check;
import com.yahoo.elide.security.User;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.text.WordUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
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

import javax.persistence.GeneratedValue;
import static com.yahoo.elide.security.UserCheck.DENY;

/**
 * Resource wrapper around Entity bean.
 *
 * @param <T> type of resource
 */
@ToString
public class PersistentResource<T> {
    private final String type;
    protected T obj;
    private final ResourceLineage lineage;
    private final Optional<String> uuid;
    private final User user;
    private final ObjectEntityCache entityCache;
    private final DataStoreTransaction transaction;
    @NonNull private final RequestScope requestScope;
    private final Optional<PersistentResource<?>> parent;

    /**
     * The Dictionary.
     */
    protected final EntityDictionary dictionary;

    /* Sort strings first by length then contents */
    private Comparator<String> comparator = (string1, string2) -> {
        int diff = string1.length() - string2.length();
        return diff == 0 ? string1.compareTo(string2) : diff;
    };

    protected static final boolean ANY = true;
    protected static final boolean ALL = false;

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

        @SuppressWarnings("unchecked")
        T obj = tx.createObject(entityClass);
        PersistentResource<T> newResource = new PersistentResource<>(obj, parent, uuid, requestScope);
        checkPermission(CreatePermission.class, newResource);
        newResource.audit(Audit.Action.CREATE);

        String type = newResource.getType();
        requestScope.getObjectEntityCache().put(type, uuid, newResource.getObject());

        // Initialize null ToMany collections
        requestScope.getDictionary().getRelationships(entityClass).stream()
                .filter(relationName -> newResource.getRelationshipType(relationName).isToMany()
                && newResource.getValue(relationName) == null)
                .forEach(relationName -> newResource.setValue(relationName, new LinkedHashSet<>()));

        // Keep track of new resources for non shareable resources
        requestScope.getNewResources().add(newResource);

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
        this.type = dictionary.getBinding(obj.getClass());
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
        return checkId.equals(id);
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
        T obj = (T) cache.get(dictionary.getBinding(loadClass), id);
        if (obj == null) {
            // try to load object
            Class<?> idType = dictionary.getIdType(loadClass);
            obj = tx.loadObject(loadClass, (Serializable) CoerceUtil.coerce(id, idType));
            if (obj == null) {
                throw new InvalidObjectIdentifierException(id);
            }
        }

        PersistentResource<T> resource = new PersistentResource<>(obj, requestScope);
        checkPermission(ReadPermission.class, resource);

        return resource;
    }

    /**
     * Load a collection from the DB.
     * @param loadClass the load class
     * @param requestScope the request scope
     * @return a filtered collection of resources loaded from the DB.
     */
    @NonNull public static <T> Set<PersistentResource<T>> loadRecords(Class<T> loadClass, RequestScope requestScope) {
        User user = requestScope.getUser();
        DataStoreTransaction tx = requestScope.getTransaction();

        LinkedHashSet<PersistentResource<T>> resources = new LinkedHashSet<>();
        if (isDenyFilter(requestScope, loadClass)) {
            return resources;
        }

        Iterable<T> list;
        ReadPermission annotation = requestScope.getDictionary().getAnnotation(loadClass, ReadPermission.class);
        FilterScope filterScope = loadChecks(annotation, requestScope);
        list = tx.loadObjects(loadClass, filterScope);

        for (T obj : list) {
            resources.add(new PersistentResource<>(obj, requestScope));
        }
        return filter(ReadPermission.class, resources);
    }

    /**
     * Update attribute in existing resource.
     *
     * @param fieldName the field name
     * @param newVal the new val
     * @return true if object updated, false otherwise
     */
    public boolean updateAttribute(String fieldName, Object newVal) {
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, fieldName);
        Object val = getAttribute(fieldName);
        if ((val != newVal) && (val == null || !val.equals(newVal))) {
            this.setValueChecked(fieldName, newVal);
            transaction.save(obj);
            audit(fieldName);
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
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, fieldName);
        checkSharePermission(resourceIdentifiers);
        RelationshipType type = getRelationshipType(fieldName);
        if (type.isToMany()) {
            return updateToManyRelation(fieldName, resourceIdentifiers);
        } else { // To One Relationship
            return updateToOneRelation(fieldName, resourceIdentifiers);
        }
    }

    /**
     * Updates a to-many relationship.
     * @param fieldName the field name
     * @param resourceIdentifiers the resource identifiers
     * @return true if updated. false otherwise
     */
    protected boolean updateToManyRelation(String fieldName, Set<PersistentResource> resourceIdentifiers) {
        Set<PersistentResource> mine = getRelation(fieldName);

        Set<PersistentResource> requested;
        Set<PersistentResource> updated;
        Set<PersistentResource> deleted;

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

        Collection collection = (Collection) this.getValue(fieldName);

        if (collection == null) {
            this.setValue(fieldName, mine);
        }

        deleted
                .stream()
                .forEach(toDelete -> {
                    checkPermission(UpdatePermission.class, this);
                    checkFieldPermission(UpdatePermission.class, this, fieldName);
                    checkPermission(DeletePermission.class, toDelete);
                    delFromCollection(collection, fieldName, toDelete);
                    deleteInverseRelation(fieldName, toDelete.getObject());
                    transaction.save(toDelete.getObject());
                });

        Sets.difference(updated, deleted)
                .stream()
                .forEach(toAdd -> {
                    addToCollection(collection, fieldName, toAdd);
                    addInverseRelation(fieldName, toAdd.getObject());
                    transaction.save(toAdd.getObject());
                });


        transaction.save(getObject());
        audit(fieldName);

        return !updated.isEmpty();
    }

    /**
     * Update a 2-one relationship.
     *
     * @param fieldName the field name
     * @param resourceIdentifiers the resource identifiers
     * @return true if updated. false otherwise
     */
    protected boolean updateToOneRelation(String fieldName, Set<PersistentResource> resourceIdentifiers) {
        Set<PersistentResource> mine = getRelation(fieldName);


        Object newValue;
        if (resourceIdentifiers == null || resourceIdentifiers.isEmpty()) {
            newValue = null;
        } else {
            PersistentResource newResource = (PersistentResource) (resourceIdentifiers.toArray()[0]);
            newValue = newResource.getObject();
        }

        PersistentResource oldResource = !mine.isEmpty() ? mine.iterator().next() : null;

        this.setValueChecked(fieldName, newValue);

        if (oldResource == null) {
            if (newValue == null) {
                return false;
            }
            addInverseRelation(fieldName, newValue);
            transaction.save(newValue);
        } else if (oldResource.getObject().equals(newValue)) {
            return false;
        } else {
            deleteInverseRelation(fieldName, oldResource.getObject());
            transaction.save(oldResource.getObject());
        }

        transaction.save(obj);
        audit(fieldName);
        return true;
    }

    /**
     * Clear all elements from a relation.
     *
     * @param relationName Name of relation to clear
     * @return True if object updated, false otherwise
     */
    public boolean clearRelation(String relationName) {
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, relationName);
        Set<PersistentResource> mine = getRelation(relationName);

        if (mine.isEmpty()) {
            return false;
        }

        RelationshipType type = getRelationshipType(relationName);

        mine.stream()
                .forEach(toDelete -> deleteInverseRelation(relationName, toDelete.getObject()));

        if (type.isToOne()) {
            PersistentResource oldValue = mine.iterator().next();
            this.nullValue(relationName, oldValue);
            transaction.save(oldValue.getObject());
        } else {
            Collection collection = (Collection) this.getValue(relationName);
            mine.stream()
                    .forEach(toDelete -> {
                        checkPermission(UpdatePermission.class, this);
                        checkFieldPermission(UpdatePermission.class, this, relationName);
                        checkPermission(DeletePermission.class, toDelete);
                        delFromCollection(collection, relationName, toDelete);
                        transaction.save(toDelete.getObject());
                    });
        }

        transaction.save(obj);

        audit(relationName);
        return true;
    }

    /**
     * Remove a relationship.
     *
     * @param fieldName the field name
     * @param removeResource the remove resource
     */
    public void removeRelation(String fieldName, PersistentResource removeResource) {
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, fieldName);
        checkPermission(DeletePermission.class, removeResource);
        Object relation = this.getValue(fieldName);

        if (relation instanceof Collection) {
            if (!((Collection) relation).contains(removeResource.getObject())) {

                //Nothing to do
                return;
            }
            delFromCollection((Collection) relation, fieldName, removeResource);
        } else {
            Object oldValue = getValue(fieldName);
            if (oldValue == null || !oldValue.equals(removeResource.getObject())) {

                //Nothing to do
                return;
            }
            this.nullValue(fieldName, removeResource);
        }

        deleteInverseRelation(fieldName, removeResource.getObject());

        transaction.save(removeResource.getObject());
        transaction.save(obj);
        audit(fieldName);
    }

    /**
     * Add relation link from a given parent resource to a child resource.
     *
     * @param fieldName which relation link
     * @param newRelation the new relation
     */
    public void addRelation(String fieldName, PersistentResource newRelation) {
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, fieldName);
        checkSharePermission(Collections.singleton(newRelation));
        Object relation = this.getValue(fieldName);

        if (relation instanceof Collection) {
            addToCollection((Collection) relation, fieldName, newRelation);
            addInverseRelation(fieldName, newRelation.getObject());
        } else {
            throw new InternalServerErrorException("Cannot add a relation to a non-collection.");
        }

        transaction.save(newRelation.getObject());
        transaction.save(obj);
        audit(fieldName);
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

        final Set<PersistentResource> newResources = getRequestScope().getNewResources();

        for (PersistentResource persistentResource : resourceIdentifiers) {
            if (!newResources.contains(persistentResource)) {
                if (persistentResource.isShareable()) {
                    checkPermission(SharePermission.class, persistentResource);
                } else {
                    throw new ForbiddenAccessException();
                }
            }
        }
    }

    /**
     * Checks if this persistent resource's underlying entity is shareable.
     *
     * @return true if this persistent resource's entity is shareable.
     */
    private boolean isShareable() {
        return getRequestScope().getDictionary().isShareable(obj.getClass());
    }

    /**
     * Delete an existing entity.
     *
     * @param parentFieldName If owned by a parent, relationship to update
     * @throws ForbiddenAccessException the forbidden access exception
     */
    public void deleteResource(String parentFieldName) throws ForbiddenAccessException {
        checkPermission(DeletePermission.class, this);
        if (parent.isPresent()) {
            PersistentResource parentResource = parent.get();
            parentResource.removeRelation(parentFieldName, this);
        }
        transaction.delete(getObject());
        audit(Audit.Action.DELETE);
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
        this.setValue("id", id);
    }

    /**
     * Indicates if the ID is generated or not.
     *
     * @return Boolean
     */
    public Boolean isIdGenerated() {
        return getIdAnnotations().stream().anyMatch(a ->
                        a.annotationType().equals(GeneratedValue.class)
        );
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
        /* getRelation performs read permission checks */
        Set<PersistentResource> resources = getRelation(relation);
        for (PersistentResource childResource : resources) {
            if (childResource.matchesId(id)) {
                return childResource;
            }
        }
        throw new InvalidObjectIdentifierException(id);
    }

    /**
     * Get collection of resources from relation field.
     *
     * @param relationName field
     * @return collection relation
     */
    public Set<PersistentResource> getRelation(String relationName) {
        List<String> relations = dictionary.getRelationships(obj);

        String realName = dictionary.getNameFromAlias(obj, relationName);
        relationName = (realName == null) ? relationName : realName;

        if (relationName == null || relations == null || !relations.contains(relationName)) {
            throw new InvalidAttributeException("No attribute " + relationName + " in " + type);
        }

        Set<PersistentResource<Object>> resources = Sets.newLinkedHashSet();

        RelationshipType type = getRelationshipType(relationName);
        Object val = this.getValue(relationName);
        if (val == null) {
            return (Set) resources;
        } else if (isDenyFilter(requestScope, dictionary.getParameterizedType(obj, relationName))) {
            return (Set) resources;
        } else if (val instanceof Collection) {
            Collection filteredVal = (Collection) val;

            if (requestScope.getTransaction() != null && !requestScope.getPredicates().isEmpty()) {
                final Class<?> entityClass = dictionary.getParameterizedType(obj, relationName);
                final String valType = dictionary.getBinding(entityClass);
                final Set<Predicate> predicates = requestScope.getPredicatesOfType(valType);
                filteredVal = requestScope.getTransaction().filterCollection(filteredVal, entityClass, predicates);
            }

            for (Object m : filteredVal) {
                resources.add(new PersistentResource<>(this, m, getRequestScope()));
            }
        } else if (type.isToOne()) {
            resources = new SingleElementSet(new PersistentResource(this, val, getRequestScope()));
        } else {
            resources.add(new PersistentResource(this, val, getRequestScope()));
        }

        return (Set) filter(ReadPermission.class, resources);
    }

    /**
     * If relationship collection type is denied, do not read lazy collection.
     *
     * @return true DENY check type
     */
    private static boolean isDenyFilter(RequestScope requestScope, Class<?> recordClass) {
        if (requestScope.getSecurityMode() == SecurityMode.BYPASS_SECURITY) {
            return false;
        }

        ReadPermission annotation = requestScope.getDictionary().getAnnotation(recordClass, ReadPermission.class);
        FilterScope filterScope = loadChecks(annotation, requestScope);

        return filterScope.getUserPermission() == DENY;
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
        return this.getValue(attr);
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
        return (Class) EntityDictionary.lookupEntityClass(obj.getClass());
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
        final int prime = 31;
        int result = 1;
        String id = dictionary.getId(getObject());
        result = prime * result + ((uuid.isPresent()) ? uuid.hashCode() : 0);
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PersistentResource) {
            PersistentResource that = (PersistentResource) obj;
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
        final Resource resource = new Resource(type, (obj == null)
                ? uuid.orElseThrow(
                        () -> new InvalidEntityBodyException("No id found on object"))
                : dictionary.getId(obj));
        resource.setRelationships(getRelationships());
        resource.setAttributes(getAttributes());
        return resource;
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationships() {
        final Map<String, Relationship> relationshipMap = new LinkedHashMap<>();
        final Set<String> relationshipFields =
                filterFields(ReadPermission.class, this, dictionary.getRelationships(obj));

        for (String field : relationshipFields) {
            Set<PersistentResource> relationships = getRelation(field);
            TreeMap<String, Resource> orderedById = new TreeMap<>(comparator);
            for (PersistentResource relationship : relationships) {
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

        final Set<String> attrFields = filterFields(ReadPermission.class, this, dictionary.getAttributes(obj));
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
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, fieldName);
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
        checkPermission(UpdatePermission.class, oldValue);
        this.setValueChecked(fieldName, null);
    }

    /**
     * Gets a value from an entity and checks read permissions.
     * @param fieldName the field name
     * @return value value
     */
    protected Object getValue(String fieldName) {
        checkPermission(ReadPermission.class, this);
        checkFieldPermission(ReadPermission.class, this, fieldName);

        return getValue(getObject(), fieldName, dictionary);
    }

    /**
     * Adds a new element to a collection and tests update permission.
     * @param collection the collection
     * @param collectionName the collection name
     * @param toAdd the to add
     */
    protected void addToCollection(Collection collection, String collectionName, PersistentResource toAdd) {
        checkPermission(UpdatePermission.class, this);
        checkFieldPermission(UpdatePermission.class, this, collectionName);
        checkPermission(ReadPermission.class, toAdd);

        if (collection == null) {
            collection = Collections.singleton(toAdd.getObject());
            this.setValueChecked(collectionName, collection);
        }

        collection.add(toAdd.getObject());
    }

    /**
     * Deletes an existing element in a collection and tests update and delete permissions.
     * @param collection the collection
     * @param collectionName the collection name
     * @param toDelete the to delete
     */
    protected void delFromCollection(Collection collection,
            String collectionName,
            PersistentResource toDelete) {

        if (collection == null) {
            return;
        }

        collection.remove(toDelete.getObject());
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value the value to set
     */
    protected void setValue(String fieldName, Object value) {
        Class<?> targetClass = obj.getClass();
        try {
            Class<?> fieldClass = dictionary.getType(targetClass, fieldName);
            String realName = dictionary.getNameFromAlias(obj, fieldName);
            fieldName = (realName != null) ? realName : fieldName;
            String setMethod = "set" + WordUtils.capitalize(fieldName);
            Method method = EntityDictionary.findMethod(targetClass, setMethod, fieldClass);
            method.invoke(obj, coerce(value, fieldName, fieldClass));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidAttributeException("No attribute or relation " + fieldName + " in " + targetClass);
        } catch (IllegalArgumentException | NoSuchMethodException noMethod) {
            try {
                Field field = targetClass.getDeclaredField(fieldName);
                field.set(obj, coerce(value, fieldName, field.getType()));
            } catch (NoSuchFieldException | IllegalAccessException noField) {
                throw new InvalidAttributeException("No attribute or relation " + fieldName + " in " + targetClass);
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

        return CoerceUtil.coerce(value, fieldClass);
    }

    private Collection coerceCollection(Collection values, String fieldName, Class<?> fieldClass) {
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

    /**
     * Invoke the get[fieldName] method on the target object OR get the field with the corresponding name.
     * @param target the object to get
     * @param fieldName the field name to get or invoke equivalent get method
     * @param dictionary the dictionary
     * @return the value
     */
    protected static Object getValue(Object target, String fieldName, EntityDictionary dictionary) {
        Class<?> targetClass = EntityDictionary.lookupEntityClass(target.getClass());
        try {
            String realName = dictionary.getNameFromAlias(target, fieldName);
            fieldName = (realName != null) ? realName : fieldName;
            Method method;
            try {
                String getMethod = "get" + WordUtils.capitalize(fieldName);
                method = EntityDictionary.findMethod(targetClass, getMethod);
            } catch (NoSuchMethodException e) {
                String getMethod = "is" + WordUtils.capitalize(fieldName);
                method = EntityDictionary.findMethod(targetClass, getMethod);
            }
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidAttributeException("No attribute or relation " + fieldName + " in " + targetClass);
        } catch (NoSuchMethodException e) {
            try {
                Field field = targetClass.getDeclaredField(fieldName);
                return field.get(target);
            } catch (NoSuchFieldException | IllegalAccessException noField) {
                throw new InvalidAttributeException("No attribute or relation " + fieldName + " in " + targetClass);
            }
        }
    }

    /**
     * If a bidirectional relationship exists, attempts to delete itself from the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param relationValue The value (B) which has been deleted from this object.
     */
    protected void deleteInverseRelation(String relationName, Object relationValue) {
        Class<?> entityClass = EntityDictionary.lookupEntityClass(obj.getClass());

        String inverseRelationName = dictionary.getRelationInverse(entityClass, relationName);
        Object inverseEntity = relationValue; // Assigned to improve readability.

        if (!inverseRelationName.equals("")) {
            Class<?> inverseRelationType = dictionary.getType(inverseEntity.getClass(), inverseRelationName);

            PersistentResource inverseResource = new PersistentResource(this, inverseEntity, getRequestScope());
            Object inverseRelation = inverseResource.getValue(inverseRelationName);

            if (inverseRelation == null) {
                return;
            }

            if (inverseRelation instanceof Collection) {
                checkPermission(UpdatePermission.class, inverseResource);
                checkFieldPermission(UpdatePermission.class, inverseResource, inverseRelationName);
                inverseResource.delFromCollection((Collection) inverseRelation, inverseRelationName, this);
            } else if (inverseRelationType.equals(this.getResourceClass())) {
                inverseResource.nullValue(inverseRelationName, this);
            } else {
                throw new InternalServerErrorException("Relationship type mismatch");
            }
        }
    }

    /**
     * If a bidirectional relationship exists, attempts to add itself to the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     * @param relationName The name of the relationship on this (A) object.
     * @param relationValue The value (B) which has been added to this object.
     */
    protected void addInverseRelation(String relationName, Object relationValue) {
        Class<?> entityClass = EntityDictionary.lookupEntityClass(obj.getClass());

        Object inverseEntity = relationValue; // Assigned to improve readability.
        String inverseRelationName = dictionary.getRelationInverse(entityClass, relationName);

        if (!inverseRelationName.equals("")) {
            Class<?> inverseRelationType = dictionary.getType(inverseEntity.getClass(), inverseRelationName);

            PersistentResource inverseResource = new PersistentResource(this, inverseEntity, getRequestScope());
            Object inverseRelation = inverseResource.getValue(inverseRelationName);

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
        }
    }

    /**
     * Filter a set of PersistentResources.
     *
     * @param <A> the type parameter
     * @param permission the permission
     * @param resources the resources
     * @return Filtered set of resources
     */
    protected static <A extends Annotation, T> Set<PersistentResource<T>> filter(Class<A> permission,
            Set<PersistentResource<T>> resources) {
        Set<PersistentResource<T>> filteredSet = new LinkedHashSet<>();
        for (PersistentResource<T> resource : resources) {
            try {
                checkPermission(permission, resource);
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
     * @param <A> the type parameter
     * @param permission the permission
     * @param resource the resource
     * @param fields the fields
     * @return Filtered set of fields
     */
    protected static <A extends Annotation> Set<String> filterFields(Class<A> permission,
            PersistentResource resource,
            Collection<String> fields) {
        Set<String> filteredSet = new LinkedHashSet<>();
        for (String field : fields) {
            try {
                if (checkIncludeSparseField(resource.getRequestScope().getSparseFields(), resource.type, field)) {
                    checkFieldPermission(permission, resource, field);
                    filteredSet.add(field);
                }
            } catch (ForbiddenAccessException e) {
                // Do nothing. Filter from set.
            }
        }
        return filteredSet;
    }

    /**
     * Check provided access permission.
     *
     * @param annotationClass one of Create, Read, Update or Delete permission annotations
     * @param annotation the instance of the annotation
     * @param resource given resource
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    static <A extends Annotation> void checkPermission(
            Class<A> annotationClass,
            A annotation,
            PersistentResource resource) {
        if (resource.getRequestScope().getSecurityMode() == SecurityMode.BYPASS_SECURITY) {
            return;
        }
        Class<? extends Check>[] anyChecks;
        Class<? extends Check>[] allChecks;
        try {
            anyChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("any").invoke(annotation, (Object[]) null);
            allChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("all").invoke(annotation, (Object[]) null);
        } catch (ReflectiveOperationException e) {
            throw new InvalidSyntaxException("Unknown permission " + annotationClass.getName(), e);
        }

        if (anyChecks.length > 0) {
            resource.requestScope.checkPermissions(annotationClass, anyChecks, ANY, resource);
        } else if (allChecks.length > 0) {
            resource.requestScope.checkPermissions(annotationClass, allChecks, ALL, resource);
        } else {
            throw new InvalidSyntaxException("Unknown permission " + annotationClass.getName());
        }
    }

    static <A extends Annotation> FilterScope loadChecks(A annotation, RequestScope requestScope) {
        if (annotation == null) {
            return new FilterScope(requestScope);
        }

        Class<? extends Check>[] anyChecks;
        Class<? extends Check>[] allChecks;
        Class<? extends Annotation> annotationClass = annotation.getClass();
        try {
            anyChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("any").invoke(annotation, (Object[]) null);
            allChecks = (Class<? extends Check>[]) annotationClass
                    .getMethod("all").invoke(annotation, (Object[]) null);
        } catch (ReflectiveOperationException e) {
            throw new InvalidSyntaxException("Unknown permission " + annotationClass.getName(), e);
        }

        if (anyChecks.length > 0) {
            return new FilterScope(requestScope, ANY, anyChecks);
        } else if (allChecks.length > 0) {
            return new FilterScope(requestScope, ALL, allChecks);
        } else {
            throw new InvalidSyntaxException("Unknown permission " + annotationClass.getName());
        }
    }

    /**
     * Check provided access permission.
     *
     * @param annotationClass one of Create, Read, Update or Delete permission annotations
     * @param resource given resource
     * @see com.yahoo.elide.annotation.CreatePermission
     * @see com.yahoo.elide.annotation.ReadPermission
     * @see com.yahoo.elide.annotation.UpdatePermission
     * @see com.yahoo.elide.annotation.DeletePermission
     */
    static <A extends Annotation> void checkPermission(Class<A> annotationClass,
            PersistentResource resource) {
        A annotation = resource.getDictionary().getAnnotation(resource, annotationClass);
        if (annotation == null) {
            return;
        }
        checkPermission(annotationClass, annotation, resource);
    }

    /**
     * Execute a set of permission checks.
     * @param checks Array of Check annotations
     * @param mode true if ANY, else ALL
     * @param resource provided PersistentResource to check
     */
    static void checkPermissions(Class<? extends Check>[] checks, boolean mode, PersistentResource resource) {
        for (Class<? extends Check> check : checks) {
            Check checkHandler;
            try {
                checkHandler = check.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new InvalidSyntaxException("Illegal permission check " + check.getName(), e);
            }
            boolean ok = resource.getRequestScope().getUser().ok(checkHandler, resource);

            if (ok && mode == ANY) {
                return;
            }

            if (!ok && mode == ALL) {
                throw new ForbiddenAccessException();
            }
        }
        if (mode == ANY) {
            throw new ForbiddenAccessException();
        }
    }

    /**
     * Check a permission on a field.
     * @param <A> the type parameter
     * @param annotationClass the annotation class
     * @param resource the resource
     * @param fieldName the field name
     */
    protected static <A extends Annotation> void checkFieldPermission(Class<A> annotationClass,
            PersistentResource resource,
            String fieldName) {
        A annotation = resource.getDictionary().getAttributeOrRelationAnnotation(resource.getResourceClass(),
                annotationClass,
                fieldName);
        if (annotation == null) {
            return;
        }

        checkPermission(annotationClass, annotation, resource);
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
     * Audit an action on a field.
     * @param fieldName the field name
     */
    protected void audit(String fieldName) {
        Audit[] annotations = dictionary.getAttributeOrRelationAnnotations(getResourceClass(),
                Audit.class,
                fieldName
                );

        if (annotations == null) {
            return;
        }
        for (Audit annotation : annotations) {
            if (annotation.action() == Audit.Action.UPDATE) {
                LogMessage message = new LogMessage(annotation, this);
                getRequestScope().getLogger().log(message);
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
    protected void audit(Audit.Action action) {
        Audit[] annotations = getResourceClass().getAnnotationsByType(Audit.class);

        if (annotations == null) {
            return;
        }
        for (Audit annotation : annotations) {
            if (annotation.action() == action) {
                LogMessage message = new LogMessage(annotation, this);
                getRequestScope().getLogger().log(message);
            }
        }
    }

    /**
     * Helper function for access to OpaqueUser in checks.
     * @return opaque user
     */
    public Object getOpaqueUser() {
        if (getRequestScope().getUser() == null) {
            return null;
        }

        return getRequestScope().getUser().getOpaqueUser();
    }
}
