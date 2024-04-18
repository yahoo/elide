/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.Elide;
import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.core.request.EntityProjection;

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
