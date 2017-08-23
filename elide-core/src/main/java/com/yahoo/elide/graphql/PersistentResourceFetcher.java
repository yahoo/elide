/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.google.common.collect.Sets;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;


@Slf4j
public class PersistentResourceFetcher implements DataFetcher {
    private final ElideSettings settings;

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
        if(operation != RelationshipOp.FETCH) {
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
        if(environment.filters.isPresent() || environment.sort.isPresent() || environment.offset.isPresent()
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
        if(context.data.isPresent()) {
            throw new BadRequestException("FETCH must not include data");
        }

        Class<?> entityClass;
        EntityDictionary dictionary = context.requestScope.getDictionary();

        /* check whether current object has a parent or not */
        if (context.isRoot()) {
            entityClass = dictionary.getEntityClass(context.field.getName());
            return fetchObject(context.requestScope, entityClass, context.ids, context.sort,
                    context.offset, context.first, context.filters);

        } else { /* fetch attribute or relationship */
            Class parentClass = context.parentResource.getResourceClass();
            String idFieldName = dictionary.getIdFieldName(parentClass);
            String fieldName = context.field.getName();
            if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
                return context.parentResource.getAttribute(fieldName);
            } else if(dictionary.isRelation(parentClass, fieldName)) { /* fetch relationship properties */
                return fetchRelationship(context.parentResource, fieldName, context.ids);
            } else if(Objects.equals(idFieldName, fieldName)) {
                return context.parentResource.getId();
            } else {
                throw new BadRequestException("Unrecognized object: " + fieldName + " for: " + parentClass.getName());
            }
        }
    }

    /**
     * Fetches a root-level entity
     * @param requestScope Request scope
     * @param entityClass Entity class
     * @param ids List of ids (can be NULL)
     * @param sort Sort by ASC/DESC
     * @param offset Pagination offset argument
     * @param first Pagination first argument
     * @param filters Filter params
     * @return {@link PersistentResource} object(s)
     */
    private Set<PersistentResource> fetchObject(RequestScope requestScope, Class entityClass,
                                                Optional<List<String>> ids, Optional<String> sort,
                                                Optional<String> offset, Optional<String> first,
                                                Optional<String> filters) {
        /* fetching a collection */
        if(!ids.isPresent()) {
            Set<PersistentResource> records = PersistentResource.loadRecords(entityClass, requestScope, Optional.empty());
            //TODO: paginate/filter/sort
            return records;
        } else { /* fetching by id(s) */
            List<String> idList = ids.get();

            /* handle empty list of ids */
            if(idList.isEmpty()) {
                throw new BadRequestException("Empty list passed to ids");
            }

            /* access records from internal db and return */
            Optional<FilterExpression> filterExpression;
            EntityDictionary dictionary = requestScope.getDictionary();

            Class<?> idType = dictionary.getIdType(entityClass);
            String idField = dictionary.getIdFieldName(entityClass);
            String entityTypeName = dictionary.getJsonAliasFor(entityClass);

            /* construct a new SQL like filter expression, eg: book.id IN [1,2] */
            filterExpression = Optional.of(new FilterPredicate(
                    new FilterPredicate.PathElement(
                            entityClass,
                            entityTypeName,
                            idType,
                            idField),
                    Operator.IN,
                    new ArrayList<>(idList)));
            return PersistentResource.loadRecords(entityClass, requestScope, filterExpression);
        }
    }

    /**
     * Fetches a relationship for a top-level entity
     * @param parentResource Parent object
     * @param fieldName Field type
     * @param ids List of ids
     * @return persistence resource object(s)
     */
    private Object fetchRelationship(PersistentResource parentResource, String fieldName,
                                     Optional<List<String>> ids) {
        Set<PersistentResource> relations = new HashSet<>();
        if(ids.isPresent()) {
            List<String> idList = ids.get();
//            TODO: poor latency (for loop), refactor getRelation() to allow filterexpression
            for(String id : idList) {
                relations.add(parentResource.getRelation(fieldName, id));
            }
        } else {
            relations = parentResource.getRelationCheckedFiltered(fieldName);
        }

        /* check for toOne relationships */
        Boolean isToOne = parentResource.getRelationshipType(fieldName).isToOne();

        if(isToOne) {
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
        if(context.ids.isPresent()) {
            throw new BadRequestException("UPSERT must not include ids");
        }

        if(!context.data.isPresent()) {
            throw new BadRequestException("UPSERT must include data argument");
        }

        Class<?> entityClass;
        EntityDictionary dictionary = context.requestScope.getDictionary();
        if(context.isRoot()) {
            entityClass = dictionary.getEntityClass(context.field.getName());
        } else {
            entityClass = dictionary.getParameterizedType(context.parentResource.getResourceClass(), context.field.getName());
        }

        /* form entities */
        Optional<Entity> parentEntity;
        if(!context.isRoot()) {
            parentEntity = Optional.of(new Entity(Optional.empty(), null, context.parentResource.getResourceClass(),
                    context.requestScope));
        } else {
            parentEntity = Optional.empty();
        }
        Set<Entity> entitySet = new HashSet<>();
        for(Map<String, Object> input : context.data.get()) {
            entitySet.add(new Entity(parentEntity, input, entityClass, context.requestScope));
        }

        /* upsert */
        for(Entity entity : entitySet) {
            graphWalker(entity, this::upsertObject);
        }

        /* fixup relationships */
        for(Entity entity : entitySet) {
            graphWalker(entity, this::updateRelationship);
            if (!context.isRoot()) { /* add relation between parent and nested entity */
                context.parentResource.addRelation(context.field.getName(), entity.toPersistentResource());
            }
        }

        return entitySet.stream().map(Entity::toPersistentResource).collect(Collectors.toSet());
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
        Set<Entity> visited = new HashSet();
        toVisit.add(entity);

        while(!toVisit.isEmpty()) {
            Entity currentEntity = toVisit.remove();
            if(visited.contains(currentEntity)) {
                continue;
            } else {
                visited.add(currentEntity);
            }
            function.execute(currentEntity);
            Set<Entity.Relationship> relationshipEntities = currentEntity.getRelationships();
            /* loop over relationships */
            for(Entity.Relationship relationship : relationshipEntities) {
                for(Entity relation : relationship.getValue()) {
                    toVisit.add(relation);
                }
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
        for(Entity.Relationship relationship : relationshipEntities) {
            toUpdate = new HashSet<>();
            for(Entity relation : relationship.getValue()) {
                toUpdate.add(relation.toPersistentResource());
            }
            resource.updateRelation(relationship.getName(), toUpdate);
        }
        return resource;
    }

    /**
     * updates or creates existing/new entities
     * @param entity Resource entity
     * @return {@link PersistentResource} object
     */
    private PersistentResource upsertObject(Entity entity) {
        Set<Entity.Attribute> attributes = entity.getAttributes();
        Optional<String> id = entity.getId();
        RequestScope requestScope = entity.getRequestScope();
        PersistentResource upsertedResource;
        PersistentResource parentResource;
        if(!entity.getParentResource().isPresent()) {
            parentResource = null;
        } else {
            parentResource = entity.getParentResource().get().toPersistentResource();
        }

        if(!id.isPresent()) {
            entity.setId();
            id = entity.getId();
            upsertedResource = PersistentResource.createObject(parentResource, entity.getEntityClass(), requestScope, id);
        } else {
            Set<PersistentResource> loadedResource = fetchObject(requestScope, entity.getEntityClass(),
                    Optional.of(Arrays.asList(id.get())), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

            if(loadedResource.isEmpty()) { /* edge case where provided id doesn't exist */
                upsertedResource = PersistentResource.createObject(parentResource, entity.getEntityClass(), requestScope, id);
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
    private PersistentResource updateAttributes(PersistentResource toUpdate, Entity entity, Set<Entity.Attribute> attributes) {
        EntityDictionary dictionary = entity.getRequestScope().getDictionary();
        Class<?> entityClass = entity.getEntityClass();
        String idFieldName = dictionary.getIdFieldName(entityClass);

        for(Entity.Attribute attribute : attributes) {
            if(dictionary.isAttribute(entityClass, attribute.getName())) { /* iterate through each attribute provided */
                toUpdate.updateAttribute(attribute.getName(), attribute.getValue());
            } else if(!Objects.equals(attribute.getName(), idFieldName)) {
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
        if(context.data.isPresent()) {
            throw new BadRequestException("DELETE must not include data argument");
        }

        if(!context.ids.isPresent()) {
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
        if(context.data.isPresent()) {
            throw new BadRequestException("REPLACE must not include data argument");
        }

        if(!context.ids.isPresent()) {
            throw new BadRequestException("REPLACE must include ids argument");
        }

        Set<PersistentResource> toRemove = (Set<PersistentResource>) fetchObjects(context);
        if(!context.isRoot()) { /* has parent */
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
        if(!context.data.isPresent()) {
            throw new BadRequestException("REPLACE must include data argument");
        }

        if(context.ids.isPresent()) {
            throw new BadRequestException("REPLACE must not include ids argument");
        }

        Set<PersistentResource> existingObjects = (Set<PersistentResource>) fetchObjects(context);
        Set<PersistentResource> upsertedObjects = upsertObjects(context);
        Set<PersistentResource> toDelete = Sets.difference(existingObjects, upsertedObjects);

        if(!context.isRoot()) { /* has parent */
            toDelete.forEach(item -> context.parentResource.removeRelation(context.field.getName(), item));
        } else { /* is root */
            toDelete.forEach(PersistentResource::deleteResource);
        }
        return upsertedObjects;
    }
}