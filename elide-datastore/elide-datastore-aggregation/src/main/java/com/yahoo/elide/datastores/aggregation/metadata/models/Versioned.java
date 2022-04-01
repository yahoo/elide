/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.metadata.models;

/**
 * Versioned interface for Metadata models.
 */
public interface Versioned {
    public String getVersion();
}
