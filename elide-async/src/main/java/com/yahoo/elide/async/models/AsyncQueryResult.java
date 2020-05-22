/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * Model for Async Query Result.
 */
@Embeddable
@Data

public class AsyncQueryResult {

    private Integer contentLength;

    private String responseBody; //success or errors

    private Integer httpStatus; // HTTP Status

    @Column(columnDefinition = "enum('EMBEDDED')")
    @Enumerated(EnumType.STRING)
    private ResultType resultType;

    Date completedOn = new Date();

}
