/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/***
 * Default API links populate provide 'self' and 'related' links.
 * To add custom links, Override `getResourceLevelLinks` and `getRelationshipLinks`.
 * Ad the subclass object in ElideAutoConfiguration.
 */
public class DefaultJSONApiLinks implements JSONApiLinks {

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
        if (resource.getRequestScope().getBaseUrlEndPoint() != null) {
            result.append(resource.getRequestScope().getBaseUrlEndPoint());
        }
        for (PersistentResource resourcePath : resource.getLineage().getResourcePath()) {
            result.append(String.join("/", resourcePath.getType(), resourcePath.getId()));
        }
        result.append(String.join("/", resource.getType(), resource.getId()));
        return result.toString();
    }
}
