/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.spring.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extra controller properties for the export endpoint.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExportControllerProperties extends ControllerProperties {
    @Data
    public static class Format {
        @Data
        public static class Csv {
            /**
             * Generates the header in a CSV formatted export.
             *
             * Set to false to skip writing the header.
             */
            private boolean writeHeader = true;
        }
        private Csv csv = new Csv();
    }

    private Format format = new Format();

    /**
     * Enable Adding Extension to table export attachments.
     */
    private boolean appendFileExtension = false;

    /**
     * The URL path prefix for the controller.
     */
    private String path = "/export";

    /**
     * Storage engine destination.
     */
    private String storageDestination = "/tmp";
}
