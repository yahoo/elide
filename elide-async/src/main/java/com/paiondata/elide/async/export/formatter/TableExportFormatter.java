/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.async.models.TableExport;
import com.paiondata.elide.core.request.EntityProjection;

import java.io.OutputStream;

/**
 * Interface which is used to format PersistentResource to output format.
 */
public interface TableExportFormatter {

    /**
     * Factory method to create the resource writer used to generate this format.
     *
     * @param outputStream the output stream to write to
     * @param entityProjection the entity projection
     * @param tableExport the table export
     * @return the resource writer
     */
    ResourceWriter newResourceWriter(OutputStream outputStream, EntityProjection entityProjection,
            TableExport tableExport);
}
