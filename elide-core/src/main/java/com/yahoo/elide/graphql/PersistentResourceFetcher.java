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
import com.yahoo.elide.core.exceptions.UnknownEntityException;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
            Class<?> loadClass = getRecordType(context.requestScope, context.outputType);
            return fetchObject(context.requestScope, loadClass, context.ids, context.sort,
                    context.offset, context.first, context.filters);

        } else { /* fetch attribute or relationship */
            return fetchObject(context.requestScope.getDictionary(), context.parentResource,
                    context.field.getName(), context.outputType.getClass().getName());
        }
    }

    /**
     * Returns the binding record type for a given entity
     * @param requestScope request scope
     * @param outputType GraphQL output type
     * @return record type
     */
    private Class getRecordType(RequestScope requestScope, GraphQLType outputType) {
        GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) outputType).getWrappedType();
        String entityName = graphQLType.getName();

        Class recordType = requestScope.getDictionary().getEntityClass(entityName);
        if (recordType == null) {
            throw new UnknownEntityException(entityName);
        } else {
            return recordType;
        }
    }

    /**
     * Fetches a root-level entity
     * @param requestScope request scope
     * @param loadClass load class
     * @param ids list of ids (can be NULL)
     * @param sort sort by ASC/DESC
     * @param offset pagination offset argument
     * @param first pagination first argument
     * @param filters filter params
     * @return {@link PersistentResource} object(s)
     */
    private Set<PersistentResource> fetchObject(RequestScope requestScope, Class loadClass,
                                                Optional<List<String>> ids, Optional<String> sort,
                                                Optional<String> offset, Optional<String> first,
                                                Optional<String> filters) {
        /* fetching a collection */
        if(!ids.isPresent()) {

            Set<PersistentResource> records = PersistentResource.loadRecords(loadClass, requestScope, Optional.empty());

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

            Class<?> idType = dictionary.getIdType(loadClass);
            String idField = dictionary.getIdFieldName(loadClass);
            String entityTypeName = dictionary.getJsonAliasFor(loadClass);

            /* construct a new SQL like filter expression, eg: book.id IN [1,2] */
            filterExpression = Optional.of(new FilterPredicate(
                    new FilterPredicate.PathElement(
                            loadClass,
                            entityTypeName,
                            idType,
                            idField),
                    Operator.IN,
                    new ArrayList<>(idList)));

            return PersistentResource.loadRecords(loadClass, requestScope, filterExpression);
        }
    }

    /**
     * fetches a non-root level attribute or relationship
     * @param dictionary entity dictionary
     * @param parentResource parent resource
     * @param fieldName field type
     * @param objectType object type, in case of an unrecognized object type
     * @return attribute or relationship object
     */
    private Object fetchObject(EntityDictionary dictionary, PersistentResource parentResource,
                               String fieldName, String objectType) {
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

        EntityDictionary dictionary = context.requestScope.getDictionary();

        Set upsertOps = new HashSet();
        for(Map<String, Object> input : context.data.get()) {
            String idFieldName = context.isRoot() ?
                    dictionary.getIdFieldName(dictionary.getEntityClass(context.field.getName())) :
                    dictionary.getIdFieldName(context.parentResource.getResourceClass());

            /* fetch id value fed to 'data' argument */
            Optional<String> id = input.entrySet().stream()
                    .filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                    .map(e -> (String)e.getValue())
                    .findFirst();

            upsertOps.addAll(inputObjectToRelationshipUpdateOperations(input, id, context.parentResource,
                    context.requestScope, context.field.getName(), context.outputType));
        }

        return upsertOps;
    }

    /**
     * utility class to support storing a triplet on each node of the queue used while upserting
     */
    private class Triplet {
        List<Map<String, Object>> input;
        PersistentResource parentResource;
        String fieldName;

        /**
         * class constructor
         * @param input data input
         * @param parentResource parent
         * @param fieldName field name
         */
        Triplet(List<Map<String, Object>> input, PersistentResource parentResource, String fieldName) {
            this.input = input;
            this.parentResource = parentResource;
            this.fieldName = fieldName;
        }
    }

    /**
     * handles both updating/creating by first handling the attributes and then the relationships, queuing up
     * any further nested relationships
     * @param input data input
     * @param parent parent
     * @param requestScope request scope
     * @param fieldName field name
     * @return set of persistent resource objects
     */
    private Set<PersistentResource> inputObjectToRelationshipUpdateOperations(Map<String, Object> input, Optional<String> id,
                                                           PersistentResource parent, RequestScope requestScope,
                                                           String fieldName, GraphQLType outputType) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Set<PersistentResource> returnOps = new HashSet();
        Set<Triplet> visited = new HashSet();
        Queue<Triplet> toVisit = new LinkedList();
        toVisit.add(new Triplet(new ArrayList(Arrays.asList(input)), parent, fieldName));

        /* loop through the queue */
        while(!toVisit.isEmpty()) {
            /* pick the head of the queue to process */
            Triplet triplet = toVisit.poll();
            Class<?> entityClass;

            /* avoid cycles */
            if(visited.contains(triplet)) {
                continue;
            } else {
                visited.add(triplet);
            }

            /* get entity class of either root or relationship (in case of non-root) */
            if(triplet.parentResource == null) {
                entityClass = dictionary.getEntityClass(triplet.fieldName);
            } else {
                entityClass = dictionary.getParameterizedType(triplet.parentResource.getResourceClass(), triplet.fieldName);
            }

            /* fetch just the attributes from current input object */
            Map<String, Object> attributesOnly = stripRelationships(triplet.input, entityClass, dictionary);

            /* fetch ALL the relationships from current input object */
            Map<String, Object> relationshipsOnly = stripAttributes(triplet.input, entityClass, dictionary);

            /* new parent will be the resource we create from the stripped attributes */
            PersistentResource newParent = updateOrCreateAttributes(attributesOnly, id, requestScope, entityClass,
                    outputType, triplet.fieldName, triplet.parentResource);

            returnOps.add(newParent);

            /* loop through each relation */
            for(Map.Entry<String, Object> relationship : relationshipsOnly.entrySet()) {
                Object value = relationship.getValue();
                List<Map<String, Object>> currentRelationship;
                Class<?> resourceClass = triplet.parentResource == null ?
                        dictionary.getParameterizedType(entityClass, relationship.getKey()) :
                        dictionary.getParameterizedType(triplet.parentResource.getResourceClass(), relationship.getKey());


                /* handle toOne relationship */
                if(!(value instanceof List)) {
                    currentRelationship = new ArrayList();
                    currentRelationship.add((Map<String, Object>) value);
                } else {
                    currentRelationship = (List) value;
                }

                /* this set will contain the resources created from the nested relationships */
                Set<PersistentResource> relationshipResource = new HashSet();

                /* get the id field name */
                String idFieldName = dictionary.getIdFieldName(resourceClass);

                /* loop through properties of each relation */
                for(Map<String, Object> entries : currentRelationship) {
                    /* fetch the attributes in the current relationship by stripping all the nested relationships */
                    Map<String, Object> relationshipAttributes = stripRelationships(new ArrayList<>(Arrays.asList(entries)), resourceClass,
                            dictionary);

                    /* fetch id value fed to current argument */
                    Optional<String> relationshipId = relationshipAttributes.entrySet().stream()
                            .filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                            .map(e -> (String)e.getValue())
                            .findFirst();

                    /* create new resources */
                    relationshipResource.add(updateOrCreateAttributes(relationshipAttributes, relationshipId, requestScope,
                            resourceClass, outputType, relationship.getKey(), newParent));

                    /* fetch any further nested relationships for later processing */
                    Map<String, Object> nestedRelationships = stripAttributes(new ArrayList<>(Arrays.asList(entries)), resourceClass,
                            dictionary);

                    /* loop through these nested relationships and add them one by one on the queue */
                    for(Map.Entry<String, Object> nestedRelation : nestedRelationships.entrySet()) {
                        toVisit.add(new Triplet((List<Map<String, Object>>) nestedRelation.getValue(),
                                relationshipResource.iterator().next(), nestedRelation.getKey()));
                    }
                }

                /* update the relationship between parent and newly created/updated nested resources */
                newParent.updateRelation(relationship.getKey(), relationshipResource);
                returnOps.addAll(relationshipResource);
            }
        }
        return returnOps;
    }

    /**
     * updates or creates existing/new entities
     * @param input data input
     * @param id id
     * @param requestScope request scope
     * @param outputType graphql output type
     * @param fieldName field name
     * @param parent parent resource
     * @return {@link PersistentResource} object
     */
    private PersistentResource updateOrCreateAttributes(Map<String, Object> input, Optional<String> id,
                                              RequestScope requestScope, Class<?> loadClass,
                                              GraphQLType outputType, String fieldName,
                                              PersistentResource parent) {
        if(!id.isPresent()) { /* no id(s) provided, must create new */
            return createObject(input, fieldName, requestScope, Optional.empty(), outputType, parent);
        }

        Set<PersistentResource> fetchEntries;
        try {
            if(parent == null) {
                fetchEntries = fetchObject(requestScope, loadClass,
                        Optional.of(Arrays.asList(id.orElse(null))), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty());
            } else {
                fetchEntries = (Set<PersistentResource>) fetchRelationship(parent, fieldName);
            }
        } catch (BadRequestException e) {
            throw e;
        }

        if(fetchEntries.isEmpty() || fetchEntries == null) { /* empty set returned, must create new */
            return createObject(input, fieldName, requestScope, Optional.empty(), outputType, parent);
        } else { /* must update object */
            return updateObject(fetchEntries, input, fieldName, requestScope, parent);
        }
    }

    /**
     * Strips out the attributes from the {@code element} singleton list and returns just the relationships, if present.
     * @param element data input
     * @param entityClass entity class
     * @param dictionary entity dictionary
     * @return relationship map
     */
    private Map<String, Object> stripAttributes(List<Map<String, Object>> element, Class<?> entityClass,
                                                EntityDictionary dictionary) {
        Map<String, Object> relationshipsOnly = new HashMap<>();
        Map<String, Object> entry = element.get(0); /* 'element' will always be singleton */

        for(String key : entry.keySet()) {
            if(dictionary.isRelation(entityClass, key)) {
                relationshipsOnly.put(key, entry.get(key));
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
    private Map<String, Object> stripRelationships(List<Map<String, Object>> element, Class<?> entityClass,
                                                   EntityDictionary dictionary) {
        Map<String, Object> attributesOnly = new HashMap<>();
        Map<String, Object> entry = element.get(0); /* 'element' will always have just one entry */

        for(String key : entry.keySet()) {
            if(dictionary.isAttribute(entityClass, key)) {
                attributesOnly.put(key, entry.get(key));
            }
            if(Objects.equals(key, dictionary.getIdFieldName(entityClass))) {
                attributesOnly.put(key, entry.get(key));
            }
        }
        return attributesOnly;
    }

    /**
     * Creates a new "root-level" resource in the database
     * @param input individual input data objects
     * @param outputType graphql-java output type
     * @param fieldName graphql-java field type
     * @param requestScope request scope
     * @param uuid uuid
     * @return {@link PersistentResource} object
     */
    private PersistentResource createObject(Map<String, Object> input, String fieldName,
                                RequestScope requestScope, Optional<String> uuid,
                                GraphQLType outputType, PersistentResource parent) {
        EntityDictionary dictionary = requestScope.getDictionary();

        Class<?> entityClass;
        if(parent == null) {
            GraphQLObjectType objectType = (GraphQLObjectType) ((GraphQLList) outputType).getWrappedType();
            entityClass = dictionary.getEntityClass(objectType.getName());
        } else {
            entityClass = dictionary.getParameterizedType(parent.getResourceClass(), fieldName);
        }
        PersistentResource toCreate = PersistentResource.createObject(parent, entityClass, requestScope, uuid);

        //add new relation between parent and the updated object
        if(parent != null) {
            parent.addRelation(fieldName, toCreate);
        }
        return updateObject(new HashSet<>(Arrays.asList(toCreate)), input, fieldName, requestScope, parent);
    }

    /**
     * Updates an object
     * @param toUpdate entities to update
     * @param input input data to update entities with
     * @return updated object
     */
    private PersistentResource updateObject(Set<PersistentResource> toUpdate, Map<String, Object> input,
                                String fieldName, RequestScope requestScope,
                                            PersistentResource parent) {
        EntityDictionary dictionary = requestScope.getDictionary();
        Class<?> entityClass;
        if(parent == null) {
            entityClass = dictionary.getEntityClass(fieldName);
        } else {
            entityClass = dictionary.getParameterizedType(parent.getResourceClass(), fieldName);
        }

        String idFieldName = dictionary.getIdFieldName(entityClass);
        PersistentResource obj = toUpdate.iterator().next();
        for(Map.Entry<String, Object> row : input.entrySet()) {
            if(dictionary.isAttribute(entityClass, row.getKey())) { /* iterate through each attribute provided */
                obj.updateAttribute(row.getKey(), row.getValue());
            } else if(Objects.equals(row.getKey(), idFieldName)) { /* update 'id' attribute */
                obj.updateAttribute(row.getKey(), row.getValue());
            } else {
                throw new BadRequestException("Unrecognized attribute passed to 'data': " + row.getKey());
            }
        }

        return obj;
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
        return toDelete;
    }
}