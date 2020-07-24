/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidObjectIdentifierException;
import com.yahoo.elide.core.exceptions.InvalidValueException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.containers.ConnectionContainer;
import com.yahoo.elide.graphql.containers.MapEntryContainer;

import com.google.common.collect.Sets;

import org.apache.commons.collections4.IterableUtils;

import graphql.language.Field;
import graphql.language.FragmentSpread;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;

/**
 * Invoked by GraphQL Java to fetch/mutate data from Elide.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher<Object> {
    private final ElideSettings settings;

    @Getter
    private final NonEntityDictionary nonEntityDictionary;

    public PersistentResourceFetcher(ElideSettings settings, NonEntityDictionary nonEntityDictionary) {
        this.settings = settings;
        this.nonEntityDictionary = nonEntityDictionary;
    }

    /**
     * Override graphql-java's {@link DataFetcher} get method to execute
     * the mutation and return some sensible output values.
     * @param environment Graphql-java's execution environment
     * @return a collection of {@link PersistentResource} objects
     */
    @Override
    public Object get(DataFetchingEnvironment environment) {
        /* fetch arguments in mutation/query */
        Map<String, Object> args = environment.getArguments();

        /* fetch current operation */
        RelationshipOp operation = (RelationshipOp) args.getOrDefault(ARGUMENT_OPERATION, RelationshipOp.FETCH);

        /* build environment object, extracts required fields */
        Environment context = new Environment(environment);

        /* safe enable debugging */
        if (log.isDebugEnabled()) {
            logContext(operation, context);
        }

        /* sanity check for pagination/filtering/sorting arguments w any operation other than FETCH */
        if (operation != RelationshipOp.FETCH) {
            filterSortPaginateSanityCheck(context);
        }

        /* delegate request */
        switch (operation) {
            case FETCH:
                return fetchObjects(context);

            case UPSERT:
                return upsertObjects(context);

            case UPDATE:
                return updateObjects(context);

            case DELETE:
                return deleteObjects(context);

            case REMOVE:
                return removeObjects(context);

            case REPLACE:
                return replaceObjects(context);

            default:
                throw new UnsupportedOperationException("Unknown operation: " + operation);
        }
    }

    /**
     * Checks whether sort/filter/pagination params are passed w unsupported operation
     * @param environment Environment encapsulating graphQL's request environment
     */
    private void filterSortPaginateSanityCheck(Environment environment) {
        if (environment.filters.isPresent() || environment.sort.isPresent() || environment.offset.isPresent()
                || environment.first.isPresent()) {
            throw new BadRequestException("Pagination/Filtering/Sorting is only supported with FETCH operation");
        }
    }

    /**
     * log current context for debugging
     * @param operation Current operation
     * @param environment Environment encapsulating graphQL's request environment
     */
    private void logContext(RelationshipOp operation, Environment environment) {
        List<?> children = (environment.field.getSelectionSet() != null)
                ? (List) environment.field.getSelectionSet().getChildren()
                : new ArrayList<>();

        List<String> fieldName = new ArrayList<String>();
        if (children.size() > 0) {
            children.stream().forEach(i -> { if (i.getClass().equals(Field.class)) {
                    fieldName.add(((Field) i).getName());
                } else if (i.getClass().equals(FragmentSpread.class)) {
                    fieldName.add(((FragmentSpread) i).getName());
                } else {
                    log.debug("A new type of Selection, other than Field and FragmentSpread was encountered, {}",
                            i.getClass());
                }
            });
        }

        String requestedFields = environment.field.getName() + fieldName.toString();

        GraphQLType parent = environment.parentType;
        if (log.isDebugEnabled()) {
            log.debug("{} {} fields with parent {}<{}>",
                    operation, requestedFields, EntityDictionary.getSimpleName(parent.getClass()), parent.getName());
        }
    }

    /**
     * handle FETCH operation
     * @param context Environment encapsulating graphQL's request environment
     * @return list of {@link PersistentResource} objects
     */
    private Object fetchObjects(Environment context) {
        /* sanity check for data argument w FETCH */
        if (context.data.isPresent()) {
            throw new BadRequestException("FETCH must not include data");
        }

        // Process fetch object for this container
        return context.container.processFetch(context, this);
    }

    /**
     * Fetches a root-level entity.
     * @param context Context for request
     * @param requestScope Request scope
     * @param entityClass Entity class
     * @param ids List of ids (can be NULL)
     * @param sort Sort by ASC/DESC
     * @param offset Pagination offset argument
     * @param first Pagination first argument
     * @param filters Filter params
     * @param generateTotals True if page totals should be generated for this type, false otherwise
     * @return {@link PersistentResource} object(s)
     */
    public ConnectionContainer fetchObject(Environment context, RequestScope requestScope, Class<?> entityClass,
                                                Optional<List<String>> ids, Optional<String> sort,
                                                Optional<String> offset, Optional<String> first,
                                                Optional<String> filters, boolean generateTotals) {
        EntityDictionary dictionary = requestScope.getDictionary();
        String typeName = dictionary.getJsonAliasFor(entityClass);

        Optional<Pagination> pagination = buildPagination(first, offset, generateTotals);
        Optional<Sorting> sorting = buildSorting(sort);
        Optional<FilterExpression> filter = buildFilter(typeName, filters, requestScope);

        /* fetching a collection */
        Set<PersistentResource> records = ids.map((idList) -> {
            /* handle empty list of ids */
            if (idList.isEmpty()) {
                throw new BadRequestException("Empty list passed to ids");
            }

            return PersistentResource.loadRecords(entityClass, idList,
                    filter, sorting, pagination, requestScope);
        }).orElseGet(() -> PersistentResource.loadRecords(
                entityClass, /* Empty list of IDs */ new ArrayList<>(), filter, sorting, pagination, requestScope
        ));

        return new ConnectionContainer(records, pagination, typeName);
    }

    /**
     * Fetches a relationship for a top-level entity.
     *
     * @param context Request context
     * @param parentResource Parent object
     * @param fieldName Field type
     * @param ids List of ids
     * @param offset Pagination offset
     * @param first Pagination first
     * @param filters Filter string
     * @param generateTotals True if page totals should be generated for this type, false otherwise
     * @return persistence resource object(s)
     */
    public Object fetchRelationship(Environment context,
                                     PersistentResource<?> parentResource,
                                     String fieldName,
                                     Optional<List<String>> ids,
                                     Optional<String> offset,
                                     Optional<String> first,
                                     Optional<String> sort,
                                     Optional<String> filters,
                                     boolean generateTotals) {
        EntityDictionary dictionary = parentResource.getRequestScope().getDictionary();
        Class<?> entityClass = dictionary.getParameterizedType(parentResource.getObject(), fieldName);
        String typeName = dictionary.getJsonAliasFor(entityClass);

        Optional<Pagination> pagination = buildPagination(first, offset, generateTotals);
        Optional<Sorting> sorting = buildSorting(sort);
        Optional<FilterExpression> filter = buildFilter(typeName, filters, parentResource.getRequestScope());

        Set<PersistentResource> relations;
        if (ids.isPresent()) {
            relations = parentResource.getRelation(fieldName, ids.get(), filter, sorting, pagination);
        } else {
            relations = parentResource.getRelationCheckedFiltered(fieldName,
                    filter, sorting, pagination);
        }

        return new ConnectionContainer(relations, pagination, typeName);
    }

    private ConnectionContainer upsertObjects(Environment context) {
        return upsertOrUpdateObjects(
                context,
                (entityObject) -> upsertObject(context, entityObject),
                RelationshipOp.UPSERT);
    }

    private ConnectionContainer updateObjects(Environment context) {
        return upsertOrUpdateObjects(
                context,
                (entityObject) -> updateObject(context, entityObject),
                RelationshipOp.UPDATE);
    }

    /**
     * handle UPSERT or UPDATE operation
     * @param context Environment encapsulating graphQL's request environment
     * @param updateFunc controls the behavior of how the update (or upsert) is performed.
     * @return Connection object.
     */
    private ConnectionContainer upsertOrUpdateObjects(Environment context,
                                                      Executor<?> updateFunc,
                                                      RelationshipOp operation) {
        /* sanity check for id and data argument w UPSERT/UPDATE */
        if (context.ids.isPresent()) {
            throw new BadRequestException(operation + " must not include ids");
        }

        if (!context.data.isPresent()) {
            throw new BadRequestException(operation + " must include data argument");
        }

        Class<?> entityClass;
        EntityDictionary dictionary = context.requestScope.getDictionary();
        if (context.isRoot()) {
            entityClass = dictionary.getEntityClass(context.field.getName());
        } else {
            entityClass = dictionary.getParameterizedType(context.parentResource.getResourceClass(),
                    context.field.getName());
        }

        /* form entities */
        Optional<Entity> parentEntity;
        if (!context.isRoot()) {
            parentEntity = Optional.of(new Entity(Optional.empty(),
                    null,
                    context.parentResource.getResourceClass(),
                    context.requestScope));
        } else {
            parentEntity = Optional.empty();
        }
        LinkedHashSet<Entity> entitySet = new LinkedHashSet<>();
        for (Map<String, Object> input : context.data.get()) {
            entitySet.add(new Entity(parentEntity, input, entityClass, context.requestScope));
        }

        /* apply function to upsert/update the object */
        for (Entity entity : entitySet) {
            graphWalker(entity, updateFunc);
        }

        /* fixup relationships */
        for (Entity entity : entitySet) {
            graphWalker(entity, this::updateRelationship);
            PersistentResource<?> childResource = entity.toPersistentResource();
            if (!context.isRoot()) {
                /* add relation between parent and nested entity */
                context.parentResource.addRelation(context.field.getName(), childResource);
            }
        }

        String entityName = dictionary.getJsonAliasFor(entityClass);

        Set<PersistentResource> resources = entitySet.stream()
                .map(Entity::toPersistentResource)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new ConnectionContainer(resources, Optional.empty(), entityName);
    }

    /**
     * A function to handle upserting (update/create) objects.
     */
    @FunctionalInterface
    private interface Executor<T> {
        T execute(Entity entity);
    }

    /**
     * Forms the graph from data {@param input} and executes a function {@param function} on all the nodes
     * @param entity Resource entity
     * @param function Function to process nodes
     * @return set of {@link PersistentResource} objects
     */
    private void graphWalker(Entity entity, Executor<?> function) {
        Queue<Entity> toVisit = new ArrayDeque<>();
        Set<Entity> visited = new LinkedHashSet<>();
        toVisit.add(entity);

        while (!toVisit.isEmpty()) {
            Entity currentEntity = toVisit.remove();
            if (visited.contains(currentEntity)) {
                continue;
            }
            visited.add(currentEntity);
            function.execute(currentEntity);
            Set<Entity.Relationship> relationshipEntities = currentEntity.getRelationships();
            /* loop over relationships */
            for (Entity.Relationship relationship : relationshipEntities) {
                toVisit.addAll(relationship.getValue());
            }
        }
    }

    /**
     * update the relationship between {@param parent} and the resource loaded by given {@param id}
     * @param entity Resource entity
     * @return {@link PersistentResource} object
     */
    private PersistentResource<?> updateRelationship(Entity entity) {
        Set<Entity.Relationship> relationshipEntities = entity.getRelationships();
        PersistentResource<?> resource = entity.toPersistentResource();
        Set<PersistentResource> toUpdate;

        /* loop over each relationship */
        for (Entity.Relationship relationship : relationshipEntities) {
            toUpdate = new LinkedHashSet<>();
            for (Entity relation : relationship.getValue()) {
                toUpdate.add(relation.toPersistentResource());
            }
            resource.updateRelation(relationship.getName(), toUpdate);
        }
        return resource;
    }

    /**
     * updates or creates existing/new entities
     * @param context request context
     * @param entity Resource entity
     * @return {@link PersistentResource} object
     */
    private PersistentResource<?> upsertObject(Environment context, Entity entity) {
        Set<Entity.Attribute> attributes = entity.getAttributes();
        Optional<String> id = entity.getId();
        RequestScope requestScope = entity.getRequestScope();
        EntityDictionary dictionary = requestScope.getDictionary();

        PersistentResource<?> upsertedResource;
        PersistentResource<?> parentResource;
        if (!entity.getParentResource().isPresent()) {
            parentResource = null;
        } else {
            parentResource = entity.getParentResource().get().toPersistentResource();
        }

        if (!id.isPresent()) {

            //If the ID is generated, it is safe to assign a temporary UUID.  Otherwise the client must provide one.
            if (dictionary.isIdGenerated(entity.getEntityClass())) {
                entity.setId(); //Assign a temporary UUID.
                id = entity.getId();
            }

            upsertedResource = PersistentResource.createObject(parentResource,
                    entity.getEntityClass(),
                    requestScope,
                    id);
        } else {
            try {
                Set<PersistentResource> loadedResource = fetchObject(context, requestScope, entity.getEntityClass(),
                        Optional.of(Arrays.asList(id.get())),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        false).getPersistentResources();
                upsertedResource = IterableUtils.first(loadedResource);

            //The ID doesn't exist yet.  Let's create the object.
            } catch (InvalidObjectIdentifierException | InvalidValueException e) {
                upsertedResource = PersistentResource.createObject(parentResource,
                        entity.getEntityClass(),
                        requestScope,
                        id);
            }
        }

        return updateAttributes(upsertedResource, entity, attributes);
    }

    private PersistentResource<?> updateObject(Environment context, Entity entity) {
        Set<Entity.Attribute> attributes = entity.getAttributes();
        Optional<String> id = entity.getId();
        RequestScope requestScope = entity.getRequestScope();
        PersistentResource<?> updatedResource;

        if (!id.isPresent()) {
            throw new BadRequestException("UPDATE data objects must include ids");
        }
        Set<PersistentResource> loadedResource = fetchObject(context, requestScope, entity.getEntityClass(),
            Optional.of(Arrays.asList(id.get())),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false).getPersistentResources();
        updatedResource = IterableUtils.first(loadedResource);

        return updateAttributes(updatedResource, entity, attributes);
    }

    /**
     * Updates an object
     * @param toUpdate Entities to update
     * @param entity Resource entity
     * @param attributes Set of entity attributes
     * @return Persistence Resource object
     */
    private PersistentResource<?> updateAttributes(PersistentResource<?> toUpdate,
                                                Entity entity,
                                                Set<Entity.Attribute> attributes) {
        EntityDictionary dictionary = entity.getRequestScope().getDictionary();
        Class<?> entityClass = entity.getEntityClass();
        String idFieldName = dictionary.getIdFieldName(entityClass);

        /* iterate through each attribute provided */
        for (Entity.Attribute attribute : attributes) {
            if (dictionary.isAttribute(entityClass, attribute.getName())) {
                Class<?> attributeType = dictionary.getType(entityClass, attribute.getName());
                Object attributeValue;
                if (Map.class.isAssignableFrom(attributeType)) {
                    attributeValue = MapEntryContainer.translateFromGraphQLMap(attribute);
                } else {
                    attributeValue = attribute.getValue();
                }
                toUpdate.updateAttribute(attribute.getName(), attributeValue);
            } else if (!Objects.equals(attribute.getName(), idFieldName)) {
                throw new IllegalStateException("Unrecognized attribute passed to 'data': " + attribute.getName());
            }
        }

        return toUpdate;
    }

    /**
     * Deletes a resource
     * @param context Environment encapsulating graphQL's request environment
     * @return set of deleted {@link PersistentResource} object(s)
     */
    private Object deleteObjects(Environment context) {
        /* sanity check for id and data argument w DELETE */
        if (context.data.isPresent()) {
            throw new BadRequestException("DELETE must not include data argument");
        }

        if (!context.ids.isPresent()) {
            throw new BadRequestException("DELETE must include ids argument");
        }

        ConnectionContainer connection = (ConnectionContainer) fetchObjects(context);
        Set<PersistentResource> toDelete = connection.getPersistentResources();
        toDelete.forEach(PersistentResource::deleteResource);

        return new ConnectionContainer(
                Collections.emptySet(),
                Optional.empty(),
                connection.getTypeName()
        );
    }

    /**
     * Removes a relationship, or deletes a root level resource
     * @param context Environment encapsulating graphQL's request environment
     * @return set of removed {@link PersistentResource} object(s)
     */
    private Object removeObjects(Environment context) {
        /* sanity check for id and data argument w REPLACE */
        if (context.data.isPresent()) {
            throw new BadRequestException("REPLACE must not include data argument");
        }

        if (!context.ids.isPresent()) {
            throw new BadRequestException("REPLACE must include ids argument");
        }


        ConnectionContainer connection = (ConnectionContainer) fetchObjects(context);
        Set<PersistentResource> toRemove = connection.getPersistentResources();
        if (!context.isRoot()) { /* has parent */
            toRemove.forEach(item -> context.parentResource.removeRelation(context.field.getName(), item));
        } else { /* is root */
            toRemove.forEach(PersistentResource::deleteResource);
        }

        return new ConnectionContainer(
                Collections.emptySet(),
                Optional.empty(),
                connection.getTypeName()
        );
    }

    /**
     * Replaces a resource, updates given resource and deletes the rest
     * belonging to the the same type/relationship family.
     * @param context Environment encapsulating graphQL's request environment
     * @return set of replaced {@link PersistentResource} object(s)
     */
    private ConnectionContainer replaceObjects(Environment context) {
        /* sanity check for id and data argument w REPLACE */
        if (!context.data.isPresent()) {
            throw new BadRequestException("REPLACE must include data argument");
        }

        if (context.ids.isPresent()) {
            throw new BadRequestException("REPLACE must not include ids argument");
        }

        ConnectionContainer existingObjects =
                (ConnectionContainer) context.container.processFetch(context, this);
        ConnectionContainer upsertedObjects = upsertObjects(context);
        Set<PersistentResource> toDelete =
                Sets.difference(existingObjects.getPersistentResources(), upsertedObjects.getPersistentResources());

        if (!context.isRoot()) { /* has parent */
            toDelete.forEach(item -> context.parentResource.removeRelation(context.field.getName(), item));
        } else { /* is root */
            toDelete.forEach(PersistentResource::deleteResource);
        }
        return upsertedObjects;
    }

    private Optional<Pagination> buildPagination(Optional<String> first,
                                                 Optional<String> offset,
                                                 boolean generateTotals) {
        return Pagination.fromOffsetAndFirst(first, offset, generateTotals, settings);
    }

    private Optional<Sorting> buildSorting(Optional<String> sort) {
        return sort.map(Sorting::parseSortRule);
    }

    private MultivaluedHashMap<String, String> getQueryParams(Optional<String> typeName, String filterStr) {
        return new MultivaluedHashMap<String, String>() {
            {
                String filterKey = "filter";
                if (typeName.isPresent()) {
                    filterKey += "[" + typeName + "]";
                }
                put(filterKey, Arrays.asList(filterStr));
            }
        };
    }

    private Optional<FilterExpression> buildFilter(String typeName,
                                                   Optional<String> filter,
                                                   RequestScope requestScope) {
        // TODO: Refactor FilterDialect interfaces to accept string or List<String> instead of (or in addition to?)
        // query params.
        return filter.map(filterStr -> {
            String errorMessage = "";
            try {
                return requestScope.getFilterDialect()
                        .parseGlobalExpression(typeName, getQueryParams(Optional.empty(), filterStr));
            } catch (ParseException e) {
                errorMessage = e.getMessage();
            }

            try {
                return requestScope.getFilterDialect()
                        .parseTypedExpression(typeName, getQueryParams(Optional.of(typeName), filterStr))
                        .get(typeName);
            } catch (ParseException e) {
                throw new BadRequestException(errorMessage + "\n" + e.getMessage());
            }
        });
    }
}
