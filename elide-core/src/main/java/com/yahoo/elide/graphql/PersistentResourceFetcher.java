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

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

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
            return fetchObject(context);

        } else {
            EntityDictionary dictionary = context.requestScope.getDictionary();
            Class parentClass = context.parentResource.getResourceClass();
            String fieldName = context.field.getName();

            if(dictionary.isAttribute(parentClass, fieldName)) { /* fetch attribute properties */
                return fetchAttribute(context.parentResource, context.field);

            } else if(dictionary.isRelation(parentClass, fieldName)){ /* fetch relationship properties */
                return fetchRelationship(context);
            } else {
                throw new BadRequestException("Unrecognized object type: " + context.outputType.getClass().getName());
            }
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
     * Fetches an entity, can be root or a nested entity
     * @param context environment encapsulating graphQL's request environment
     * @return persistent resource object(s)
     */
    private Object fetchObject(Environment context) {
        Class recordType = getRecordType(context.requestScope, context.outputType);
        return fetchObject(context.requestScope, recordType, context.ids, context.sort,
                context.offset, context.first, context.filters);
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
                    loadRecords(recordType, requestScope, Optional.empty()));

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
     * @param context environment encapsulating graphQL's request environment
     * @return persistence resource object(s)
     */
    private Object fetchRelationship(Environment context) {
        String fieldName = context.field.getName();

        Set<PersistentResource> relations = context.parentResource.getRelationCheckedFiltered(fieldName);

        /* check for toOne relationships */
        Boolean isToOne = context.parentResource.getRelationshipType(fieldName).isToOne();

        if(isToOne) {
            return relations.iterator().next();
        } else {
            return relations;
        }
    }

    /** stub code **/
    private Object replaceObjects(Environment context) { return null; }

    private Object upsertObjects(Environment context) { return null; }

    private Object removeObjects(Environment context) { return null; }

    private Object deleteObjects(Environment context) { return null; }
}
