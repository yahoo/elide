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
import java.util.Collections;
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

    PersistentResourceFetcher(ElideSettings settings) { this.settings = settings; }

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
     * Utility method to get the entityClass using the {@param dictionary} and the {@param fieldname} based on whether
     * the {@param parent} is present or not
     * @param parent parent resource
     * @param dictionary entity dictionary
     * @param fieldName graphql field name
     * @return entityClass
     */
    private static Class<?> getEntityClass(Optional<PersistentResource> parent, EntityDictionary dictionary, String fieldName) {
        return !parent.isPresent() ? getRecordType(dictionary, fieldName) :
                dictionary.getParameterizedType(parent.get().getResourceClass(), fieldName);
    }

    /**
     * Utility method to get id field name associated with {@param entityClass} of a resource
     * @param dictionary entity dictionary
     * @param entityClass entity class
     * @return idFieldName
     */
    private static String getIdFieldName(EntityDictionary dictionary, Class<?> entityClass) {
        return dictionary.getIdFieldName(entityClass);
    }

    /**
     * Utility method to get the id in provided {@param input}
     * @param input data input
     * @param idFieldName id field name
     * @return id
     */
    private static Optional<String> getId(Map<String, Object> input, String idFieldName) {
        return  input.entrySet().stream()
                .filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                .map(e -> (String)e.getValue())
                .findFirst();
    }

    /**
     * Utility method to set the id in {@param input} if one wasn't provided in user mutation
     * @param input data input
     * @param idFieldName id field name
     */
    private static void setId(Map<String, Object> input, String idFieldName) {
        String id = UUID.randomUUID().toString()
                .replaceAll("[^0-9]", "")
                .substring(0, 3); //limit the number of digits to prevent InvalidValueException in PersistentResource.createObject()
        //TODO: this is hacky, ask for a workaround for this.
        input.put(idFieldName, id);
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

        /* check whether current object has a parent or not */
        if (context.isRoot()) {
            Class<?> entityClass = getRecordType(context.requestScope.getDictionary(), context.field.getName());
            return fetchObject(context.requestScope, entityClass, context.ids, context.sort,
                    context.offset, context.first, context.filters);

        } else { /* fetch attribute or relationship */
            return fetchObject(context.requestScope.getDictionary(), context.parentResource,
                    context.field.getName());
        }
    }

    /**
     * Returns the binding record type for a given entity
     * @param dictionary entity dictionary
     * @param fieldName field name
     * @return record type
     */
    private static Class<?> getRecordType(EntityDictionary dictionary, String fieldName) {
        return dictionary.getEntityClass(fieldName);
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
            String idField = getIdFieldName(dictionary, entityClass);
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
     * @param parentResource parent resource
     * @param fieldName field type
     * @return attribute or relationship object
     */
    private Object fetchObject(EntityDictionary dictionary, PersistentResource parentResource,
                               String fieldName) {
        Class parentClass = parentResource.getResourceClass();

        if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return fetchAttribute(parentResource, fieldName);

        } else if(dictionary.isRelation(parentClass, fieldName)){ /* fetch relationship properties */
            return fetchRelationship(parentResource, fieldName);
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
     * @return persistence resource object(s)
     */
    private Object fetchRelationship(PersistentResource parentResource, String fieldName) {
        //TODO: fix "fetch" nested entities by id(s), ask if we can use getRelationChecked() [protected method]
        Set<PersistentResource> relations = parentResource.getRelationCheckedFiltered(fieldName);

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

        Set upsertOps = new HashSet();
        /* upsert */
        for(Map<String, Object> input : context.data.get()) {
            upsertOps.addAll(graphWalker(input, this::upsertObject, Optional.ofNullable(context.parentResource),
                    context.requestScope, context.field.getName()));
        }

        /* fixup relationships */
        for(Map<String, Object> input : context.data.get()) {
            graphWalker(input, this::updateRelationship, Optional.ofNullable(context.parentResource),
                    context.requestScope, context.field.getName());
        }

        return upsertOps;
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
        T execute(Map<String, Object> input, String id,
                                   RequestScope requestScope, Class<?> entityClass,
                                   String fieldName, Optional<PersistentResource> parent);
    }

    /**
     * Forms the graph from data {@param input} and upsert's {@param function} all the nodes
     * @param input input data
     * @param function function to process nodes
     * @param parent parent resource
     * @param requestScope request scope
     * @param fieldName graphql field name
     * @return set of {@link PersistentResource} objects
     */
    private Set<PersistentResource> graphWalker(Map<String, Object> input, Executor function,
                                                Optional<PersistentResource> parent, RequestScope requestScope,
                                                String fieldName) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Queue<Triplet> toVisit = new ArrayDeque();
        Set<Triplet> visited = new HashSet();
        toVisit.add(new Triplet(input, parent, fieldName));
        Set<PersistentResource> toReturn = new HashSet<>();

        while(!toVisit.isEmpty()) {
            Triplet triplet = toVisit.remove();
            if(visited.contains(triplet)) {
                continue;
            } else {
                visited.add(triplet);
            }
            Class<?> entityClass = getEntityClass(triplet.parentResource, dictionary, triplet.fieldName);
            String idFieldName = getIdFieldName(dictionary, entityClass);
            Optional<String> id = getId(triplet.input, idFieldName);
            if(!id.isPresent()) {
                setId(triplet.input, idFieldName);
            }
            /* first execute the given function with input data and then add the remaining relationships back to queue */
            PersistentResource newParent = (PersistentResource) function.execute(triplet.input, getId(triplet.input,
                    idFieldName).get(), requestScope, entityClass, triplet.fieldName, triplet.parentResource);
            toReturn.add(newParent);
            Map<String, Object> relationshipsOnly = stripAttributes(triplet.input, entityClass,
                    dictionary);
            /* loop over relationships */
            for(Map.Entry<String, Object> relationship : relationshipsOnly.entrySet()) {
                Boolean isToOne = newParent.getRelationshipType(relationship.getKey()).isToOne();
                List<Map<String, Object>> entryToAdd;
                if(isToOne) {
                    entryToAdd = new ArrayList<>();
                    entryToAdd.add((Map<String, Object>) relationship.getValue());
                } else {
                    entryToAdd = (List) relationship.getValue();
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
     * @param id id
     * @param requestScope request scope
     * @param entityClass entity class
     * @param fieldName graphql field name
     * @param parent parent resource
     * @return {@link PersistentResource} object
     */
    private PersistentResource updateRelationship(Map<String, Object> dataField, String id,
                                            RequestScope requestScope, Class<?> entityClass,
                                            String fieldName, Optional<PersistentResource> parent) {
        PersistentResource resource = PersistentResource.loadRecord(entityClass, id, requestScope);
        parent.ifPresent(persistentResource ->
                persistentResource.updateRelation(fieldName, Collections.singleton(resource)));
        return resource;
    }

    /**
     * updates or creates existing/new entities
     * @param dataField data input
     * @param id id
     * @param requestScope request scope
     * @param fieldName field name
     * @param parent parent resource
     * @return {@link PersistentResource} object
     */
    private PersistentResource upsertObject(Map<String, Object> dataField, String id,
                                             RequestScope requestScope, Class<?> entityClass,
                                             String fieldName, Optional<PersistentResource> parent) {
        Map<String, Object> input = stripRelationships(dataField, entityClass, requestScope.getDictionary());

        Set<PersistentResource> fetchEntries = fetchObject(requestScope, entityClass,
                        Optional.of(Arrays.asList(id)), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty());

        if(fetchEntries.isEmpty()) { /* empty set returned, must create new */
            return createObject(input, fieldName, requestScope, Optional.of(id), parent);
        } else { /* must update object */
            return updateAttributes(fetchEntries.iterator().next(), entityClass, input, requestScope);
        }
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
        String idFieldName = getIdFieldName(dictionary, entityClass);

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
     * Creates a new "root-level" resource in the database
     * @param input individual input data objects
     * @param fieldName graphql-java field type
     * @param requestScope request scope
     * @param uuid uuid
     * @return {@link PersistentResource} object
     */
    private PersistentResource createObject(Map<String, Object> input, String fieldName,
                                RequestScope requestScope, Optional<String> uuid,
                                            Optional<PersistentResource> parent) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Class<?> entityClass = getEntityClass(parent, dictionary, fieldName);
        PersistentResource toCreate = PersistentResource.createObject(parent.orElse(null), entityClass, requestScope, uuid);
        //add new relation between parent and the updated object
        if(parent.isPresent()) {
            parent.get().addRelation(fieldName, toCreate);
        }
        return updateAttributes(toCreate, entityClass, input, requestScope);
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

        String idFieldName = getIdFieldName(dictionary, entityClass);
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