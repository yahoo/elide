/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.document.processors;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.jsonapi.models.JsonApiDocument;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

/**
 * A Document Processor that add requested relations to the include block of teh JsonApiDocument.
 */
public class IncludedProcessor implements DocumentProcessor {

    private final static String RELATION_PATH_DELIMITER = "\\.";
    private final static String RELATION_PATH_SEPARATOR = ",";

    /**
     * If the include query param is present, this processor will add the requested relations resources
     * to the included block of the JsonApiDocument.
     */
    @Override
    public void execute(JsonApiDocument jsonApiDocument, PersistentResource resource,
                        Optional<MultivaluedMap<String, String>> queryParams) {
        if (isPresent(queryParams, "include")) {
            addIncludedResources(jsonApiDocument, resource, queryParams.get().get("include"));
        }
    }

    /**
     * If the include query param is present, this processor will add the requested relations resources
     * to the included block of the JsonApiDocument.
     */
    @Override
    public void execute(JsonApiDocument jsonApiDocument, Set<PersistentResource> resources,
                        Optional<MultivaluedMap<String, String>> queryParams) {
        if (isPresent(queryParams, "include")) {

            // Process include for each resource
            resources.forEach(resource ->
                    addIncludedResources(jsonApiDocument, resource, queryParams.get().get("include")));
        }
    }

    /**
     * Adds the requested relation resources to the included block of the JsonApiDocument.
     */
    private void addIncludedResources(JsonApiDocument jsonApiDocument, PersistentResource rec,
            List<String> requestedRelationPaths) {
        // Process each include relation path
        requestedRelationPaths.forEach(pathParam -> {
            List<String> pathList = Arrays.asList(pathParam.split(RELATION_PATH_SEPARATOR));

            pathList.forEach(requestedRelationPath -> {
                List<String> relationPath =
                        Lists.newArrayList(requestedRelationPath.split(RELATION_PATH_DELIMITER));
                addResourcesForPath(jsonApiDocument, rec, relationPath);
            });
        });
    }

    /**
     * Adds all the relation resources for a given relation path to the included block of the
     * JsonApiDocument.
     */
    private void addResourcesForPath(JsonApiDocument jsonApiDocument, PersistentResource<?> rec,
                                     List<String> relationPath) {

        //Pop off a relation of relation path
        String relation = relationPath.remove(0);

        rec.getRelation(relation).forEach(resource -> {
            jsonApiDocument.addIncluded(resource.toResource());

            //If more relations left in the path, process a level deeper
            if (!relationPath.isEmpty()) {
                //Use a copy of the relationPath to preserve the path for remaining branches of the relationship tree
                addResourcesForPath(jsonApiDocument, resource, new ArrayList(relationPath));
            }
        });
    }

    private boolean isPresent(Optional<MultivaluedMap<String, String>> queryParams, String key) {
        return (queryParams.isPresent() && queryParams.get().get(key) != null);
    }
}
