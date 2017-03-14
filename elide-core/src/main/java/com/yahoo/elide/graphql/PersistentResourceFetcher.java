/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yahoo.elide.graphql.ModelBuilder.ID_ARGUMENT;
import static com.yahoo.elide.graphql.ModelBuilder.OPERATION_ARGUMENT;

/**
 * Interacts with {@link PersistentResource} to fetch data for GraphQL.
 */
@Slf4j
public class PersistentResourceFetcher implements DataFetcher {

    @Override
    public Object get(DataFetchingEnvironment environment) {
        GraphQLContext context = (GraphQLContext) environment.getContext();
        Object source = environment.getSource();
        GraphQLType parent = environment.getParentType();
        GraphQLType output = environment.getFieldType();
        List<Field> fields = environment.getFields();
        Map<String, Object> args = environment.getArguments();

        if (log.isDebugEnabled()) {
            log.debug("fetching {} fields for {} with parent {}<{}>",
                    fields.stream().map(field -> {
                        List<Field> children = field.getSelectionSet() != null
                                ? (List) field.getSelectionSet().getChildren()
                                : new ArrayList<>();
                        return field.getName() + (
                                children.size() > 0
                                        ? "(" + children.stream().map(Field::getName).collect(Collectors.toList()) + ")"
                                        : ""
                        );
                    })
                            .collect(Collectors.toList()), source, parent.getClass().getSimpleName(), parent.getName());
        }

        String id = (String) args.get(ID_ARGUMENT);
        RelationshipOp operation = (RelationshipOp) args.getOrDefault(OPERATION_ARGUMENT, RelationshipOp.FETCH);

        // TODO: Do we really want to return these objects or do we want to store them as PResources and finish later?
        if (RelationshipOp.FETCH.equals(operation)) {
            if (output instanceof GraphQLList) {
                GraphQLObjectType type = (GraphQLObjectType) ((GraphQLList) output).getWrappedType();
                if (id == null) {
                    return loadCollectionOf(type.getName(), context);
                }
                return Arrays.asList(load(type.getName(), id, context));
            } else if (output instanceof GraphQLObjectType) {
                return load(output.getName(), id, context);
            } else if (output instanceof GraphQLScalarType) {
                return fetchProperty((PersistentResource) source, fields, context);
            } else {
                throw new IllegalStateException("WTF mate?");
            }
        } else {
            // TODO: Implement
            throw new UnsupportedOperationException("Not implemented.");
        }
    }

    private Object fetchProperty(PersistentResource source, List<Field> fields, GraphQLContext context) {
        RequestScope requestScope = context.requestScope;
        EntityDictionary dictionary = requestScope.getDictionary();
        Class<?> sourceClass = source.getResourceClass();
        // TODO: When can we have more than one? What should we return in such a case?
        ArrayList<Object> fieldValues = new ArrayList<>();
        for (Field field : fields) {
            String fieldName = field.getName();
            if (dictionary.isAttribute(sourceClass, fieldName)) {
                fieldValues.add(source.getAttribute(fieldName));
            } else if(dictionary.isRelation(sourceClass, fieldName)) {
                // TODO: Fetch id?...
                fieldValues.add(source.getRelation(fieldName, ""));
            } else {
                log.debug("Tried to fetch property off of invalid loaded object.");
                throw new IllegalStateException("Hm. Bad news.");
            }
        }
        return fieldValues;
    }

    protected List<Object> loadCollectionOf(String type, GraphQLContext context) {
        RequestScope requestScope = context.requestScope;
        Class<Object> recordType = (Class) requestScope.getDictionary().getEntityClass(type);
        Set<PersistentResource<Object>> records = PersistentResource.loadRecords(recordType, requestScope);
        return records.stream()
//                .map(PersistentResource::getObject)
                .collect(Collectors.toList());
    }

    protected Object load(String type, String id, GraphQLContext context) {
        RequestScope requestScope = context.requestScope;
        Class<?> recordType = requestScope.getDictionary().getEntityClass(type);
        if (recordType == null) {
            log.debug("No type found for typename: {}", type);
            // TODO: Throw bad request exception here
        }
        PersistentResource resource = PersistentResource.loadRecord(recordType, id, requestScope);
        context.setLastLoaded(resource);
        return resource;
//        return resource.getObject();
    }
}
