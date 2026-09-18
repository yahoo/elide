/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.exceptions.InvalidAttributeException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Request scope object for relaying request-related data to various subsystems.
 */
public class JsonApiRequestScope extends RequestScope {
    @Getter private final JsonApiDocument jsonApiDocument;
    @Getter private final JsonApiMapper mapper;
    @Getter private final int updateStatusCode;
    @Getter private final MultipleFilterDialect filterDialect;
    @Getter private final Map<String, Set<String>> sparseFields;

    protected Map<String, FilterExpression> expressionsByType;

    /* Used to filter across heterogeneous types during the first load */
    protected FilterExpression globalFilterExpression;

    /**
     * Create a new RequestScope.
     *
     * @param route         the route
     * @param transaction   the transaction for this request
     * @param user          the user making this request
     * @param requestId     request ID
     * @param elideSettings Elide settings object
     */
    public JsonApiRequestScope(Route route,
                        DataStoreTransaction transaction,
                        User user,
                        UUID requestId,
                        ElideSettings elideSettings,
                        Function<RequestScope, EntityProjection> entityProjection,
                        JsonApiDocument jsonApiDocument
                        ) {
        super(route, transaction, user, requestId, elideSettings, entityProjection);
        this.jsonApiDocument = jsonApiDocument;

        this.globalFilterExpression = null;
        this.expressionsByType = new LinkedHashMap<>();
        this.sparseFields = parseSparseFields(getRoute().getParameters());

        JsonApiSettings jsonApiSettings = elideSettings.getSettings(JsonApiSettings.class);
        this.mapper = jsonApiSettings.getJsonApiMapper();
        this.updateStatusCode = jsonApiSettings.getUpdateStatusCode();

        List<JoinFilterDialect> joinFilterDialects = new ArrayList<>(jsonApiSettings.getJoinFilterDialects());
        List<SubqueryFilterDialect> subqueryFilterDialects = new ArrayList<>(
                jsonApiSettings.getSubqueryFilterDialects());

        EntityDictionary entityDictionary = elideSettings.getEntityDictionary();

        if (joinFilterDialects.isEmpty()) {
            joinFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            joinFilterDialects.add(RSQLFilterDialect.builder().dictionary(entityDictionary).build());
        }

        if (subqueryFilterDialects.isEmpty()) {
            subqueryFilterDialects.add(new DefaultFilterDialect(entityDictionary));
            subqueryFilterDialects.add(RSQLFilterDialect.builder().dictionary(entityDictionary).build());
        }

        this.filterDialect = new MultipleFilterDialect(joinFilterDialects,
                subqueryFilterDialects);

        Map<String, List<String>> queryParams = getRoute().getParameters();
        String path = route.getPath();
        String apiVersion = route.getApiVersion();

        if (!queryParams.isEmpty()) {

            /* Extract any query param that starts with 'filter' */
            Map<String, List<String>> filterParams = getFilterParams(queryParams);

            String errorMessage = "";
            if (! filterParams.isEmpty()) {

                /* First check to see if there is a global, cross-type filter */
                try {
                    globalFilterExpression = filterDialect.parseGlobalExpression(path, filterParams, apiVersion);
                } catch (ParseException e) {
                    errorMessage = e.getMessage();
                }

                /* Next check to see if there is are type specific filters */
                try {
                    expressionsByType.putAll(filterDialect.parseTypedExpression(path, filterParams, apiVersion));
                } catch (ParseException e) {

                    /* If neither dialect parsed, report the last error found */
                    if (globalFilterExpression == null) {

                        if (errorMessage.isEmpty()) {
                            errorMessage = e.getMessage();
                        } else if (! errorMessage.equals(e.getMessage())) {

                            /* Combine the two different messages together */
                            errorMessage = errorMessage + "\n" + e.getMessage();
                        }

                        throw new BadRequestException(errorMessage, e);
                    }
                }
            }
        }
    }

    /**
     * Special copy constructor for use by PatchRequestScope.
     *
     * @param route             the route
     * @param jsonApiDocument   the json api document
     * @param outerRequestScope the outer request scope
     */
    protected JsonApiRequestScope(Route route, JsonApiDocument jsonApiDocument, JsonApiRequestScope outerRequestScope) {
        super(outerRequestScope);
        this.route = route;
        this.jsonApiDocument = jsonApiDocument;
        this.sparseFields = outerRequestScope.sparseFields;
        this.expressionsByType = outerRequestScope.expressionsByType;
        this.globalFilterExpression = outerRequestScope.globalFilterExpression;
        setEntityProjection(new EntityProjectionMaker(outerRequestScope.getElideSettings().getEntityDictionary(), this)
                .parsePath(this.route.getPath()));
        this.updateStatusCode = outerRequestScope.getUpdateStatusCode();
        this.mapper = outerRequestScope.getMapper();
        this.filterDialect = outerRequestScope.getFilterDialect();
    }

    /**
     * Parses queryParams and produces sparseFields map.
     * @param queryParams The request query parameters
     * @return Parsed sparseFields map
     */
    public static Map<String, Set<String>> parseSparseFields(Map<String, List<String>> queryParams) {
        Map<String, Set<String>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> kv : queryParams.entrySet()) {
            String key = kv.getKey();
            if (key.startsWith("fields[") && key.endsWith("]")) {
                String type = key.substring(7, key.length() - 1);

                LinkedHashSet<String> filters = new LinkedHashSet<>();
                for (String filterParams : kv.getValue()) {
                    Collections.addAll(filters, filterParams.split(","));
                }

                if (!filters.isEmpty()) {
                    result.put(type, filters);
                }
            }
        }

        return result;
    }

    /**
     * Get filter expression for a specific collection type.
     * @param type The name of the type
     * @return The filter expression for the given type
     */
    public Optional<FilterExpression> getFilterExpressionByType(String type) {
        return Optional.ofNullable(expressionsByType.get(type));
    }

    /**
     * Get filter expression for a specific collection type.
     * @param entityClass The class to lookup
     * @return The filter expression for the given type
     */
    public Optional<FilterExpression> getFilterExpressionByType(Type<?> entityClass) {
        return Optional.ofNullable(expressionsByType.get(dictionary.getTypeName(entityClass)));
    }

    /**
     * Get the global/cross-type filter expression.
     * @param loadClass Entity class
     * @return The global filter expression evaluated at the first load
     */
    public Optional<FilterExpression> getLoadFilterExpression(Type<?> loadClass) {
        Optional<FilterExpression> filterExpression;
        if (globalFilterExpression == null) {
            String typeName = dictionary.getTypeName(loadClass);
            filterExpression =  getFilterExpressionByType(typeName);
        } else {
            filterExpression = Optional.of(globalFilterExpression);
        }
        return filterExpression;
    }

    /**
     * Get the filter expression for a particular relationship.
     * @param parentType The parent type which has the relationship
     * @param relationName The relationship name
     * @return A type specific filter expression for the given relationship
     */
    public Optional<FilterExpression> getExpressionForRelation(Type<?> parentType, String relationName) {
        final Type<?> entityClass = dictionary.getParameterizedType(parentType, relationName);
        if (entityClass == null) {
            throw new InvalidAttributeException(relationName, dictionary.getTypeName(parentType));
        }

        final String valType = dictionary.getTypeName(entityClass);
        return getFilterExpressionByType(valType);
    }

    /**
     * Extracts any query params that start with 'filter'.
     * @param queryParams request query params
     * @return extracted filter params
     */
    private static Map<String, List<String>> getFilterParams(Map<String, List<String>> queryParams) {
        Map<String, List<String>> returnMap = new LinkedHashMap<>();

        queryParams.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("filter"))
                .forEach(entry -> returnMap.put(entry.getKey(), entry.getValue()));
        return returnMap;
    }

    /**
     * Returns a mutable {@link JsonApiRequestScopeBuilder} for building {@link JsonApiRequestScope}.
     *
     * @return the builder
     */
    public static JsonApiRequestScopeBuilder builder() {
        return new JsonApiRequestScopeBuilder();
    }

    /**
     * A mutable builder for building {@link JsonApiRequestScope}.
     */
    public static class JsonApiRequestScopeBuilder extends RequestScopeBuilder {
        protected JsonApiDocument jsonApiDocument;

        public JsonApiRequestScopeBuilder jsonApiDocument(JsonApiDocument jsonApiDocument) {
            this.jsonApiDocument = jsonApiDocument;
            return this;
        }

        @Override
        public JsonApiRequestScope build() {
            applyDefaults();
            return new JsonApiRequestScope(this.route, this.dataStoreTransaction, this.user, this.requestId,
                    this.elideSettings, this.entityProjection, this.jsonApiDocument);
        }

        @Override
        public JsonApiRequestScopeBuilder route(Route route) {
            super.route(route);
            return this;
        }

        @Override
        public JsonApiRequestScopeBuilder dataStoreTransaction(DataStoreTransaction transaction) {
            super.dataStoreTransaction(transaction);
            return this;
        }

        @Override
        public JsonApiRequestScopeBuilder user(User user) {
            super.user(user);
            return this;
        }

        @Override
        public JsonApiRequestScopeBuilder requestId(UUID requestId) {
            super.requestId(requestId);
            return this;
        }

        @Override
        public JsonApiRequestScopeBuilder elideSettings(ElideSettings elideSettings) {
            super.elideSettings(elideSettings);
            return this;
        }

        @Override
        public JsonApiRequestScopeBuilder entityProjection(Function<RequestScope, EntityProjection> entityProjection) {
            super.entityProjection(entityProjection);
            return this;
        }
    }
}
