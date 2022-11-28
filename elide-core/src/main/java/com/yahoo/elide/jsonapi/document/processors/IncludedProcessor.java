/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.exceptions.ForbiddenAccessException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.jsonapi.EntityProjectionMaker;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

/**
 * A Document Processor that add requested relations to the include block of the JsonApiDocument.
 */
public class IncludedProcessor implements DocumentProcessor {
    private static final String RELATION_PATH_DELIMITER = "\\.";
    private static final String RELATION_PATH_SEPARATOR = ",";
    private static final String INCLUDE = "include";

    /**
     * If the include query param is present, this processor will add the requested relations resources
     * to the included block of the JsonApiDocument.
     */
    @Override
    public void execute(JsonApiDocument jsonApiDocument, RequestScope scope, PersistentResource resource,
                        MultivaluedMap<String, String> queryParams) {
        if (isPresent(queryParams, INCLUDE)) {
            addIncludedResources(jsonApiDocument, resource, queryParams.get(INCLUDE));
        }
    }

    /**
     * If the include query param is present, this processor will add the requested relations resources
     * to the included block of the JsonApiDocument.
     */
    @Override
    public void execute(
            JsonApiDocument jsonApiDocument,
            RequestScope scope,
            LinkedHashSet<PersistentResource> resources,
            MultivaluedMap<String, String> queryParams
    ) {
        if (isPresent(queryParams, INCLUDE)) {

            // Process include for each resource
            resources.forEach(resource ->
                    addIncludedResources(jsonApiDocument, resource, queryParams.get(INCLUDE)));
        }
    }

    /**
     * Adds the requested relation resources to the included block of the JsonApiDocument.
     */
    private void addIncludedResources(JsonApiDocument jsonApiDocument, PersistentResource rec,
            List<String> requestedRelationPaths) {

        EntityProjectionMaker maker = new EntityProjectionMaker(rec.getDictionary(), rec.getRequestScope());
        EntityProjection projection = maker.parseInclude(rec.getResourceType());
        // Process each include relation path
        requestedRelationPaths.forEach(pathParam -> {
            List<String> pathList = Arrays.asList(pathParam.split(RELATION_PATH_SEPARATOR));

            pathList.forEach(requestedRelationPath -> {
                List<String> relationPath = Lists.newArrayList(requestedRelationPath.split(RELATION_PATH_DELIMITER));
                addResourcesForPath(jsonApiDocument, rec, relationPath, projection);
            });
        });
    }

    /**
     * Adds all the relation resources for a given relation path to the included block of the
     * JsonApiDocument.
     */
    private void addResourcesForPath(JsonApiDocument jsonApiDocument, PersistentResource<?> rec,
                                     List<String> relationPath,
                                     EntityProjection projection) {

        //Pop off a relation of relation path
        String relation = relationPath.remove(0);

        Set<PersistentResource> collection;
        Relationship relationship = projection.getRelationship(relation).orElseThrow(IllegalStateException::new);
        try {
            collection = rec.getRelationCheckedFiltered(relationship).toList(LinkedHashSet::new).blockingGet();

        } catch (ForbiddenAccessException e) {
            return;
        }

        collection.forEach(resource -> {
            jsonApiDocument.addIncluded(resource.toResource());

            //If more relations left in the path, process a level deeper
            if (!relationPath.isEmpty()) {
                //Use a copy of the relationPath to preserve the path for remaining branches of the relationship tree
                addResourcesForPath(jsonApiDocument, resource, new ArrayList<>(relationPath),
                        relationship.getProjection());
            }
        });
    }

    private static boolean isPresent(MultivaluedMap<String, String> queryParams, String key) {
        return queryParams != null && queryParams.get(key) != null;
    }
}
