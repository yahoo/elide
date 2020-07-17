/*
 * Copyright 2015, Yahoo Inc.
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
public class DefaultJSONApiLinks {
    /**
     * Links to be used in EntityProjection
     * @param resource
     * @return
     */
    public Map<String, String> getResourceLevelLinks(PersistentResource resource) {
        return ImmutableMap.of("self", getResourceUrl(resource));
    }

    /**
     * Links to be used in Relationships of EntityProjection
     * @param resource
     * @return
     */
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
        for (PersistentResource resourcePath : resource.getLineage().getResourcePath()) {
            result.append(String.join("/", resourcePath.getType(), resourcePath.getId()));
        }
        result.append(String.join("/", resource.getType(), resource.getId()));
        return result.toString();
    }
}
