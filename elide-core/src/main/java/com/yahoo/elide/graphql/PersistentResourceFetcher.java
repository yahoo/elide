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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
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
     * @param environment graphql-java's execution environment
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
     * @param environment environment encapsulating graphQL's request environment
     */
    private void filterSortPaginateSanityCheck(Environment environment) {
        if(environment.filters.isPresent() || environment.sort.isPresent() || environment.offset.isPresent()
                || environment.first.isPresent()) {
            throw new BadRequestException("Pagination/Filtering/Sorting is only supported with FETCH operation");
        }
    }

    /**
     * log current context for debugging
     * @param operation current operation
     * @param environment environment encapsulating graphQL's request environment
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
     * @param input data input
     * @param idFieldName id field name
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
     * @param context environment encapsulating graphQL's request environment
     * @return list of {@link PersistentResource} objects
     */
    private Object fetchObjects(Environment context) {
        /* sanity check for data argument w FETCH */
        if(context.data.isPresent()) {
            throw new BadRequestException("FETCH must not include data");
        }

        Class<?> entityClass;
        EntityDictionary dictionary = context.requestScope.getDictionary();
        if(context.parentResource != null) {
            entityClass = dictionary.getLoadClass(context.parentResource.getResourceClass(), context.field.getName());
        } else {
            entityClass = dictionary.getLoadClass(null, context.field.getName());
        }

        /* check whether current object has a parent or not */
        if (context.isRoot()) {
            return fetchObject(context.requestScope, entityClass, context.ids, context.sort,
                    context.offset, context.first, context.filters);

        } else { /* fetch attribute or relationship */
            return fetchObject(context.requestScope.getDictionary(), entityClass, context.ids,
                    context.parentResource, context.field.getName());
        }
    }

    /**
     * Fetches a root-level entity
     * @param requestScope request scope
     * @param entityClass entity class
     * @param ids list of ids (can be NULL)
     * @param sort sort by ASC/DESC
     * @param offset pagination offset argument
     * @param first pagination first argument
     * @param filters filter params
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
     * @param dictionary entity dictionary
     * @param entityClass entity class
     * @param ids list of ids
     * @param parentResource parent resource
     * @param fieldName field type
     * @return attribute or relationship object
     */
    private Object fetchObject(EntityDictionary dictionary, Class<?> entityClass,
                               Optional<List<String>> ids, PersistentResource parentResource,
                               String fieldName) {
        Class parentClass = parentResource.getResourceClass();

        if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return fetchAttribute(parentResource, fieldName);
        } else if(dictionary.isRelation(parentClass, fieldName)){ /* fetch relationship properties */
            return fetchRelationship(parentResource, fieldName, ids, dictionary, entityClass);
        } else {
            throw new BadRequestException("Unrecognized object: " + fieldName + " for: " + parentClass.getName());
        }
    }

    /**
     * FETCH attributes of top level entity
     * @param parentResource parent object
     * @param fieldName Field type
     * @return list of {@link PersistentResource} objects
     */
    private Object fetchAttribute(PersistentResource parentResource, String fieldName) {
        return parentResource.getAttribute(fieldName);
    }

    /**
     * Fetches a relationship for a top-level entity
     * @param parentResource parent object
     * @param fieldName field type
     * @param ids list of ids
     * @param dictionary entity dictionary
     * @param entityClass entity class
     * @return persistence resource object(s)
     */
    private Object fetchRelationship(PersistentResource parentResource, String fieldName,
                                     Optional<List<String>> ids, EntityDictionary dictionary,
                                     Class<?> entityClass) {
        Set<PersistentResource> relations = new HashSet<>();
        if(ids.isPresent()) {
            List<String> idList = ids.get();
            Optional<FilterExpression> filterExpression;

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
//            TODO: this still doesn't filter by the filterexpression, why??
//            relations = parentResource.getRelationChecked(fieldName, filterExpression);
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
     * @param context environment encapsulating graphQL's request environment
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

        Set<PersistentResource> upsertedObjects = new HashSet();
        /* upsert */
        for(Map<String, Object> input : context.data.get()) {
            upsertedObjects.addAll(graphWalker(input, this::upsertObject, Optional.ofNullable(context.parentResource),
                    context.requestScope, context.field.getName()));
        }

        /* fixup relationships */
        for(Map<String, Object> input : context.data.get()) {
            graphWalker(input, this::updateRelationship, Optional.ofNullable(context.parentResource),
                    context.requestScope, context.field.getName());
        }
//        TODO: ask how to add relation correctly, this gives us checkSharedPermission error with both a set and as singleton objects, why??
//        if(!context.isRoot()) {
//            for(PersistentResource obj : upsertedObjects) {
//                context.parentResource.addRelation(obj.getType(), obj);
//            }
//        }
        return upsertedObjects;
    }

    /**
     * utility class to support storing a triplet on each node of the queue used while upserting
     */
    private class Triplet {
        private Map<String, Object> input;
        private Optional<PersistentResource> parentResource;
        private String fieldName;

        /**
         * class constructor
         * @param input data input
         * @param parentResource parent
         * @param fieldName field name
         */
        private Triplet(Map<String, Object> input, Optional<PersistentResource> parentResource, String fieldName) {
            this.input = input;
            this.parentResource = parentResource;
            this.fieldName = fieldName;
        }
    }

    /**
     * A function to handle upserting (update/create) objects.
     */
    @FunctionalInterface
    private interface Executor<T> {
        T execute(Map<String, Object> input, RequestScope requestScope, Class<?> entityClass);
    }

    /**
     * Forms the graph from data {@param input} and upsert's {@param function} all the nodes
     * @param input input data
     * @param function function to process nodes
     * @param parentResource parent resource
     * @param requestScope request scope
     * @param fieldName graphql field name
     * @return set of {@link PersistentResource} objects
     */
    private Set<PersistentResource> graphWalker(Map<String, Object> input, Executor function,
                                                Optional<PersistentResource> parentResource, RequestScope requestScope,
                                                String fieldName) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Queue<Triplet> toVisit = new ArrayDeque();
        Set<Triplet> visited = new HashSet();
        toVisit.add(new Triplet(input, parentResource, fieldName));
        Set<PersistentResource> toReturn = new HashSet<>();
        while(!toVisit.isEmpty()) {
            Triplet triplet = toVisit.remove();
            if(visited.contains(triplet)) {
                continue;
            } else {
                visited.add(triplet);
            }
            Class<?> entityClass = triplet.parentResource
                    .<Class<?>>map(parent -> dictionary.getLoadClass(parent.getResourceClass(), triplet.fieldName))
                    .orElseGet(() -> dictionary.getLoadClass(null, triplet.fieldName));

            /* first execute the given function with input data and then add the remaining relationships back to queue */
            PersistentResource newParent = (PersistentResource) function.execute(triplet.input, requestScope, entityClass);
            toReturn.add(newParent);
            Map<String, Object> relationships = stripAttributes(triplet.input, entityClass, dictionary);
            /* loop over relationships */
            for(Map.Entry<String, Object> relationship : relationships.entrySet()) {
                Boolean isToOne = newParent.getRelationshipType(relationship.getKey()).isToOne();
                List<Map<String, Object>> entryToAdd = new ArrayList<>();
                if(isToOne) {
                    entryToAdd.add((Map<String, Object>) relationship.getValue());
                } else {
                    entryToAdd.addAll((List) relationship.getValue());
                }
                /* loop over each resource of the relationship */
                for(Map<String, Object> entry : entryToAdd) {
                    toVisit.add(new Triplet(entry, Optional.of(newParent), relationship.getKey()));
                }
            }
        }
        return toReturn;
    }

    /**
     * update the relationship between {@param parent} and the resource loaded by given {@param id}
     * @param dataField data input
     * @param requestScope request scope
     * @param entityClass entity class
     * @return {@link PersistentResource} object
     */
    private PersistentResource updateRelationship(Map<String, Object> dataField, RequestScope requestScope,
                                                  Class<?> entityClass) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Map<String, Object> relationships = stripAttributes(dataField, entityClass, dictionary);
        String id = getId(dataField, dictionary.getIdFieldName(entityClass)).get();
        PersistentResource resource = PersistentResource.loadRecord(entityClass, id, requestScope);

        /* loop over each relationship */
        for(Map.Entry<String, Object> relationship : relationships.entrySet()) {
            Class<?> loadClass = dictionary.getLoadClass(resource.getResourceClass(), relationship.getKey());
            Boolean isToOne = resource.getRelationshipType(relationship.getKey()).isToOne();
            List<Map<String, Object>> relationshipToList = new ArrayList<>();
            Set<PersistentResource> relationshipSet = new HashSet<>();
            if(isToOne) {
                relationshipToList.add((Map<String, Object>) relationship.getValue());
            } else {
                relationshipToList.addAll((List) relationship.getValue());
            }
            /* loop over each resource of the relationship */
            for(Map<String, Object> entry : relationshipToList) {
                id = getId(entry, dictionary.getIdFieldName(entityClass)).get();
                relationshipSet.add(PersistentResource.loadRecord(loadClass, id, requestScope));
            }
            resource.updateRelation(relationship.getKey(), relationshipSet);
        }
        return resource;
    }

    /**
     * updates or creates existing/new entities
     * @param dataField data input
     * @param requestScope request scope
     * @return {@link PersistentResource} object
     */
    private PersistentResource upsertObject(Map<String, Object> dataField, RequestScope requestScope,
                                            Class<?> entityClass) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Map<String, Object> input = stripRelationships(dataField, entityClass, dictionary);
        String idFieldName = dictionary.getIdFieldName(entityClass);
        Optional<String> id = getId(input, idFieldName);
        if(!id.isPresent()) {
            String uuid = UUID.randomUUID().toString()
                    .replaceAll("[^0-9]", "")
                    .substring(0, 3); //limit the number of digits to prevent InvalidValueException in PersistentResource.createObject()
            //TODO: this is hacky, ask for a workaround for this.
            dataField.put(idFieldName, uuid);
            id = Optional.of(uuid);
        }
        PersistentResource upsertedResource;
        Set<PersistentResource> loadedResource = fetchObject(requestScope, entityClass, Optional.of(Arrays.asList(id.get())),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        if(loadedResource.isEmpty()) {
            //TODO: ask if we need a parent here
            upsertedResource = PersistentResource.createObject(null, entityClass, requestScope, id);
        } else {
            upsertedResource = loadedResource.iterator().next();
        }
        return updateAttributes(upsertedResource, entityClass, input, requestScope);
    }

    /**
     * Strips out the attributes from the {@code element} singleton list and returns just the relationships, if present.
     * @param element data input
     * @param entityClass entity class
     * @param dictionary entity dictionary
     * @return relationship map
     */
    private static Map<String, Object> stripAttributes(Map<String, Object> element, Class<?> entityClass,
                                                EntityDictionary dictionary) {
        Map<String, Object> relationshipsOnly = new HashMap<>();

        for(String key : element.keySet()) {
            if(dictionary.isRelation(entityClass, key)) {
                relationshipsOnly.put(key, element.get(key));
            }
        }
        return relationshipsOnly;
    }

    /**
     * Strips out the relationships from the {@code element} singleton list and returns just the attributes, if present.
     * @param element data input
     * @param entityClass entity class
     * @param dictionary entity dictionary
     * @return attributes map
     */
    private static Map<String, Object> stripRelationships(Map<String, Object> element, Class<?> entityClass,
                                                   EntityDictionary dictionary) {
        Map<String, Object> attributesOnly = new HashMap<>();
        String idFieldName = dictionary.getIdFieldName(entityClass);

        for(String key : element.keySet()) {
            if(dictionary.isAttribute(entityClass, key)) {
                attributesOnly.put(key, element.get(key));
            }
            if(Objects.equals(key, idFieldName)) {
                attributesOnly.put(key, element.get(key));
            }
        }
        return attributesOnly;
    }

    /**
     * Updates an object
     * @param toUpdate entities to update
     * @param entityClass
     * @param input input data to update entities with  @return updated object
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
     * @param context environment encapsulating graphQL's request environment
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
     * @param context environment encapsulating graphQL's request environment
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
     * @param context environment encapsulating graphQL's request environment
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