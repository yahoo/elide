/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.document.processors;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.RequestScope;
import com.paiondata.elide.jsonapi.models.JsonApiDocument;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * An interface for building and processing a response document.
 */
public interface DocumentProcessor {

    //TODO Possibly add a 'beforeExecute' method for setup

    /**
     * A method for making transformations to the JsonApiDocument.
     *
     * @param jsonApiDocument the json api document
     * @param scope the request scope
     * @param resource the resource
     * @param queryParams the query params
     */
    void execute(JsonApiDocument jsonApiDocument,
                 RequestScope scope,
                 PersistentResource resource,
                 Map<String, List<String>> queryParams);

    /**
     * A method for making transformations to the JsonApiDocument.
     *
     * @param jsonApiDocument the json api document
     * @param scope the request scope
     * @param resources the resources
     * @param queryParams the query params
     */
    void execute(JsonApiDocument jsonApiDocument,
                 RequestScope scope,
                 LinkedHashSet<PersistentResource> resources,
                 Map<String, List<String>> queryParams);

    //TODO Possibly add a something like a 'afterExecute' method to process after the first round of execution
}
