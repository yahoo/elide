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
     * Utility method to get the id in provided {@param input}
     * @param input Data input
     * @param idFieldName Id field name
     * @return id
     */
    private static Optional<String> getId(Map<String, Object> input, String idFieldName) {
        return input.entrySet().stream()
               .filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
               .map(e -> (String)e.getValue())
               .findFirst();
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
            entityClass = dictionary.getParameterizedType(context.parentResource.getResourceClass(), context.field.getName());
            return fetchObject(context.requestScope.getDictionary(), entityClass, context.ids,
                    context.parentResource, context.field.getName());
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
     * fetches a non-root level attribute or relationship
     * @param dictionary Entity dictionary
     * @param entityClass Entity class
     * @param ids List of ids
     * @param parentResource Parent resource
     * @param fieldName Field type
     * @return attribute or relationship object
     */
    private Object fetchObject(EntityDictionary dictionary, Class<?> entityClass,
                               Optional<List<String>> ids, PersistentResource parentResource,
                               String fieldName) {
        Class parentClass = parentResource.getResourceClass();

        if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return parentResource.getAttribute(fieldName);
        } else if(dictionary.isRelation(parentClass, fieldName)){ /* fetch relationship properties */
            return fetchRelationship(parentResource, fieldName, ids);
        } else {
            throw new BadRequestException("Unrecognized object: " + fieldName + " for: " + parentClass.getName());
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

        Set<PersistentResource> upsertedObjects = new HashSet();
        /* upsert */
        for(Map<String, Object> input : context.data.get()) {
            upsertedObjects.addAll(graphWalker(input, this::upsertObject, entityClass, context.requestScope));
        }

        /* fixup relationships */
        for(Map<String, Object> input : context.data.get()) {
            graphWalker(input, this::updateRelationship, entityClass, context.requestScope);
        }

        /* add relation between parent and nested entity */
        if(!context.isRoot()) {
            for(PersistentResource obj : upsertedObjects) {
                context.parentResource.addRelation(context.field.getName(), obj);
            }
        }
        return upsertedObjects;
    }

    /**
     * A function to handle upserting (update/create) objects.
     */
    @FunctionalInterface
    private interface Executor<T> {
        T execute(Entity entity, RequestScope requestScope);
    }

    /**
     * Forms the graph from data {@param input} and upsert's {@param function} all the nodes
     * @param input Input data
     * @param function Function to process nodes
     * @param entityClass Entity class
     * @param requestScope Request scope
     * @return set of {@link PersistentResource} objects
     */
    private Set<PersistentResource> graphWalker(Map<String, Object> input, Executor function,
                                                Class<?> entityClass, RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Queue<Entity> toVisit = new ArrayDeque();
        Set<Entity> visited = new HashSet();
        toVisit.add(new Entity(Optional.empty(), Optional.of(input), entityClass));
        Set<PersistentResource> toReturn = new HashSet<>();

        while(!toVisit.isEmpty()) {
            Entity entity = toVisit.remove();
            if(visited.contains(entity)) {
                continue;
            } else {
                visited.add(entity);
            }
            PersistentResource newParent = (PersistentResource) function.execute(entity, requestScope);
            if(toReturn.isEmpty()) {
                toReturn.add(newParent);
            }
            Set<Entity> relationshipEntities = entity.stripAttributes(requestScope);
            /* loop over relationships */
            for(Entity relationship : relationshipEntities) {
                String firstKey = relationship.getData().keySet().iterator().next();
                Object firstValue = relationship.getData().get(firstKey);
                Boolean isToOne = newParent.getRelationshipType(firstKey).isToOne();
                List<Map<String, Object>> entryToAdd = new ArrayList<>();
                if(isToOne) {
                    entryToAdd.add((Map<String, Object>) firstValue);
                } else {
                    entryToAdd.addAll((List) firstValue);
                }
                /* loop over each resource of the relationship */
                for(Map<String, Object> entry : entryToAdd) {
                    Class<?>  loadClass = dictionary.getParameterizedType(entity.getEntityClass(), firstKey);
                    toVisit.add(new Entity(Optional.of(entity), Optional.of(entry), loadClass));
                }
            }
        }
        return toReturn;
    }

    /**
     * update the relationship between {@param parent} and the resource loaded by given {@param id}
     * @param entity Resource entity
     * @param requestScope Request scope
     * @return {@link PersistentResource} object
     */
    private PersistentResource updateRelationship(Entity entity, RequestScope requestScope) {
        Set<Entity> relationshipEntities = entity.stripAttributes(requestScope);
        PersistentResource resource = entity.toPersistentResource(requestScope);
        /* loop over each relationship */
        for(Entity relationship : relationshipEntities) {
            String firstKey = relationship.getData().keySet().iterator().next();
            Object firstValue = relationship.getData().get(firstKey);
            Boolean isToOne = resource.getRelationshipType(firstKey).isToOne();
            List<Map<String, Object>> relationshipToList = new ArrayList<>();
            Set<PersistentResource> relationshipSet = new HashSet<>();
            if(isToOne) {
                relationshipToList.add((Map<String, Object>) firstValue);
            } else {
                relationshipToList.addAll((List) firstValue);
            }
            /* loop over each resource of the relationship */
            for(Map<String, Object> entry : relationshipToList) {
                Entity relationshipEntity = new Entity(relationship.getParentResource(), Optional.of(entry),
                        relationship.getEntityClass());
                relationshipSet.add(relationshipEntity.toPersistentResource(requestScope));
            }
            resource.updateRelation(firstKey, relationshipSet);
        }
        return resource;
    }

    /**
     * updates or creates existing/new entities
     * @param entity Resource entity
     * @param requestScope Request scope
     * @return {@link PersistentResource} object
     */
    private PersistentResource upsertObject(Entity entity, RequestScope requestScope) {
        Entity attributeEntity = entity.stripRelationships(requestScope);
        Optional<String> id = attributeEntity.getId(requestScope);
        if(!id.isPresent()) {
            entity.setId(requestScope);
            id = entity.getId(requestScope);
        }
        PersistentResource upsertedResource;
        Set<PersistentResource> loadedResource = fetchObject(requestScope, attributeEntity.getEntityClass(),
                Optional.of(Arrays.asList(id.get())), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        if(loadedResource.isEmpty()) {
            PersistentResource parentResource;
            if(!attributeEntity.getParentResource().isPresent()) {
                parentResource = null;
            } else {
                parentResource = attributeEntity.getParentResource().get().toPersistentResource(requestScope);
            }
            upsertedResource = PersistentResource.createObject(parentResource, attributeEntity.getEntityClass(), requestScope, id);
        } else {
            upsertedResource = loadedResource.iterator().next();
        }
        return updateAttributes(upsertedResource, attributeEntity.getEntityClass(), attributeEntity.getData(), requestScope);
    }

    /**
     * Updates an object
     * @param toUpdate Entities to update
     * @param entityClass Entity Class
     * @param input Input data to update entities with  @return updated object
     */
    private PersistentResource updateAttributes(PersistentResource toUpdate, Class<?> entityClass,
                                                Map<String, Object> input, RequestScope requestScope) {
        EntityDictionary dictionary = requestScope.getDictionary();

        String idFieldName = dictionary.getIdFieldName(entityClass);
        for(Map.Entry<String, Object> row : input.entrySet()) {
            if(dictionary.isAttribute(entityClass, row.getKey())) { /* iterate through each attribute provided */
                toUpdate.updateAttribute(row.getKey(), row.getValue());
            } else if(Objects.equals(row.getKey(), idFieldName)) { /* update 'id' attribute */
                toUpdate.updateAttribute(row.getKey(), row.getValue());
            } else {
                throw new IllegalStateException("Unrecognized attribute passed to 'data': " + row.getKey());
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