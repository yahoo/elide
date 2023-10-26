/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.EntityProjection;

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
     * @return
     */
    default TableExportFormatterContext createContext(EntityProjection entityProjection, TableExport tableExport,
            Supplier<Integer> recordNumberSupplier) {
        return new TableExportFormatterContext(entityProjection, tableExport, recordNumberSupplier);
    }

    /**
     * Format PersistentResource.
     * @param context the TableExportFormatterContext.
     * @param resource PersistentResource to format
     * @return output string
     */
    String format(TableExportFormatterContext context, PersistentResource<?> resource);

    /**
     * Pre Format Action.
     * Example: Generate Header, Metadata etc.
     * @param context the TableExportFormatterContext.
     * @return output string
     */
    default String preFormat(TableExportFormatterContext context) {
        return null;
    }

    /**
     * Post Format Action.
     * Example: Generate Metadata, Stats etc.
     * @param context the TableExportFormatterContext.
     * @return output string
     */
    default String postFormat(TableExportFormatterContext context) {
        return null;
    }
}
