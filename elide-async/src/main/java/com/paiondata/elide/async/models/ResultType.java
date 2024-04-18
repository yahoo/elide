/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.models;

/**
 * Result type for table export.
 */
public abstract class ResultType {
    public static final String JSON = "JSON";
    public static final String CSV = "CSV";
    public static final String XLSX = "XLSX";
}
