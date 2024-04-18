/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.links;

import com.paiondata.elide.core.PersistentResource;

import java.util.Map;

public interface JsonApiLinks {
    /**
     * Links to be used in the Response Entity.
     * @param resource
     * @return
     */
    Map<String, String> getResourceLevelLinks(PersistentResource resource);

    /**
     * Links to be used in Relationships of the Response Entity.
     * @param resource
     * @return
     */
    Map<String, String> getRelationshipLinks(PersistentResource resource, String field);
}
