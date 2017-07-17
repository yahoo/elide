/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

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
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLObjectType;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
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
     * @return a collection of persistent resource objects
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

            case DELETE:
                return deleteObjects(context);

            case REMOVE:
                return removeObjects(context);

            case UPSERT:
                return upsertObjects(context);

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
     * @return list of persistent resource objects
     */
    private Object fetchObjects(Environment context) {
        /* sanity check for data argument w FETCH */
        if(context.data.isPresent()) {
            throw new BadRequestException("FETCH must not include data");
        }

        /* check whether current object has a parent or not */
        if (context.isRoot()) {
            return fetchObject(context.requestScope, context.outputType, context.ids, context.sort,
                    context.offset, context.first, context.filters);

        } else { /* fetch attribute or relationship */
            //TODO: fix "fetch" nested entities by id(s)
            return fetchObject(context.requestScope.getDictionary(), context.parentResource,
                    context.field, context.outputType.getClass().getName());
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

        String entityType = graphQLType.getName();
        Class recordType = requestScope.getDictionary().getEntityClass(entityType);
        if (recordType == null) {
            throw new UnknownEntityException(entityType);
        } else {
            return recordType;
        }
    }

    /**
     * Fetches a root-level entity
     * @param requestScope request scope
     * @param outputType GraphQL output type
     * @param ids list of ids (can be NULL)
     * @param sort sort by ASC/DESC
     * @param offset pagination offset argument
     * @param first pagination first argument
     * @param filters filter params
     * @return persistent resource object(s)
     */
    private Object fetchObject(RequestScope requestScope, GraphQLType outputType, Optional<List<String>> ids,
                               Optional<String> sort, Optional<String> offset,
                               Optional<String> first, Optional<String> filters) {
        Class recordType = getRecordType(requestScope, outputType);

        /* fetching a collection */
        if(!ids.isPresent()) {

            Set<PersistentResource> records = PersistentResource.loadRecords(recordType, requestScope, Optional.empty());

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

            Class<?> idType = dictionary.getIdType(recordType);
            String idField = dictionary.getIdFieldName(recordType);
            String entityTypeName = dictionary.getJsonAliasFor(recordType);

            /* construct a new SQL like filter expression, eg: book.id IN [1,2] */
            filterExpression = Optional.of(new FilterPredicate(
                    new FilterPredicate.PathElement(
                            recordType,
                            entityTypeName,
                            idType,
                            idField),
                    Operator.IN,
                    new ArrayList<>(idList)));

            return PersistentResource.loadRecords(recordType, requestScope, filterExpression);
        }
    }

    /**
     * fetches a non-root level entity
     * @param dictionary entity dictionary
     * @param parentResource parent resource
     * @param field field type
     * @param objectType object type, in case of an unrecognized object type
     * @return attribute or relationship object
     */
    private Object fetchObject(EntityDictionary dictionary, PersistentResource parentResource,
                               Field field, String objectType) {
        Class parentClass = parentResource.getResourceClass();
        String fieldName = field.getName();

        if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
            return fetchAttribute(parentResource, field);

        } else if(dictionary.isRelation(parentClass, fieldName)){ /* fetch relationship properties */
            return fetchRelationship(parentResource, field);
        } else {
            throw new BadRequestException("Unrecognized object type: " + objectType);
        }
    }

    /**
     * FETCH attributes of top level entity
     * @param parentResource parent object
     * @param field Field type
     * @return list of persistent resource objects
     */
    private Object fetchAttribute(PersistentResource parentResource, Field field) {
        return parentResource.getAttribute(field.getName());
    }

    /**
     * Fetches a relationship for a top-level entity
     * @param parentResource parent object
     * @param field Field type
     * @return persistence resource object(s)
     */
    private Object fetchRelationship(PersistentResource parentResource, Field field) {
        String fieldName = field.getName();

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
     * @return list of persistent resource objects
     */
    private Object upsertObjects(Environment context) {
        /* sanity check for id and data argument w UPSERT */
        if(context.ids.isPresent()) {
            throw new BadRequestException("UPSERT must not include ids");
        }

        if(!context.data.isPresent()) {
            throw new BadRequestException("UPSERT must include data argument");
        }

        Set<PersistentResource> upsertedObjects = new HashSet<>();
        for(Map<String, Object> input : context.data.get()) {
            EntityDictionary dictionary = context.requestScope.getDictionary();


            if(context.isRoot()) {
                /* get internal reference idFieldName, 'id' in this case */
                String idFieldName = dictionary.getIdFieldName(dictionary.getEntityClass(context.field.getName()));

                upsertedObjects.add((PersistentResource) upsertObject(idFieldName, input,
                        context.requestScope, context.field, context.outputType));
            } else {
                String idFieldName = dictionary.getIdFieldName(context.parentResource.getResourceClass());

                upsertedObjects.add((PersistentResource) upsertObject(idFieldName, input, dictionary,
                        context.parentResource, context.field, context.outputType, context.requestScope));
            }
        }
        return upsertedObjects;
    }

    /**
     * handle upsert on non root-level entities
     * @param idFieldName id field name
     * @param input input data object
     * @param dictionary entity dictionary
     * @param parentSource parent persistent resource object
     * @param field graphql-java field type
     * @param outputType graphql-java output type
     * @return list/set of persistent resource objects
     */
    private Object upsertObject(String idFieldName, Map<String, Object> input, EntityDictionary dictionary,
                                PersistentResource parentSource, Field field, GraphQLType outputType,
                                RequestScope requestScope) {
        /* fetch id valued fed to 'data' argument */
        Set<String> ids = input.entrySet().stream().filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                .map(entry -> (String) entry.getValue())
                .collect(Collectors.toSet());

        try {
            Object fetchEntries = fetchObject(dictionary, parentSource, field, outputType.getClass().getName());
            if(fetchEntries == null) {
                return createObject(input, outputType, requestScope, Optional.of(ids), field, parentSource);
            } else {
                return updateObject(); //TODO: implement update
            }
        } catch (BadRequestException e) {
            log.debug(e.toString());
            return createObject(input, outputType, requestScope, Optional.of(ids), field, parentSource);
        }
    }

    /**
     * creates or updates a new/existing entity
     * @param idFieldName field name
     * @param input input data object
     * @param requestScope request scope
     * @param outputType graphql-java output type
     * @return list/set of persistent resource objects
     */
    private Object upsertObject(String idFieldName, Map<String, Object> input,
                                RequestScope requestScope,  Field field, GraphQLType outputType) {
        /* fetch id values fed to 'data' argument */
        Set<String> ids = input.entrySet().stream().filter(entry -> idFieldName.equalsIgnoreCase(entry.getKey()))
                .map(entry -> (String) entry.getValue())
                .collect(Collectors.toSet());
        try {
            Set<PersistentResource> fetchEntries = (Set) fetchObject(requestScope, outputType,
                    Optional.of(new ArrayList<>(ids)), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty());
            if(fetchEntries.isEmpty()) { /* empty set returned, must create new */
                return createObject(input, outputType, requestScope, Optional.of(ids), field, null);
            } else { /* must update object */
                return updateObject(); //TODO: implement update
            }
        } catch(BadRequestException e) { /* no id(s) provided, must create new */
            log.debug(e.toString());
            return createObject(input, outputType, requestScope, Optional.empty(), field, null);
        }
    }

    /**
     * Creates a new "root-level" resource in the database
     * @param input individual input data objects
     * @param outputType graphql-java output type
     * @param requestScope request scope
     * @param ids id
     * @return persistent resource object
     */
    private Object createObject(Map<String, Object> input, GraphQLType outputType,
                                RequestScope requestScope, Optional<Set<String>> ids,
                                Field field, PersistentResource parent) {
        GraphQLObjectType objectType = (GraphQLObjectType) ((GraphQLList) outputType).getWrappedType();
        EntityDictionary dictionary = requestScope.getDictionary();

        Optional<String> uuid = ids.map(strings -> strings.iterator().next());
        Class<?> entityClass;
        PersistentResource toCreate;

        if(parent == null) {
            entityClass = dictionary.getEntityClass(objectType.getName());
            toCreate = PersistentResource.createObject(null, entityClass, requestScope, uuid);

        } else {
            String idFieldName = dictionary.getIdFieldName(dictionary.getEntityClass(field.getName()));
            entityClass = dictionary.getParameterizedType(parent.getResourceClass(), field.getName());
            Optional<String> parentId = Optional.ofNullable(String.valueOf(PersistentResource.getValue(parent.getObject(),
                    idFieldName, requestScope)));
            toCreate = PersistentResource.createObject(parent, entityClass,  requestScope, parentId);
        }

        input.entrySet().stream()
                .filter(entry -> dictionary.isAttribute(entityClass, entry.getKey()))
                .forEach(entry -> toCreate.updateAttribute(entry.getKey(), entry.getValue()));

        return toCreate;
    }

    /** stub code **/
    private Object updateObject() { return null; }

    private Object replaceObjects(Environment context) { return null; }

    private Object removeObjects(Environment context) { return null; }

    private Object deleteObjects(Environment context) { return null; }
}