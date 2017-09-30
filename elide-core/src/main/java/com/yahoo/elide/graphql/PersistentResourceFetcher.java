/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.google.common.collect.Sets;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidPredicateException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.expression.AndFilterExpression;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.pagination.Pagination;
import com.yahoo.elide.core.sort.Sorting;
import com.yahoo.elide.graphql.containers.ConnectionContainer;
import com.yahoo.elide.graphql.containers.EdgesContainer;
import com.yahoo.elide.graphql.containers.NodeContainer;
import com.yahoo.elide.graphql.containers.PageInfoContainer;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;

/**
 * Invoked by GraphQL Java to fetch/mutate data from Elide.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {
    private final ElideSettings settings;

    private static final String EDGES_KEYWORD = "edges";
    private static final String NODE_KEYWORD = "node";

    // Page info keywords
    private static final String PAGE_INFO_KEYWORD = "pageInfo";
    private static final String PAGE_INFO_HAS_NEXT_PAGE_KEYWORD = "hasNextPage";
    private static final String PAGE_INFO_START_CURSOR_KEYWORD = "startCursor";
    private static final String PAGE_INFO_END_CURSOR_KEYWORD = "endCursor";
    private static final String PAGE_INFO_TOTAL_RECORDS_KEYWORD = "totalRecords";

    private Optional<Pagination> lastPaginationObject = Optional.empty();

    PersistentResourceFetcher(ElideSettings settings) {
        this.settings = settings;
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
        List<Field> children = (environment.field.getSelectionSet() != null)
                ? (List) environment.field.getSelectionSet().getChildren()
                : new ArrayList<>();
        String requestedFields = environment.field.getName() + (children.size() > 0
                ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")" : "");
        GraphQLType parent = environment.parentType;
        log.debug("{} {} fields with parent {}<{}>",
            operation, requestedFields, parent.getClass().getSimpleName(), parent.getName());
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

        /* check whether current object has a parent or not */
        if (context.isRoot()) {
            return handleRootQuery(context);
        } else { /* fetch attribute or relationship */
            if (context.rawSource instanceof ConnectionContainer) {
                return handleConnectionQuery(context);
            }
            if (context.rawSource instanceof EdgesContainer) {
                return handleEdgesQuery(context);
            }
            if (context.rawSource instanceof PageInfoContainer) {
                return handlePageInfoQuery(context);
            }
            if (context.rawSource instanceof NodeContainer) {
                return handleNodeQuery(context);
            }
            Class parentClass = context.parentResource.getResourceClass();
            String fieldName = context.field.getName();
            throw new BadRequestException("Unrecognized object: " + fieldName + " for: " + parentClass.getName());
        }
    }

    private Object handleRootQuery(Environment context) {
        EntityDictionary dictionary = context.requestScope.getDictionary();
        Class<?> entityClass = dictionary.getEntityClass(context.field.getName());
        String entityType = dictionary.getJsonAliasFor(entityClass);
        boolean generateTotals = requestContainsPageInfo(entityType, context.field);
        Set<PersistentResource> resources = fetchObject(context, context.requestScope, entityClass, context.ids,
                context.sort, context.offset, context.first, context.filters, generateTotals);
        return new ConnectionContainer(resources, lastPaginationObject, entityType);
    }

    private Object handleConnectionQuery(Environment context) {
        String fieldName = context.field.getName();
        ConnectionContainer container = (ConnectionContainer) context.rawSource;

        switch (fieldName) {
            case EDGES_KEYWORD:
                return container.getPersistentResources().stream()
                        .map(EdgesContainer::new)
                        .collect(Collectors.toList());
            case PAGE_INFO_KEYWORD:
                return new PageInfoContainer(container);
            default:
                break;
        }

        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in a connection object.");
    }

    private Object handleEdgesQuery(Environment context) {
        String fieldName = context.field.getName();

        if (NODE_KEYWORD.equals(fieldName)) {
            return new NodeContainer(context.parentResource);
        }
        throw new BadRequestException("Invalid request. Looking for field: " + fieldName + " in an edges object.");
    }

    private Object handlePageInfoQuery(Environment context) {
        String fieldName = context.field.getName();
        PageInfoContainer container = (PageInfoContainer) context.rawSource;
        ConnectionContainer connectionContainer = container.getConnectionContainer();
        Optional<Pagination> pagination = connectionContainer.getPagination();

        List<String> ids = connectionContainer.getPersistentResources().stream()
                .map(PersistentResource::getId)
                .sorted()
                .collect(Collectors.toList());

        return pagination.map(pageValue -> {
            switch (fieldName) {
                case PAGE_INFO_HAS_NEXT_PAGE_KEYWORD: {
                    int numResults = ids.size();
                    int nextOffset = numResults + pageValue.getOffset();
                    return nextOffset < pageValue.getPageTotals();
                }
                case PAGE_INFO_START_CURSOR_KEYWORD:
                    return pageValue.getOffset();
                case PAGE_INFO_END_CURSOR_KEYWORD:
                    return pageValue.getOffset() + ids.size();
                case PAGE_INFO_TOTAL_RECORDS_KEYWORD:
                    return pageValue.getPageTotals();
                default:
                    break;
            }
            throw new BadRequestException("Invalid request. Looking for field: "
                    + fieldName + " in an pageInfo object.");
        }).orElseThrow(() -> new BadRequestException("Could not generate pagination information for type: "
                + connectionContainer.getTypeName()));
    }

    private Object handleNodeQuery(Environment context) {
        EntityDictionary dictionary = context.requestScope.getDictionary();
        Class parentClass = context.parentResource.getResourceClass();
        String fieldName = context.field.getName();
        String idFieldName = dictionary.getIdFieldName(parentClass);

        if (dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return context.parentResource.getAttribute(fieldName);
        }
        if (dictionary.isRelation(parentClass, fieldName)) { /* fetch relationship properties */
            String entityType = dictionary.getJsonAliasFor(dictionary.getParameterizedType(parentClass, fieldName));
            boolean generateTotals = requestContainsPageInfo(entityType, context.field);
            return fetchRelationship(context, context.parentResource,
                    fieldName, context.ids, context.offset, context.first, context.sort, context.filters,
                    generateTotals);
        }
        if (Objects.equals(idFieldName, fieldName)) {
            return new DeferredId(context.parentResource);
        }
        throw new BadRequestException("Unrecognized object: " + fieldName + " for: "
                + parentClass.getName() + " in node");
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
    private Set<PersistentResource> fetchObject(Environment context, RequestScope requestScope, Class entityClass,
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

            /* access records from internal db and return */
            Class<?> idType = dictionary.getIdType(entityClass);
            String idField = dictionary.getIdFieldName(entityClass);

            /* construct a new SQL like filter expression, eg: book.id IN [1,2] */
            FilterExpression idFilter = new FilterPredicate(
                    new Path.PathElement(
                            entityClass,
                            idType,
                            idField),
                    Operator.IN,
                    new ArrayList<>(idList));
            FilterExpression filterExpression = filter
                    .map(fe -> (FilterExpression) new AndFilterExpression(idFilter, fe))
                    .orElse(idFilter);

             return PersistentResource.loadRecords(entityClass,
                    Optional.of(filterExpression), sorting, pagination, requestScope);
        }).orElseGet(() ->
            PersistentResource.loadRecords(entityClass,
                    filter,
                    sorting,
                    pagination,
                    requestScope)
        );

        // I don't like doing this... We should rearrange and clean up later, but we want to move quickly on this
        // if possible.
        lastPaginationObject = pagination;

        return records;
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
    private Object fetchRelationship(Environment context,
                                     PersistentResource parentResource,
                                     String fieldName,
                                     Optional<List<String>> ids,
                                     Optional<String> offset,
                                     Optional<String> first,
                                     Optional<String> sort,
                                     Optional<String> filters,
                                     boolean generateTotals) {
        EntityDictionary dictionary = parentResource.getRequestScope().getDictionary();
        Class entityClass = dictionary.getParameterizedType(parentResource.getObject(), fieldName);
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

        /* check for toOne relationships */
        Boolean isToOne = parentResource.getRelationshipType(fieldName).isToOne();

        // Need to clean this up. This mutation feels brittle.
        lastPaginationObject = pagination;

        if (isToOne) {
            return relations.iterator().next();
        } else {
            return relations;
        }
    }

    /**
     * handle UPSERT operation
     * @param context Environment encapsulating graphQL's request environment
     * @return list of {@link PersistentResource} objects
     */
    private Set<PersistentResource> upsertObjects(Environment context) {
        /* sanity check for id and data argument w UPSERT */
        if (context.ids.isPresent()) {
            throw new BadRequestException("UPSERT must not include ids");
        }

        if (!context.data.isPresent()) {
            throw new BadRequestException("UPSERT must include data argument");
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

        /* upsert */
        for (Entity entity : entitySet) {
            graphWalker(entity, (e) -> upsertObject(context, e));
        }

        /* fixup relationships */
        for (Entity entity : entitySet) {
            graphWalker(entity, this::updateRelationship);
            if (!context.isRoot()) { /* add relation between parent and nested entity */
                context.parentResource.addRelation(context.field.getName(), entity.toPersistentResource());
            }
        }

        return entitySet.stream()
                .map(Entity::toPersistentResource)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * A function to handle upserting (update/create) objects.
     */
    @FunctionalInterface
    private interface Executor<T> {
        T execute(Entity entity);
    }

    /**
     * Forms the graph from data {@param input} and upserts {@param function} all the nodes
     * @param entity Resource entity
     * @param function Function to process nodes
     * @return set of {@link PersistentResource} objects
     */
    private void graphWalker(Entity entity, Executor function) {
        Queue<Entity> toVisit = new ArrayDeque();
        Set<Entity> visited = new LinkedHashSet<>();
        toVisit.add(entity);

        while (!toVisit.isEmpty()) {
            Entity currentEntity = toVisit.remove();
            if (visited.contains(currentEntity)) {
                continue;
            } else {
                visited.add(currentEntity);
            }
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
    private PersistentResource updateRelationship(Entity entity) {
        Set<Entity.Relationship> relationshipEntities = entity.getRelationships();
        PersistentResource resource = entity.toPersistentResource();
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
    private PersistentResource upsertObject(Environment context, Entity entity) {
        Set<Entity.Attribute> attributes = entity.getAttributes();
        Optional<String> id = entity.getId();
        RequestScope requestScope = entity.getRequestScope();
        PersistentResource upsertedResource;
        PersistentResource parentResource;
        if (!entity.getParentResource().isPresent()) {
            parentResource = null;
        } else {
            parentResource = entity.getParentResource().get().toPersistentResource();
        }

        if (!id.isPresent()) {
            entity.setId();
            id = entity.getId();
            upsertedResource = PersistentResource.createObject(parentResource,
                    entity.getEntityClass(),
                    requestScope,
                    id);
        } else {
            Set<PersistentResource> loadedResource = fetchObject(context, requestScope, entity.getEntityClass(),
                    Optional.of(Arrays.asList(id.get())),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    false);

            if (loadedResource.isEmpty()) { /* edge case where provided id doesn't exist */
                upsertedResource = PersistentResource.createObject(parentResource,
                        entity.getEntityClass(),
                        requestScope,
                        id);
            } else {
                upsertedResource = loadedResource.iterator().next();
            }
        }

        return updateAttributes(upsertedResource, entity, attributes);
    }

    /**
     * Updates an object
     * @param toUpdate Entities to update
     * @param entity Resource entity
     * @param attributes Set of entity attributes
     * @return Persistence Resource object
     */
    private PersistentResource updateAttributes(PersistentResource toUpdate,
                                                Entity entity,
                                                Set<Entity.Attribute> attributes) {
        EntityDictionary dictionary = entity.getRequestScope().getDictionary();
        Class<?> entityClass = entity.getEntityClass();
        String idFieldName = dictionary.getIdFieldName(entityClass);

        /* iterate through each attribute provided */
        for (Entity.Attribute attribute : attributes) {
            if (dictionary.isAttribute(entityClass, attribute.getName())) {
                toUpdate.updateAttribute(attribute.getName(), attribute.getValue());
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

        Set<PersistentResource> toDelete = (Set<PersistentResource>) fetchObjects(context);
        toDelete.forEach(PersistentResource::deleteResource);
        return toDelete;
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

        Set<PersistentResource> toRemove = (Set<PersistentResource>) fetchObjects(context);
        if (!context.isRoot()) { /* has parent */
            toRemove.forEach(item -> context.parentResource.removeRelation(context.field.getName(), item));
        } else { /* is root */
            toRemove.forEach(PersistentResource::deleteResource);
        }
        return toRemove;
    }

    /**
     * Replaces a resource, updates given resource and deletes the rest
     * belonging to the the same type/relationship family.
     * @param context Environment encapsulating graphQL's request environment
     * @return set of replaced {@link PersistentResource} object(s)
     */
    private Object replaceObjects(Environment context) {
        /* sanity check for id and data argument w REPLACE */
        if (!context.data.isPresent()) {
            throw new BadRequestException("REPLACE must include data argument");
        }

        if (context.ids.isPresent()) {
            throw new BadRequestException("REPLACE must not include ids argument");
        }

        Set<PersistentResource> existingObjects = (Set<PersistentResource>) fetchObjects(context);
        Set<PersistentResource> upsertedObjects = upsertObjects(context);
        Set<PersistentResource> toDelete = Sets.difference(existingObjects, upsertedObjects);

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

    private Optional<FilterExpression> buildFilter(String typeName,
                                                   Optional<String> filter,
                                                   RequestScope requestScope) {
        // TODO: Refactor FilterDialect interfaces to accept string or List<String> instead of (or in addition to?)
        // query params.
        return filter.map(filterStr -> {
            MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>() {
                {
                    put("filter[" + typeName + "]", Arrays.asList(filterStr));
                }
            };
            try {
                return requestScope.getFilterDialect().parseTypedExpression(typeName, queryParams).get(typeName);
            } catch (ParseException e) {
                log.debug("Filter parse exception caught", e);
                throw new InvalidPredicateException("Could not parse filter for type: " + typeName);
            }
        });
    }

    private boolean requestContainsPageInfo(String entityType, Field field) {
        return field.getSelectionSet().getSelections().stream()
                .anyMatch(f -> f instanceof Field && PAGE_INFO_KEYWORD.equals(((Field) f).getName()));
    }
}
