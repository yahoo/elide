/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;

import java.net.URL;

import javax.persistence.Embeddable;

/**
 * Model for Table Export Result.
 */
@Embeddable
@Data
public class TableExportResult extends AsyncAPIResult {
    private URL url;

    private String message;
}
