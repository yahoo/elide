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
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                        JsonApiDocument jsonApiDocument
                        ) {
        super(route, transaction, user, requestId, elideSettings);
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
            MultivaluedMap<String, String> filterParams = getFilterParams(queryParams);

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
    private static MultivaluedMap<String, String> getFilterParams(Map<String, List<String>> queryParams) {
        MultivaluedMap<String, String> returnMap = new MultivaluedHashMap<>();

        queryParams.entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith("filter"))
                .forEach(entry -> returnMap.put(entry.getKey(), entry.getValue()));
        return returnMap;
    }
}
