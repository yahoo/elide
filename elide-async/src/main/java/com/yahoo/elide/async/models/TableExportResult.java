/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.URL;

import javax.persistence.Embeddable;
import javax.persistence.Lob;

/**
 * Model for Table Export Result.
 */
@Embeddable
@Data
@EqualsAndHashCode(callSuper = true)
public class TableExportResult extends AsyncAPIResult {
    private URL url;

    @Lob
    private String message;
}
