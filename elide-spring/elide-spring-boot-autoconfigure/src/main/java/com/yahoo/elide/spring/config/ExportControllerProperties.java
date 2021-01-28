/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.config;

import lombok.Data;

/**
 * Extra controller properties for the export endpoint.
 */
@Data
public class ExportControllerProperties extends ControllerProperties {

    /**
     * Skip including Header in CSV formatted export.
     */
    private boolean skipCSVHeader = false;

    /**
     * The URL path prefix for the controller.
     */
    private String path = "/export";
}
