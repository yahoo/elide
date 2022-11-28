/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model for Async Query Result.
 */
@Embeddable
@Data
@EqualsAndHashCode(callSuper = true)
public class AsyncQueryResult extends AsyncAPIResult {
    private Integer contentLength;

    @Lob
    private String responseBody;  //URL or Response body
}
