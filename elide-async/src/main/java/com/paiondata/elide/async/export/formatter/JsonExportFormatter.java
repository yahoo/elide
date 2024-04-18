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
public class JsonExportFormatter implements TableExportFormatter {
    private ObjectMapper mapper;

    public JsonExportFormatter(Elide elide) {
        this.mapper = elide.getObjectMapper();
    }

    @Override
    public ResourceWriter newResourceWriter(OutputStream outputStream, EntityProjection entityProjection,
            TableExport tableExport) {
        return new JsonResourceWriter(outputStream, mapper);
    }
}
