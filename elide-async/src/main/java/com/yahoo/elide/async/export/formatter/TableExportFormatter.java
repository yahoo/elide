/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.EntityProjection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Interface which is used to format PersistentResource to output format.
 */
public interface TableExportFormatter {

    /**
     * Creates context for storing state during the table export.
     * <p>
     * The context can be sub-classed to store additional state needed by the table
     * export formatter during processing.
     *
     * @param entityProjection the projection
     * @param tableExport the query
     * @param recordNumberSupplier the record number processed
     * @return the context
     */
    default TableExportFormatterContext createContext(EntityProjection entityProjection, TableExport tableExport,
            Supplier<Integer> recordNumberSupplier) {
        return new TableExportFormatterContext(entityProjection, tableExport, recordNumberSupplier);
    }

    /**
     * Format PersistentResource.
     * @param context the TableExportFormatterContext.
     * @param resource PersistentResource to format
     * @param outputStream to write
     */
    void format(TableExportFormatterContext context, PersistentResource<?> resource, OutputStream outputStream)
            throws IOException;

    /**
     * Pre Format Action.
     * Example: Generate Header, Metadata etc.
     * @param context the TableExportFormatterContext.
     * @param outputStream to write
     */
    default void preFormat(TableExportFormatterContext context, OutputStream outputStream) throws IOException {
    }

    /**
     * Post Format Action.
     * Example: Generate Metadata, Stats etc.
     * @param context the TableExportFormatterContext.
     * @param outputStream to write
     */
    default void postFormat(TableExportFormatterContext context, OutputStream outputStream) throws IOException {
    }
}
