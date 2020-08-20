/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import java.util.Map;

public interface JSONApiLinks {
    /**
     * Links to be used in the Respose Entity
     * @param resource
     * @return
     */
    Map<String, String> getResourceLevelLinks(PersistentResource resource);

    /**
     * Links to be used in Relationships of the Response Entity
     * @param resource
     * @return
     */
    Map<String, String> getRelationshipLinks(PersistentResource resource, String field);
}
