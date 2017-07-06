/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.DataStoreTransaction;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.exceptions.UnknownEntityException;
import graphql.GraphQL;
import graphql.language.Field;
import graphql.schema.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import java.util.*;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ARGUMENT_OPERATION;

@Slf4j
public class PersistentResourceFetcher implements DataFetcher {
    private final ElideSettings settings;

    public PersistentResourceFetcher(ElideSettings settings) { this.settings = settings; }

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
            throw new IllegalArgumentException("Pagination/Filtering/Sorting is only supported with FETCH operation");
        }
    }

    /**
     * log current context for debugging
     * @param operation current operation
     * @param environment environment encapsulating graphQL's request environment
     */
    private void logContext(RelationshipOp operation, Environment environment) {
        List<Field> children = environment.field.getSelectionSet() != null
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
        if(context.data.isPresent()) throw new BadRequestException("FETCH must not include data");

        /* check whether current object has a parent or not */
        if (context.isRoot() || context.outputType instanceof GraphQLList) {
            /* top level entity */
            GraphQLObjectType graphQLType = (GraphQLObjectType) ((GraphQLList) context.outputType).getWrappedType();
            String entityType = graphQLType.getName();
            Class recordType = context.requestScope.getDictionary().getEntityClass(entityType);

            if (recordType == null) {
                throw new UnknownEntityException(entityType);
            }

            return fetchObject(context.requestScope, recordType, context.ids, context.sort,
                    context.offset, context.first, context.filters);
        } else {
            /* fetch properties */
            return fetchObject(context.requestScope, context.parentResource, context.field,
                    context.outputType, context.ids, context.sort, context.offset, context.first, context.filters);
        }
    }

    /**
     * FETCH top level entity either by ids or as a collection
     * @param requestScope request scope
     * @param recordType record type
     * @param ids list of ids (can be NULL)
     * @param sort sort by ASC/DESC
     * @param offset pagination offset argument
     * @param first pagination first argument
     * @param filters filter params
     * @return list of persistent resource objects
     */
    private Object fetchObject(RequestScope requestScope, Class recordType, Optional<List<String>> ids,
                               Optional<String> sort, Optional<String> offset,
                               Optional<String> first, Optional<String> filters) {
        /* fetching a collection */
        if(!ids.isPresent()) {
            List<PersistentResource> records = new ArrayList<>(PersistentResource.
                    loadRecords(recordType, requestScope));

            //TODO: paginate/filter/sort
            return records;

        } else { /* fetching by id */
            List<String> idList = ids.get();

            /* handle empty list of ids */
            if(idList.isEmpty()) throw new IllegalArgumentException("Empty list passed to ids");

            /* access records from internal db and return */
            HashSet recordSet = new HashSet();
            for (String id : idList) {
                recordSet.add(PersistentResource.loadRecord(recordType, id, requestScope));
            }
            return recordSet;
        }
    }

    /**
     * FETCH attributes of top level entity
     * @param requestScope request scope
     * @param parentResource parent object
     * @param field Field type
     * @param outputType GraphQL object output type
     * @param ids list of ids
     * @param sort sort by ASC/DESC
     * @param offset pagination offset argument
     * @param first pagination first argument
     * @param filters filter params
     * @return list of persistent resource objects
     */
    private Object fetchObject(RequestScope requestScope, PersistentResource parentResource, Field field,
                               GraphQLType outputType, Optional<List<String>> ids,
                               Optional<String> sort, Optional<String> offset,
                               Optional<String> first, Optional<String> filters) {

        //TODO: Do we need P/F/S? If so, do it!

        if(outputType instanceof GraphQLObjectType) {
            DataStoreTransaction tx = requestScope.getTransaction();
            Object obj =
                    tx.getRelation(tx, parentResource.getObject(), field.getName(),
                            null, null, null, requestScope);
            return new PersistentResource(obj, parentResource, requestScope.getUUIDFor(obj), requestScope);
        }

        else if(outputType instanceof GraphQLScalarType || outputType instanceof GraphQLEnumType) {
            EntityDictionary dictionary = requestScope.getDictionary();

            if (dictionary.isAttribute(parentResource.getResourceClass(), field.getName())) {
                return parentResource.getAttribute(field.getName());
            } else {
                log.debug("Tried to fetch property off of invalid loaded object.");
                throw new InvalidAttributeException(field.getName(), parentResource.getType());
            }
        }

        throw new IllegalStateException("Unrecognized object type: " + outputType.getClass().getName());
    }

    private Object replaceObjects(Environment context) { return null; }

    private Object upsertObjects(Environment context) { return null; }

    private Object removeObjects(Environment context) { return null; }

    private Object deleteObjects(Environment context) { return null; }
}
