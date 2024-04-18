/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for processing OpenAPI.
 */
public class OpenApis {
    private OpenApis() {
    }

    public static void removePathsByTags(OpenAPI openApi, String... tagsToRemove) {
        Set<String> tags = new HashSet<>();
        Collections.addAll(tags, tagsToRemove);
        removePathsByTags(openApi, tags);
    }

    public static void removePathsByTags(OpenAPI openApi, Set<String> tagsToRemove) {
        Set<String> pathsToRemove = new HashSet<>();
        Paths paths = openApi.getPaths();
        if (paths != null) {
            openApi.getPaths().forEach((path, pathItem) -> {
                Set<String> tags = new HashSet<>();
                if (pathItem.getGet() != null) {
                    tags.addAll(pathItem.getGet().getTags());
                } else if (pathItem.getPost() != null) {
                    tags.addAll(pathItem.getPost().getTags());
                }
                for (String tagToRemove : tagsToRemove) {
                    if (tags.contains(tagToRemove)) {
                        pathsToRemove.add(path);
                        break;
                    }
                }
            });
        }
        pathsToRemove.forEach(key -> openApi.getPaths().remove(key));
    }
}
