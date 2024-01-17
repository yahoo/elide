/*
 * Copyright 2023, Yahoo Inc.
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
 * XLSX output format implementation.
 */
public class XlsxExportFormatter implements TableExportFormatter {

    private boolean writeHeader = true;
    private ObjectMapper objectMapper;

    public XlsxExportFormatter(Elide elide, boolean writeHeader) {
        this.writeHeader = writeHeader;
        this.objectMapper = elide.getObjectMapper();
    }

    @Override
    public ResourceWriter newResourceWriter(OutputStream outputStream, EntityProjection entityProjection,
            TableExport tableExport) {
        return new XlsxResourceWriter(outputStream, objectMapper, writeHeader, entityProjection);
    }
}
