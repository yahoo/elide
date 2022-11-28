/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.core.dictionary.EntityBinding.EMPTY_BINDING;
import static com.yahoo.elide.core.dictionary.EntityDictionary.getType;
import static com.yahoo.elide.core.type.ClassType.COLLECTION_TYPE;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.NonTransferable;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.audit.InvalidSyntaxException;
import com.yahoo.elide.core.audit.LogMessage;
import com.yahoo.elide.core.audit.LogMessageImpl;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.RelationshipType;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.exceptions.InternalServerErrorException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.InvalidEntityBodyException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.InPredicate;
import com.yahoo.elide.core.filter.visitors.VerifyFieldAccessFilterExpressionVisitor;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Pagination;
import com.yahoo.elide.core.request.Sorting;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.permissions.ExpressionResult;
import com.yahoo.elide.core.security.visitors.CanPaginateVisitor;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.jsonapi.document.processors.WithMetadata;
import com.yahoo.elide.jsonapi.models.Data;
import com.yahoo.elide.jsonapi.models.Meta;
import com.yahoo.elide.jsonapi.models.Relationship;
import com.yahoo.elide.jsonapi.models.Resource;
import com.yahoo.elide.jsonapi.models.ResourceIdentifier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

import io.reactivex.Observable;
import lombok.NonNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
public class PersistentResource<T> implements com.yahoo.elide.core.security.PersistentResource<T> {
    public static final Set<String> ALL_FIELDS = null;
    public static final String CLASS_NO_FIELD = "";
    /**
     * The Dictionary.
     */
    protected final EntityDictionary dictionary;
    private final Type type;
    private final String typeName;
    private final ResourceLineage lineage;
    private final Optional<String> uuid;
    private final DataStoreTransaction transaction;
    private final RequestScope requestScope;
    /* Sort strings first by length then contents */
    private final Comparator<String> lengthFirstComparator = (string1, string2) -> {
        int diff = string1.length() - string2.length();
        return diff == 0 ? string1.compareTo(string2) : diff;
    };
    protected T obj;
    private int hashCode = 0;

    /**
     * Construct a new resource from the ID provided.
     *
     * @param obj                the obj
     * @param parent             the parent
     * @param id                 the id
     * @param parentRelationship The parent relationship traversed to this resource.
     * @param scope              the request scope
     */
    public PersistentResource(
            @NonNull T obj,
            PersistentResource parent,
            String parentRelationship,
            String id,
            @NonNull RequestScope scope
    ) {
        this.obj = obj;
        this.type = getType(obj);
        this.uuid = Optional.ofNullable(id);
        this.lineage = parent != null
                ? new ResourceLineage(parent.lineage, parent, parentRelationship)
                : new ResourceLineage();
        this.dictionary = scope.getDictionary();
        this.typeName = dictionary.getJsonAliasFor(type);
        this.transaction = scope.getTransaction();
        this.requestScope = scope;
        dictionary.initializeEntity(obj);
    }

    /**
     * Construct a new resource from the ID provided.
     *
     * @param obj   the obj
     * @param id    the id
     * @param scope the request scope
     */
    public PersistentResource(
            @NonNull T obj,
            String id,
            @NonNull RequestScope scope
    ) {
        this(obj, null, null, id, scope);
    }

    /**
     * Create a resource in the database.
     *
     * @param entityClass  the entity class
     * @param requestScope the request scope
     * @param uuid         the (optional) uuid
     * @param <T>          object type
     * @return persistent resource
     */
    public static <T> PersistentResource<T> createObject(
            Type<T> entityClass,
            RequestScope requestScope,
            Optional<String> uuid) {
        return createObject(null, null, entityClass, requestScope, uuid);
    }

    /**
     * Create a resource in the database.
     *
     * @param parent             - The immediate ancestor in the lineage or null if this is a root.
     * @param parentRelationship - The name of the parent relationship traversed to create this object.
     * @param entityClass        the entity class
     * @param requestScope       the request scope
     * @param uuid               the (optional) uuid
     * @param <T>                object type
     * @return persistent resource
     */
    public static <T> PersistentResource<T> createObject(
            PersistentResource<?> parent,
            String parentRelationship,
            Type<T> entityClass,
            RequestScope requestScope,
            Optional<String> uuid) {

        T obj = requestScope.getTransaction().createNewObject(entityClass, requestScope);

        String id = uuid.orElse(null);

        PersistentResource<T> newResource = new PersistentResource<>(obj, parent, parentRelationship, id, requestScope);

        //The ID must be assigned before we add it to the new resources set.  Persistent resource
        //hashcode and equals are only based on the ID/UUID & type.
        assignId(newResource, id);

        // Keep track of new resources for non-transferable resources
        requestScope.getNewPersistentResources().add(newResource);
        checkPermission(CreatePermission.class, newResource);

        newResource.auditClass(Audit.Action.CREATE, new ChangeSpec(newResource, null, null, newResource.getObject()));

        requestScope.publishLifecycleEvent(newResource, CREATE);

        requestScope.setUUIDForObject(newResource.type, id, newResource.getObject());

        // Initialize null ToMany collections
        requestScope.getDictionary().getRelationships(entityClass).stream()
                .filter(relationName -> newResource.getRelationshipType(relationName).isToMany()
                        && newResource.getValueUnchecked(relationName) == null)
                .forEach(relationName -> newResource.setValue(relationName, new LinkedHashSet<>()));

        newResource.markDirty();
        return newResource;
    }

    /**
     * Load an single entity from the DB.
     *
     * @param projection   What to load from the DB.
     * @param id           the id
     * @param requestScope the request scope
     * @param <T>          type of resource
     * @return resource persistent resource
     * @throws InvalidObjectIdentifierException the invalid object identifier exception
     */
    @SuppressWarnings("resource")
    @NonNull
    public static <T> PersistentResource<T> loadRecord(
            EntityProjection projection,
            String id,
            RequestScope requestScope
    ) throws InvalidObjectIdentifierException {
        Preconditions.checkNotNull(projection);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(requestScope);

        DataStoreTransaction tx = requestScope.getTransaction();
        EntityDictionary dictionary = requestScope.getDictionary();
        Type<?> loadClass = projection.getType();

        // Check the resource cache if exists
        Object obj = requestScope.getObjectById(loadClass, id);
        if (obj == null) {
            // try to load object
            Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(loadClass,
                    requestScope, projection.getRequestedFields());
            Type<?> idType = dictionary.getIdType(loadClass);

            projection = projection
                    .copyOf()
                    .filterExpression(permissionFilter.orElse(null))
                    .build();

            obj = tx.loadObject(projection, (Serializable) CoerceUtil.coerce(id, idType), requestScope);
            if (obj == null) {
                throw new InvalidObjectIdentifierException(id, dictionary.getJsonAliasFor(loadClass));
            }
        }

        PersistentResource<T> resource = new PersistentResource<>(
                (T) obj,
                requestScope.getUUIDFor(obj),
                requestScope);

        // No need to have read access for a newly created object
        if (!requestScope.getNewResources().contains(resource)) {
            resource.checkFieldAwarePermissions(ReadPermission.class, projection.getRequestedFields());
        }

        return resource;
    }

    /**
     * Get a FilterExpression parsed from FilterExpressionCheck.
     *
     * @param <T>             the type parameter
     * @param loadClass       the load class
     * @param requestScope    the request scope
     * @param requestedFields The set of requested fields
     * @return a FilterExpression defined by FilterExpressionCheck.
     */
    private static <T> Optional<FilterExpression> getPermissionFilterExpression(Type<T> loadClass,
                                                                                RequestScope requestScope,
                                                                                Set<String> requestedFields) {
        try {
            return requestScope.getPermissionExecutor().getReadPermissionFilter(loadClass, requestedFields);
        } catch (ForbiddenAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Load a collection from the datastore.
     *
     * @param projection   the projection to load
     * @param requestScope the request scope
     * @param ids          a list of object identifiers to optionally load.  Can be empty.
     * @return a filtered collection of resources loaded from the datastore.
     */
    public static Observable<PersistentResource> loadRecords(
            EntityProjection projection,
            List<String> ids,
            RequestScope requestScope) {

        Type<?> loadClass = projection.getType();
        Pagination pagination = projection.getPagination();
        Sorting sorting = projection.getSorting();

        FilterExpression filterExpression = projection.getFilterExpression();

        EntityDictionary dictionary = requestScope.getDictionary();

        DataStoreTransaction tx = requestScope.getTransaction();

        if (shouldSkipCollection(loadClass, ReadPermission.class, requestScope, projection.getRequestedFields())) {
            if (ids.isEmpty()) {
                return Observable.empty();
            }
            throw new InvalidObjectIdentifierException(ids.toString(), dictionary.getJsonAliasFor(loadClass));
        }

        Set<String> requestedFields = projection.getRequestedFields();

        if (pagination != null && !pagination.isDefaultInstance()
                && !CanPaginateVisitor.canPaginate(loadClass, dictionary, requestScope, requestedFields)) {
            throw new BadRequestException(String.format("Cannot paginate %s",
                    dictionary.getJsonAliasFor(loadClass)));
        }

        Set<PersistentResource> newResources = new LinkedHashSet<>();

        if (!ids.isEmpty()) {
            String typeAlias = dictionary.getJsonAliasFor(loadClass);
            newResources = requestScope.getNewPersistentResources().stream()
                    .filter(resource -> typeAlias.equals(resource.getTypeName())
                            && ids.contains(resource.getUUID().orElse("")))
                    .collect(Collectors.toSet());
            FilterExpression idExpression = buildIdFilterExpression(ids, loadClass, dictionary, requestScope);

            // Combine filters if necessary
            filterExpression = Optional.ofNullable(filterExpression)
                    .map(fe -> (FilterExpression) new AndFilterExpression(idExpression, fe))
                    .orElse(idExpression);
        }

        Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(loadClass, requestScope,
                requestedFields);
        if (permissionFilter.isPresent()) {
            if (filterExpression != null) {
                filterExpression = new AndFilterExpression(filterExpression, permissionFilter.get());
            } else {
                filterExpression = permissionFilter.get();
            }
        }

        EntityProjection modifiedProjection = projection
                .copyOf()
                .filterExpression(filterExpression)
                .sorting(sorting)
                .pagination(pagination)
                .build();

        Observable<PersistentResource> existingResources = filter(
                ReadPermission.class,
                Optional.ofNullable(modifiedProjection.getFilterExpression()),
                projection.getRequestedFields(),
                Observable.fromIterable(
                        new PersistentResourceSet(tx.loadObjects(modifiedProjection, requestScope), requestScope))
        );

        // TODO: Sort again in memory now that two sets are glommed together?
        Observable<PersistentResource> allResources =
                Observable.fromIterable(newResources).mergeWith(existingResources);

        Set<String> foundIds = new HashSet<>();

        allResources = allResources.doOnNext((resource) -> {
            String id = (String) resource.getUUID().orElseGet(resource::getId);
            if (ids.contains(id)) {
                foundIds.add(id);
            }
        });

        allResources = allResources.doOnComplete(() -> {
            Set<String> missedIds = Sets.difference(new HashSet<>(ids), foundIds);
            if (!missedIds.isEmpty()) {
                throw new InvalidObjectIdentifierException(missedIds.toString(), dictionary.getJsonAliasFor(loadClass));
            }
        });

        return allResources;
    }

    /**
     * Build an id filter expression for a particular entity type.
     *
     * @param ids        Ids to include in the filter expression
     * @param entityType Type of entity
     * @return Filter expression for given ids and type.
     */
    private static FilterExpression buildIdFilterExpression(List<String> ids,
                                                            Type<?> entityType,
                                                            EntityDictionary dictionary,
                                                            RequestScope scope) {
        Type<?> idType = dictionary.getIdType(entityType);
        String idField = dictionary.getIdFieldName(entityType);

        List<Object> coercedIds = ids.stream()
                .filter(id -> scope.getObjectById(entityType, id) == null) // these don't exist yet
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
     * Determine whether or not to skip loading a collection.
     *
     * @param resourceClass   Resource class
     * @param annotationClass Annotation class
     * @param requestedFields The set of requested fields
     * @param requestScope    Request scope
     * @return True if collection should be skipped (i.e. denied access), false otherwise
     */
    private static boolean shouldSkipCollection(Type<?> resourceClass, Class<? extends Annotation> annotationClass,
                                                RequestScope requestScope, Set<String> requestedFields) {
        try {
            requestScope.getPermissionExecutor().checkUserPermissions(resourceClass, annotationClass, requestedFields);
            return false;
        } catch (ForbiddenAccessException e) {
            return true;
        }
    }

    /**
     * Invoke the get[fieldName] method on the target object OR get the field with the corresponding name.
     *
     * @param target       the object to get
     * @param fieldName    the field name to get or invoke equivalent get method
     * @param requestScope the request scope
     * @return the value
     */
    public static Object getValue(Object target, String fieldName, RequestScope requestScope) {
        return requestScope.getDictionary().getValue(target, fieldName, requestScope);
    }

    /**
     * Filter a set of PersistentResources.
     * Verify fields have ReadPermission on filter join.
     *
     * @param permission the permission
     * @param resources  the resources
     * @return Filtered set of resources
     */
    protected static Observable<PersistentResource> filter(Class<? extends Annotation> permission,
                                                           Optional<FilterExpression> filter,
                                                           Set<String> requestedFields,
                                                           Observable<PersistentResource> resources) {

        return resources.filter(resource -> {
            try {
                // NOTE: This is for avoiding filtering on _newly created_ objects within this transaction.
                // Namely-- in a JSONPATCH request or GraphQL request-- we need to read all newly created
                // resources /regardless/ of whether or not we actually have permission to do so; this is to
                // retrieve the object id to return to the caller. If no fields on the object are readable by the caller
                // then they will be filtered out and only the id is returned. Similarly, all future requests to this
                // object will behave as expected.
                if (!resource.getRequestScope().getNewResources().contains(resource)) {
                    resource.checkFieldAwarePermissions(permission, requestedFields);
                    // Verify fields have ReadPermission on filter join
                    return !filter.isPresent()
                            || filter.get().accept(new VerifyFieldAccessFilterExpressionVisitor(resource));
                }
                return true;
            } catch (ForbiddenAccessException e) {
                return false;
            }
        });
    }

    private static <A extends Annotation> ExpressionResult checkPermission(
            Class<A> annotationClass, PersistentResource resource) {
        return resource.requestScope.getPermissionExecutor().checkPermission(annotationClass, resource);
    }

    private static <A extends Annotation> ExpressionResult checkUserPermission(
            Class<A> annotationClass, Object obj, RequestScope requestScope, Set<String> requestedFields) {
        return requestScope.getPermissionExecutor()
                .checkUserPermissions(getType(obj), annotationClass, requestedFields);
    }

    protected static boolean checkIncludeSparseField(Map<String, Set<String>> sparseFields, String type,
                                                     String fieldName) {
        if (!sparseFields.isEmpty()) {
            if (!sparseFields.containsKey(type)) {
                return false;
            }

            return sparseFields.get(type).contains(fieldName);
        }

        return true;
    }

    /**
     * Assign provided id if id field is not generated.
     *
     * @param persistentResource resource
     * @param id                 resource id
     */
    private static void assignId(PersistentResource persistentResource, String id) {

        //If id field is not a `@GeneratedValue` or mapped via a `@MapsId` attribute
        //then persist the provided id
        if (!persistentResource.isIdGenerated()) {
            if (StringUtils.isNotEmpty(id)) {
                persistentResource.setId(id);
            } else {
                //If expecting id to persist and id is not present, throw exception
                throw new BadRequestException(
                        "No id provided, cannot persist " + persistentResource.getTypeName());
            }
        }
    }

    private static <T> T firstOrNullIfEmpty(final Collection<T> coll) {
        return CollectionUtils.isEmpty(coll) ? null : IterableUtils.first(coll);
    }

    public static <T> T firstOrNullIfEmpty(final Observable<T> coll) {
        return firstOrNullIfEmpty(coll.toList().blockingGet());
    }

    @Override
    public String toString() {
        return String.format("PersistentResource{type=%s, id=%s}", typeName, uuid.orElseGet(this::getId));
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
     * Update attribute in existing resource.
     *
     * @param fieldName the field name
     * @param newVal    the new val
     * @return true if object updated, false otherwise
     */
    public boolean updateAttribute(String fieldName, Object newVal) {
        Type<?> fieldClass = dictionary.getType(getResourceType(), fieldName);
        final Object coercedNewValue = dictionary.coerce(obj, newVal, fieldName, fieldClass);
        Object val = getValueUnchecked(fieldName);
        checkFieldAwareDeferPermissions(UpdatePermission.class, fieldName, coercedNewValue, val);
        if (!Objects.equals(val, coercedNewValue)) {
            if (val == null
                    || coercedNewValue == null
                    || !dictionary.isComplexAttribute(getType(obj), fieldName)) {
                this.setValueChecked(fieldName, coercedNewValue);
            } else {
                if (newVal instanceof Map) {

                    //We perform a copy here for two reasons:
                    //1. We want the original so we can dispatch update life cycle hooks.
                    //2. Some stores (Hibernate) won't notice changes to an attribute if the attribute
                    //has a @TypeDef annotation unless we modify the reference in the parent object.  This rules
                    //out an update in place strategy.
                    Object copy = copyComplexAttribute(val);

                    //Update the copy.
                    this.updateComplexAttribute(dictionary, (Map<String, Object>) newVal, copy, requestScope);

                    //Set the copy.
                    dictionary.setValue(obj, fieldName, copy);
                    triggerUpdate(fieldName, val, copy);
                } else {
                    this.setValueChecked(fieldName, coercedNewValue);
                }
            }
            this.markDirty();
            //Hooks for customize logic for setAttribute/Relation
            if (dictionary.isAttribute(getType(obj), fieldName)) {
                transaction.setAttribute(obj, Attribute.builder()
                        .name(fieldName)
                        .type(fieldClass)
                        .argument(Argument.builder()
                                .name("_UNUSED_")
                                .value(newVal).build())
                        .build(), requestScope);
            }
            return true;
        }
        return false;
    }

    private void updateComplexAttribute(EntityDictionary dictionary,
                                        Map<String, Object> updateValue,
                                        Object currentValue,
                                        RequestScope scope) {
        for (String field : updateValue.keySet()) {
            final Object newValue = updateValue.get(field);
            final Object coercedNewValue =
                    dictionary.coerce(currentValue, newValue, field, dictionary.getType(currentValue, field));
            final Object newOriginal = dictionary.getValue(currentValue, field, scope);
            if (!Objects.equals(newOriginal, coercedNewValue)) {
                if (newOriginal == null
                        || coercedNewValue == null
                        || !dictionary.isComplexAttribute(ClassType.of(currentValue.getClass()), field)) {
                    dictionary.setValue(currentValue, field, coercedNewValue);
                } else {
                    if (newValue instanceof Map) {
                        this.updateComplexAttribute(dictionary, (Map<String, Object>) newValue, newOriginal, scope);
                    } else {
                        dictionary.setValue(currentValue, field, coercedNewValue);
                    }
                }
            }
        }
    }

    /**
     * Copies a complex attribute.  If the attribute fields are complex, recurses to perform a deep copy.
     * @param object The attribute to copy.
     * @return The copy.
     */
    private Object copyComplexAttribute(Object object) {
        if (object == null) {
            return null;
        }

        Type<?> type = getType(object);
        EntityBinding binding = dictionary.getEntityBinding(type);

        Preconditions.checkState(! binding.equals(EMPTY_BINDING), "Model not found.");
        Preconditions.checkState(binding.apiRelationships.isEmpty(), "Deep copy of relationships not supported");

        Object copy;
        try {
            copy = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot perform deep copy of " + type.getName(), e);
        }

        binding.apiAttributes.forEach(attribute -> {
            Object newValue;
            Object oldValue = dictionary.getValue(object, attribute, requestScope);
            if (! dictionary.isComplexAttribute(type, attribute)) {
                newValue = oldValue;
            } else {
                newValue = copyComplexAttribute(oldValue);
            }
            dictionary.setValue(copy, attribute, newValue);
        });

        return copy;
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
     *
     * @param fieldName           the field name
     * @param resourceIdentifiers the resource identifiers
     * @return True if object updated, false otherwise
     */
    public boolean updateRelation(String fieldName, Set<PersistentResource> resourceIdentifiers) {
        RelationshipType type = getRelationshipType(fieldName);

        Set<PersistentResource> resources = filter(
                ReadPermission.class,
                Optional.empty(),
                ALL_FIELDS,
                getRelationUncheckedUnfiltered(fieldName)).toList(LinkedHashSet::new).blockingGet();

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
     *
     * @param fieldName           the field name
     * @param resourceIdentifiers the resource identifiers
     * @param mine                Existing, filtered relationships for field name
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

        checkTransferablePermission(added);

        Set<Object> newRelationships = new LinkedHashSet<>();
        Set<Object> deletedRelationships = new LinkedHashSet<>();

        deleted
                .stream()
                .forEach(toDelete -> {
                    deletedRelationships.add(toDelete.getObject());
                });

        added
                .stream()
                .forEach(toAdd -> {
                    newRelationships.add(toAdd.getObject());
                });

        Collection collection = (Collection) this.getValueUnchecked(fieldName);
        modifyCollection(collection, fieldName, newRelationships, deletedRelationships, true);

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
     * @param fieldName           the field name
     * @param resourceIdentifiers the resource identifiers
     * @param mine                Existing, filtered relationships for field name
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
            checkTransferablePermission(resourceIdentifiers);
        } else if (oldResource.getObject().equals(newValue)) {
            return false;
        } else {
            checkTransferablePermission(resourceIdentifiers);
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
        Set<PersistentResource> mine = filter(ReadPermission.class, Optional.empty(),
                ALL_FIELDS,
                getRelationUncheckedUnfiltered(relationName)).toList(LinkedHashSet::new).blockingGet();

        checkFieldAwareDeferPermissions(UpdatePermission.class, relationName, Collections.emptySet(),
                mine.stream().map(PersistentResource::getObject).collect(Collectors.toSet()));

        if (mine.isEmpty()) {
            return false;
        }

        RelationshipType type = getRelationshipType(relationName);

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
                            deletedRelationships.add(toDelete.getObject());
                        });
                modifyCollection(collection, relationName, Collections.emptySet(), deletedRelationships, true);
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
     * @param fieldName      the field name
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
            modifyCollection((Collection) relation, fieldName, Collections.emptySet(),
                    Set.of(removeResource.getObject()), true);
        } else {
            if (relation == null || removeResource == null || !relation.equals(removeResource.getObject())) {
                //Nothing to do
                return;
            }
            this.nullValue(fieldName, removeResource);

            if (hasInverseRelation(fieldName)) {
                deleteInverseRelation(fieldName, removeResource.getObject());
                removeResource.markDirty();
            }
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
     * Add relation link from a given parent resource to a child resource.
     *
     * @param fieldName   which relation link
     * @param newRelation the new relation
     */
    public void addRelation(String fieldName, PersistentResource newRelation) {
        checkTransferablePermission(Collections.singleton(newRelation));
        Object relation = this.getValueUnchecked(fieldName);

        if (relation instanceof Collection) {
            if (modifyCollection((Collection) relation, fieldName,
                    Set.of(newRelation.getObject()), Collections.emptySet(), true)) {
                this.markDirty();

                //Hook for updateToManyRelation
                transaction.updateToManyRelation(transaction, obj, fieldName,
                        Sets.newHashSet(newRelation.getObject()), new LinkedHashSet<>(), requestScope);

                addInverseRelation(fieldName, newRelation.getObject());
            }
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
    protected void checkTransferablePermission(Set<PersistentResource> resourceIdentifiers) {
        if (resourceIdentifiers == null) {
            return;
        }

        final Set<PersistentResource> newResources = getRequestScope().getNewPersistentResources();

        for (PersistentResource persistentResource : resourceIdentifiers) {

            //New resources are exempt from NonTransferable checks
            if (newResources.contains(persistentResource)
                    //This allows nested object hierarchies of non-transferables that are created in more than one
                    //client request. A & B are created in one request and C is created in a subsequent request).
                    //Even though B is non-transferable, while creating C in /A/B, C is allowed to
                    //reference B because B & C are part of the same non-transferable object hierarchy.
                    //To do this, the client must be able to read B (since they navigated through it) and also
                    //update the relationship that links B & C.

                    //The object being added (C) is non-transferable
                    || (!dictionary.isTransferable(getResourceType())
                    //The object being added to (B) is not strict
                    && !dictionary.isStrictNonTransferable(persistentResource.getResourceType())
                    //B is in C's lineage (/B/C).
                    && persistentResource.equals(lineage.getParent()))) {
                continue;
            }

            checkPermission(NonTransferable.class, persistentResource);
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

        Type<?> resourceClass = getResourceType();
        List<String> relationships = dictionary.getRelationships(resourceClass);
        for (String relationName : relationships) {

            /* Skip updating inverse relationships for deletes which are cascaded */
            if (dictionary.cascadeDeletes(resourceClass, relationName)) {
                continue;
            }
            String inverseRelationName = dictionary.getRelationInverse(resourceClass, relationName);
            if (!"".equals(inverseRelationName)) {
                for (PersistentResource inverseResource : getRelationUncheckedUnfiltered(relationName)
                        .toList().blockingGet()) {
                    if (hasInverseRelation(relationName)) {
                        deleteInverseRelation(relationName, inverseResource.getObject());
                        inverseResource.markDirty();
                    }
                }
            }
        }

        transaction.delete(getObject(), requestScope);
        auditClass(Audit.Action.DELETE, new ChangeSpec(this, null, getObject(), null));
        requestScope.publishLifecycleEvent(this, DELETE);
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
        dictionary.setId(obj, id);
    }

    /**
     * Indicates if the ID is generated or not.
     *
     * @return Boolean
     */
    public boolean isIdGenerated() {
        return dictionary.getEntityBinding(type).isIdGenerated();
    }

    /**
     * Gets UUID.
     *
     * @return the UUID
     */
    @Override
    public Optional<String> getUUID() {
        return uuid;
    }

    /**
     * Get relation looking for a _single_ id.
     * <p>
     * NOTE: Filter expressions for this type are _not_ applied at this level.
     *
     * @param relationship The relationship
     * @param id           single id to lookup
     * @return The PersistentResource of the sought id or null if does not exist.
     */
    public PersistentResource getRelation(com.yahoo.elide.core.request.Relationship relationship, String id) {
        List<PersistentResource> resources =
                getRelation(Collections.singletonList(id), relationship).toList().blockingGet();

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
     * Load a relation from the PersistentResource.
     *
     * @param relationship the relation
     * @param ids          a list of object identifiers to optionally load.  Can be empty.
     * @return PersistentResource relation
     */
    public Observable<PersistentResource> getRelation(List<String> ids,
                                                      com.yahoo.elide.core.request.Relationship relationship) {

        FilterExpression filterExpression = Optional.ofNullable(relationship.getProjection().getFilterExpression())
                .orElse(null);

        assertPropertyExists(relationship.getName());
        Type<?> entityType = dictionary.getParameterizedType(getResourceType(), relationship.getName());

        Set<PersistentResource> newResources = new LinkedHashSet<>();

        /* If this is a bulk edit request and the ID we are fetching for is newly created... */
        if (!ids.isEmpty()) {
            // Fetch our set of new resources that we know about since we can't find them in the datastore
            newResources = requestScope.getNewPersistentResources().stream()
                    .filter(resource -> entityType.isAssignableFrom(resource.getResourceType())
                            && ids.contains(resource.getUUID().orElse("")))
                    .collect(Collectors.toSet());

            FilterExpression idExpression = buildIdFilterExpression(ids, entityType, dictionary, requestScope);

            // Combine filters if necessary
            filterExpression = Optional.ofNullable(relationship.getProjection().getFilterExpression())
                    .map(fe -> (FilterExpression) new AndFilterExpression(idExpression, fe))
                    .orElse(idExpression);
        }

        // TODO: Filter on new resources?
        // TODO: Update pagination to subtract the number of new resources created?

        Observable<PersistentResource> existingResources = filter(
                ReadPermission.class,
                Optional.ofNullable(filterExpression),
                relationship.getProjection().getRequestedFields(),
                getRelation(relationship.copyOf()
                        .projection(relationship.getProjection().copyOf()
                                .filterExpression(filterExpression)
                                .build())
                        .build(), true));

        // TODO: Sort again in memory now that two sets are glommed together?
        Observable<PersistentResource> allResources =
                Observable.fromIterable(newResources).mergeWith(existingResources);

        Set<String> foundIds = new HashSet<>();

        allResources = allResources.doOnNext((resource) -> {
            String id = (String) (resource.getUUID().orElseGet(resource::getId));
            if (ids.contains(id)) {
                foundIds.add(id);
            }
        });

        allResources = allResources.doOnComplete(() -> {
            Set<String> missedIds = Sets.difference(new HashSet<>(ids), foundIds);
            if (!missedIds.isEmpty()) {
                throw new InvalidObjectIdentifierException(missedIds.toString(), relationship.getName());
            }
        });

        return allResources;
    }

    /**
     * Get observable of resources from relation field.
     *
     * @param relationship relationship
     * @return collection relation
     */
    public Observable<PersistentResource> getRelationCheckedFiltered(
            com.yahoo.elide.core.request.Relationship relationship) {
        return filter(ReadPermission.class,
                Optional.ofNullable(relationship.getProjection().getFilterExpression()),
                relationship.getProjection().getRequestedFields(),
                getRelation(relationship, true));
    }

    private Observable<PersistentResource> getRelationUncheckedUnfiltered(String relationName) {
        assertPropertyExists(relationName);
        return getRelation(com.yahoo.elide.core.request.Relationship.builder()
                .name(relationName)
                .alias(relationName)
                .projection(EntityProjection.builder()
                        .type(dictionary.getParameterizedType(getResourceType(), relationName))
                        .build())
                .build(), false);
    }

    private void assertPropertyExists(String propertyName) {
        if (propertyName == null || dictionary.getParameterizedType(obj, propertyName) == null) {
            throw new InvalidAttributeException(propertyName, this.getTypeName());
        }
    }

    private Observable<PersistentResource> getRelation(com.yahoo.elide.core.request.Relationship relationship,
                                                       boolean checked) {

        if (checked && !checkRelation(relationship)) {
            return Observable.empty();
        }

        Type<?> relationClass = dictionary.getParameterizedType(obj, relationship.getName());

        Optional<Pagination> pagination = Optional.ofNullable(relationship.getProjection().getPagination());

        if (pagination.filter(Predicates.not(Pagination::isDefaultInstance)).isPresent()
                && !CanPaginateVisitor.canPaginate(
                relationClass,
                dictionary,
                requestScope,
                relationship.getProjection().getRequestedFields())) {

            throw new BadRequestException(String.format("Cannot paginate %s",
                    dictionary.getJsonAliasFor(relationClass)));
        }

        return getRelationUnchecked(relationship);
    }

    /**
     * Check the permissions of the relationship, and return true or false.
     *
     * @param relationship The relationship to the entity
     * @return True if the relationship to the entity has valid permissions for the user
     */
    protected boolean checkRelation(com.yahoo.elide.core.request.Relationship relationship) {
        String relationName = relationship.getName();

        String realName = dictionary.getNameFromAlias(obj, relationName);
        relationName = (realName == null) ? relationName : realName;

        assertPropertyExists(relationName);

        checkFieldAwareDeferPermissions(ReadPermission.class, relationName, null, null);

        return !shouldSkipCollection(
                dictionary.getParameterizedType(obj, relationName),
                ReadPermission.class,
                requestScope,
                relationship.getProjection().getRequestedFields());
    }

    /**
     * Get collection of resources from relation field.  Does not filter the relationship and does
     * not invoke lifecycle hooks.
     *
     * @param relationship the relationship to fetch
     * @return collection relation
     */
    public Observable<PersistentResource> getRelationChecked(com.yahoo.elide.core.request.Relationship relationship) {
        if (!checkRelation(relationship)) {
            return Observable.empty();
        }
        return getRelationUnchecked(relationship);
    }

    /**
     * Retrieve an unchecked set of relations.
     */
    private Observable<PersistentResource> getRelationUnchecked(
            com.yahoo.elide.core.request.Relationship relationship) {
        String relationName = relationship.getName();
        FilterExpression filterExpression = relationship.getProjection().getFilterExpression();
        Pagination pagination = relationship.getProjection().getPagination();
        Sorting sorting = relationship.getProjection().getSorting();

        RelationshipType type = getRelationshipType(relationName);
        final Type<?> relationClass = dictionary.getParameterizedType(obj, relationName);
        if (relationClass == null) {
            throw new InvalidAttributeException(relationName, this.getTypeName());
        }

        //Invoke filterExpressionCheck and then merge with filterExpression.
        Optional<FilterExpression> permissionFilter = getPermissionFilterExpression(relationClass,
                requestScope, relationship.getProjection().getRequestedFields());
        Optional<FilterExpression> computedFilters = Optional.ofNullable(filterExpression);

        if (permissionFilter.isPresent() && filterExpression != null) {
            FilterExpression mergedExpression =
                    new AndFilterExpression(filterExpression, permissionFilter.get());
            computedFilters = Optional.of(mergedExpression);
        } else if (permissionFilter.isPresent()) {
            computedFilters = permissionFilter;
        }

        com.yahoo.elide.core.request.Relationship modifiedRelationship = relationship.copyOf()
                .projection(relationship.getProjection().copyOf()
                        .filterExpression(computedFilters.orElse(null))
                        .sorting(sorting)
                        .pagination(pagination)
                        .build()
                ).build();

        Observable<PersistentResource> resources;

        if (type.isToMany()) {
            DataStoreIterable val = transaction.getToManyRelation(transaction, obj, modifiedRelationship, requestScope);

            if (val == null) {
                return Observable.empty();
            }
            resources = Observable.fromIterable(
                    new PersistentResourceSet(this, relationName, val, requestScope));
        } else {
            Object val = transaction.getToOneRelation(transaction, obj, modifiedRelationship, requestScope);
            if (val == null) {
                return Observable.empty();
            }
            resources = Observable.fromArray(new PersistentResource(val, this, relationName,
                    requestScope.getUUIDFor(val), requestScope));
        }

        return resources;
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
     *
     * @param attr Attribute name
     * @return Object value for attribute
     */
    @Deprecated
    public Object getAttribute(String attr) {
        assertPropertyExists(attr);

        return this.getAttribute(
                Attribute.builder()
                        .name(attr)
                        .alias(attr)
                        .type(dictionary.getParameterizedType(getResourceType(), attr))
                        .build());
    }

    /**
     * Get the value for a particular attribute (i.e. non-relational field)
     *
     * @param attr the Attribute
     * @return Object value for attribute
     */
    public Object getAttribute(Attribute attr) {
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
     *
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
    public Type<T> getResourceType() {
        return (Type) dictionary.lookupBoundClass(getType(obj));
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    @Override
    public String getTypeName() {
        return typeName;
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
            return this.matchesId(theirId) && Objects.equals(this.typeName, that.typeName);
        }
        return false;
    }

    /**
     * Returns whether or not this resource was created in this transaction.
     *
     * @return True if this resource is newly created.
     */
    public boolean isNewlyCreated() {
        return requestScope.getNewResources().contains(this);
    }

    /**
     * Gets lineage.
     *
     * @return the lineage
     */
    public ResourceLineage getLineage() {
        return this.lineage;
    }

    /**
     * Gets dictionary.
     *
     * @return the dictionary
     */
    public EntityDictionary getDictionary() {
        return dictionary;
    }

    /**
     * Gets request scope.
     *
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
     *
     * @return The Resource
     */
    public Resource toResource(EntityProjection projection) {
        return toResource(() -> getRelationships(projection), this::getAttributes);
    }

    /**
     * Fetch a resource with support for lambda function for getting relationships and attributes.
     *
     * @param relationshipSupplier The relationship supplier (getRelationships())
     * @param attributeSupplier    The attribute supplier
     * @return The Resource
     */
    private Resource toResource(final Supplier<Map<String, Relationship>> relationshipSupplier,
                                final Supplier<Map<String, Object>> attributeSupplier) {
        return toResource(relationshipSupplier.get(), attributeSupplier.get());
    }

    /**
     * Convert a persistent resource to a resource.
     *
     * @param relationships The relationships
     * @param attributes    The attributes
     * @return The Resource
     */
    public Resource toResource(final Map<String, Relationship> relationships,
                               final Map<String, Object> attributes) {
        final Resource resource = new Resource(typeName, (obj == null)
                ? uuid.orElseThrow(
                () -> new InvalidEntityBodyException("No id found on object"))
                : dictionary.getId(obj));
        resource.setRelationships(relationships);
        resource.setAttributes(attributes);
        if (requestScope.getElideSettings().isEnableJsonLinks()) {
            resource.setLinks(requestScope.getElideSettings().getJsonApiLinks().getResourceLevelLinks(this));
        }

        if (! (getObject() instanceof WithMetadata)) {
            return resource;
        }

        WithMetadata withMetadata = (WithMetadata) getObject();
        Set<String> fields = withMetadata.getMetadataFields();

        if (fields.size() == 0) {
            return resource;
        }

        Meta meta = new Meta(new HashMap<>());

        for (String field : fields) {
            meta.getMetaMap().put(field, withMetadata.getMetadataField(field).get());
        }

        resource.setMeta(meta);

        return resource;
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationships() {
        return getRelationshipsWithRelationshipFunction((relationName) -> {
            Optional<FilterExpression> filterExpression = requestScope.getExpressionForRelation(getResourceType(),
                    relationName);

            return getRelationCheckedFiltered(com.yahoo.elide.core.request.Relationship.builder()
                    .alias(relationName)
                    .name(relationName)
                    .projection(EntityProjection.builder()
                            .type(dictionary.getParameterizedType(getResourceType(), relationName))
                            .filterExpression(filterExpression.orElse(null))
                            .build())
                    .build());
        });
    }

    /**
     * Get relationship mappings.
     *
     * @return Relationship mapping
     */
    private Map<String, Relationship> getRelationships(EntityProjection projection) {
        return getRelationshipsWithRelationshipFunction(
                (relationName) -> getRelationCheckedFiltered(projection.getRelationship(relationName)
                        .orElseThrow(IllegalStateException::new)
                ));
    }

    /**
     * Get relationship mappings.
     *
     * @param relationshipFunction a function to load the value of a relationship. Takes a string of the relationship
     *                             name and returns the relationship's value.
     * @return Relationship mapping
     */
    protected Map<String, Relationship> getRelationshipsWithRelationshipFunction(
            final Function<String, Observable<PersistentResource>> relationshipFunction) {
        final Map<String, Relationship> relationshipMap = new LinkedHashMap<>();
        final Set<String> relationshipFields = filterFields(dictionary.getRelationships(obj));

        for (String field : relationshipFields) {
            TreeMap<String, Resource> orderedById = new TreeMap<>(lengthFirstComparator);
            for (PersistentResource relationship : relationshipFunction.apply(field).toList().blockingGet()) {
                orderedById.put(relationship.getId(),
                        new ResourceIdentifier(relationship.getTypeName(), relationship.getId()).castToResource());

            }
            Observable<Resource> resources = Observable.fromIterable(orderedById.values());

            Data<Resource> data;
            RelationshipType relationshipType = getRelationshipType(field);
            if (relationshipType.isToOne()) {
                data = new Data<>(firstOrNullIfEmpty(resources));
            } else {
                data = new Data<>(resources);
            }
            Map<String, String> links = null;
            if (requestScope.getElideSettings().isEnableJsonLinks()) {
                links = requestScope.getElideSettings()
                        .getJsonApiLinks()
                        .getRelationshipLinks(this, field);
            }
            relationshipMap.put(field, new Relationship(links, data));
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
     *
     * @param fieldName the field name
     * @param newValue  the new value
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
     *
     * @param fieldName the field name to set or invoke equivalent set method
     * @param oldValue  the old value
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
     *
     * @param attribute the attribute to fetch.
     * @return value value
     */
    protected Object getValueChecked(Attribute attribute) {
        checkFieldAwareDeferPermissions(ReadPermission.class, attribute.getName(), null, null);
        return transaction.getAttribute(getObject(), attribute, requestScope);
    }

    /**
     * Retrieve an object without checking read permissions (i.e. value is used internally and not sent to others)
     *
     * @param fieldName the field name
     * @return Value
     */
    protected Object getValueUnchecked(String fieldName) {
        return getValue(getObject(), fieldName, requestScope);
    }

    protected boolean modifyCollection(
            Collection toModify,
            String collectionName,
            Collection toAdd,
            Collection toRemove,
            boolean updateInverse) {

        Collection copyOfOriginal = copyCollection(toModify);

        Collection modified = CollectionUtils.union(CollectionUtils.emptyIfNull(toModify), toAdd);
        modified = CollectionUtils.subtract(modified, toRemove);

        checkFieldAwareDeferPermissions(
                UpdatePermission.class,
                collectionName,
                modified,
                copyOfOriginal);

        if (updateInverse) {
            for (Object adding : toAdd) {
                addInverseRelation(collectionName, adding);
            }

            for (Object removing : toRemove) {
                deleteInverseRelation(collectionName, removing);
            }
        }

        if (toModify == null) {
            this.setValueChecked(collectionName, modified);
            return true;
        } else {
            if (copyOfOriginal.equals(modified)) {
                return false;
            }
            toModify.addAll(toAdd);
            toModify.removeAll(toRemove);

            triggerUpdate(collectionName, copyOfOriginal, modified);
            return true;
        }
    }

    /**
     * Invoke the set[fieldName] method on the target object OR set the field with the corresponding name.
     *
     * @param fieldName the field name to set or invoke equivalent set method
     * @param value     the value to set
     */
    protected void setValue(String fieldName, Object value) {
        final Object original = getValueUnchecked(fieldName);

        dictionary.setValue(obj, fieldName, value);

        triggerUpdate(fieldName, original, value);
    }

    /**
     * If a bidirectional relationship exists, attempts to delete itself from the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     *
     * @param relationName  The name of the relationship on this (A) object.
     * @param inverseEntity The value (B) which has been deleted from this object.
     */
    protected void deleteInverseRelation(String relationName, Object inverseEntity) {
        String inverseField = getInverseRelationField(relationName);

        if (!"".equals(inverseField)) {
            Type<?> inverseType = dictionary.getType(inverseEntity, inverseField);

            String uuid = requestScope.getUUIDFor(inverseEntity);
            PersistentResource inverseResource = new PersistentResource(inverseEntity,
                    this, relationName, uuid, requestScope);
            Object inverseRelation = inverseResource.getValueUnchecked(inverseField);

            if (inverseRelation == null) {
                return;
            }

            if (inverseRelation instanceof Collection) {
                inverseResource.modifyCollection((Collection) inverseRelation, inverseField,
                        Collections.emptySet(), Set.of(this.getObject()), false);
            } else if (inverseType.isAssignableFrom(this.getResourceType())) {
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
        return dictionary.getRelationInverse(type, relationName);
    }

    /**
     * If a bidirectional relationship exists, attempts to add itself to the inverse
     * relationship. Given A to B as the relationship, A corresponds to this and B is the inverse.
     *
     * @param relationName The name of the relationship on this (A) object.
     * @param inverseObj   The value (B) which has been added to this object.
     */
    protected void addInverseRelation(String relationName, Object inverseObj) {
        String inverseName = dictionary.getRelationInverse(type, relationName);

        if (!"".equals(inverseName)) {
            Type<?> inverseType = dictionary.getType(inverseObj, inverseName);

            String uuid = requestScope.getUUIDFor(inverseObj);
            PersistentResource inverseResource = new PersistentResource(inverseObj,
                    this, relationName, uuid, requestScope);
            Object inverseRelation = inverseResource.getValueUnchecked(inverseName);

            if (COLLECTION_TYPE.isAssignableFrom(inverseType)) {
                if (inverseRelation != null) {
                    inverseResource.modifyCollection((Collection) inverseRelation, inverseName,
                            Set.of(this.getObject()), Collections.emptySet(), false);
                } else {
                    inverseResource.setValueChecked(inverseName, Collections.singleton(this.getObject()));
                }
            } else if (inverseType.isAssignableFrom(this.getResourceType())) {
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
     * Filter a set of fields.
     *
     * @param fields the fields
     * @return Filtered set of fields
     */
    protected Set<String> filterFields(Collection<String> fields) {
        Set<String> filteredSet = new LinkedHashSet<>();
        for (String field : fields) {
            try {

                if (checkIncludeSparseField(requestScope.getSparseFields(), typeName, field)) {
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
        LifeCycleHookBinding.Operation action = isNewlyCreated()
                ? CREATE
                : UPDATE;

        requestScope.publishLifecycleEvent(this, fieldName, action, Optional.of(changeSpec));
        requestScope.publishLifecycleEvent(this, action);
        auditField(new ChangeSpec(this, fieldName, original, value));
    }

    private <A extends Annotation> ExpressionResult checkFieldAwarePermissions(
            Class<A> annotationClass,
            Set<String> requestedFields
    ) {
        return requestScope.getPermissionExecutor().checkPermission(annotationClass, this, requestedFields);
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

    /**
     * Audit an action on field.
     *
     * @param changeSpec Change spec for audit
     */
    protected void auditField(final ChangeSpec changeSpec) {
        final String fieldName = changeSpec.getFieldName();
        Audit[] annotations = dictionary.getAttributeOrRelationAnnotations(getResourceType(),
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
                LogMessage message = new LogMessageImpl(annotation, this, Optional.of(changeSpec));
                getRequestScope().getAuditLogger().log(message);
            } else {
                throw new InvalidSyntaxException("Only Audit.Action.UPDATE is allowed on fields.");
            }
        }
    }

    /**
     * Audit an action on an entity.
     *
     * @param action     the action
     * @param changeSpec the change that occurred
     */
    protected void auditClass(Audit.Action action, ChangeSpec changeSpec) {
        Audit[] annotations = getResourceType().getAnnotationsByType(Audit.class);

        if (annotations == null) {
            return;
        }
        for (Audit annotation : annotations) {
            for (Audit.Action auditAction : annotation.action()) {
                if (auditAction == action) { // compare object reference
                    LogMessage message = new LogMessageImpl(annotation, this, Optional.ofNullable(changeSpec));
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
}
