/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.filter.dialect.ParseException;
import com.yahoo.elide.core.filter.dialect.jsonapi.MultipleFilterDialect;
import com.yahoo.elide.core.request.route.Route;
import com.yahoo.elide.core.security.User;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import lombok.Getter;

import java.util.LinkedHashMap;
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
        this.mapper = elideSettings.getMapper();
        this.updateStatusCode = elideSettings.getUpdateStatusCode();

        this.filterDialect = new MultipleFilterDialect(elideSettings.getJoinFilterDialects(),
                elideSettings.getSubqueryFilterDialects());

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
        setEntityProjection(new EntityProjectionMaker(outerRequestScope.getElideSettings().getDictionary(), this)
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
}
