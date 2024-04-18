/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.models;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.URL;

/**
 * Model for Table Export Result.
 */
@Embeddable
@Data
@EqualsAndHashCode(callSuper = true)
public class TableExportResult extends AsyncApiResult {
    private URL url;

    @Lob
    private String message;
}
