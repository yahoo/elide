/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Embeddable;

/**
 * Model for Async Query Result.
 */
@Embeddable
@Data

public class AsyncQueryResult {

    private Integer contentLength;

    private Integer recordCount;

    private String responseBody;  //URL or Response body

    private Integer httpStatus; // HTTP Status

    private Date completedOn = new Date();

    private Blob attachment; // To allow expansion to XLSX?

}
