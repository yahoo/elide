/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.Elide;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.request.EntityProjection;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;

/**
 * JSON output format implementation.
 */
public class CsvExportFormatter implements TableExportFormatter {

    private boolean writeHeader = true;
    private ObjectMapper objectMapper;

    public CsvExportFormatter(Elide elide, boolean writeHeader) {
        this.writeHeader = writeHeader;
        this.objectMapper = elide.getObjectMapper();
    }

    @Override
    public ResourceWriter newResourceWriter(OutputStream outputStream, EntityProjection entityProjection,
            TableExport tableExport) {
        return new CsvResourceWriter(outputStream, objectMapper, writeHeader, entityProjection);
    }
}
