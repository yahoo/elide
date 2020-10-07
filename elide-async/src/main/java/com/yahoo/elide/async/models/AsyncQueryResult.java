/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;

import javax.persistence.Embeddable;

/**
 * Model for Async Query Result.
 */
@Embeddable
@Data
public class AsyncQueryResult extends AsyncAPIResult {
    private Integer contentLength;

    private String responseBody;  //URL or Response body
}
