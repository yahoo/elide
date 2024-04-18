/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.ElideSettings;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.core.datastore.DataStoreTransaction;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.filter.dialect.ParseException;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.security.User;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        setEntityProjection(new EntityProjectionMaker(outerRequestScope.getElideSettings().getEntityDictionary(), this)
                .parsePath(this.route.getPath()));
        this.updateStatusCode = outerRequestScope.getUpdateStatusCode();
        this.mapper = outerRequestScope.getMapper();
        this.filterDialect = outerRequestScope.getFilterDialect();
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
