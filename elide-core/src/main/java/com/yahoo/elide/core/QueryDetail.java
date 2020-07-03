/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryDetail implementation class. Stores the model name (root entity class) and
 * the queryText that the underlying datastore will eventually run
 */
@Value
public class QueryDetail {
    private String modelName;
    private List<String> queryText;

    public QueryDetail() {
        modelName = "";
        queryText = new ArrayList<String>();

        queryText.add("");
    }

    public QueryDetail(String modelName) {
        this.modelName = modelName;
        queryText = new ArrayList<String>();

        queryText.add("");
    }

    public QueryDetail(String modelName, List<String> queryText) {
        this.modelName = modelName;
        this.queryText = queryText;
    }
}
