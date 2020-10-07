/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import lombok.Data;

import java.util.Date;

import javax.persistence.MappedSuperclass;

/**
 * Base Model for Async Query Result.
 */
@MappedSuperclass
@Data
public abstract class AsyncAPIResult {

    private Integer recordCount;

    private Integer httpStatus; // HTTP Status

    private Date completedOn = new Date();
}
