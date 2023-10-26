/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.request.EntityProjection;

import java.util.function.Supplier;

/**
 * Context for storing state during the table export.
 * <p>
 * This can be sub-classed to store additional state during the table export.
 */
public class TableExportFormatterContext {
    protected EntityProjection entityProjection;
    protected TableExport tableExport;
    protected Supplier<Integer> recordNumberSupplier;

    /**
     * Constructor.
     *
     * @param entityProjection the projection
     * @param tableExport the query
     * @param recordNumberSupplier the current record number
     */
    public TableExportFormatterContext(EntityProjection entityProjection, TableExport tableExport,
            Supplier<Integer> recordNumberSupplier) {
        this.entityProjection = entityProjection;
        this.tableExport = tableExport;
        this.recordNumberSupplier = recordNumberSupplier;
    }

    /**
     * Gets the {@link EntityProjection}.
     *
     * @return the projection
     */
    public EntityProjection getEntityProjection() {
        return entityProjection;
    }

    /**
     * Gets the {@link TableExport}.
     *
     * @return the query
     */
    public TableExport getTableExport() {
        return tableExport;
    }

    /**
     * Gets the record number.
     *
     * @return the current record number
     */
    public int getRecordNumber() {
        return recordNumberSupplier.get();
    }
}
