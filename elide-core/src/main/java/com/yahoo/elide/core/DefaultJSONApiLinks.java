/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

/***
 * Default API links populate provide 'self' and 'related' links.
 * To add custom links, Override `getResourceLevelLinks` and `getRelationshipLinks`.
 * Ad the subclass object in ElideAutoConfiguration.
 */
public class DefaultJSONApiLinks implements JSONApiLinks {

    private final String baseUrl;

    public DefaultJSONApiLinks() {
        this("");
    }

    public DefaultJSONApiLinks(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Map<String, String> getResourceLevelLinks(PersistentResource resource) {
        return ImmutableMap.of("self", getResourceUrl(resource));
    }

    @Override
    public Map<String, String> getRelationshipLinks(PersistentResource resource, String field) {
        String resourceUrl = getResourceUrl(resource);
        return ImmutableMap.of(
                "self", String.join("/", resourceUrl, "relationships", field),
                "related", String.join("/", resourceUrl, field));
    }

    /**
     * Creates the link from resources path
     * @param resource
     * @return
     */
    protected String getResourceUrl(PersistentResource resource) {
        StringBuilder result = new StringBuilder();

        if (baseUrl == null || baseUrl.isEmpty()) {
            if (resource.getRequestScope().getBaseUrlEndPoint() != null) {
                result.append(resource.getRequestScope().getBaseUrlEndPoint());
            }
        } else {
            result.append(baseUrl);
        }

        List<ResourceLineage.LineagePath> path = resource.getLineage().getResourcePath();
        if (path.size() > 0) {
            result.append(String.join("/", getPathSegment(path), resource.getId()));
        } else {
            result.append(String.join("/", resource.getType(), resource.getId()));
        }

        return result.toString();
    }

    private String getPathSegment(List<ResourceLineage.LineagePath> path) {
        StringBuilder result = new StringBuilder();

        int pathSegmentCount = 0;
        for (ResourceLineage.LineagePath pathElement : path) {
            PersistentResource resource = pathElement.getResource();
            if (pathSegmentCount > 0) {
                result.append("/");
                result.append(String.join("/",
                        resource.getId(), pathElement.getRelationship()));
            } else {
                result.append(String.join("/",
                        resource.getType(), resource.getId(), pathElement.getRelationship()));
            }
            pathSegmentCount++;
        }

        return result.toString();
    }
}
