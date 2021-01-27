/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.EntityProjection;

/**
 * Interface which is used to format PersistentResource to output format.
 */
public interface TableExportFormatter {

    /**
     * Format PersistentResource.
     * @param resource PersistentResource to format
     * @param recordNumber Record Number being processed.
     * @return output string
     */
    public String format(PersistentResource resource, Integer recordNumber);

    /**
     * Pre Format Action.
     * Example: Generate Header, Metadata etc.
     * @param projection Entity projection.
     * @param query TableExport type object.
     * @return output string
     */
    public String preFormat(EntityProjection projection, TableExport query);

    /**
     * Post Format Action.
     * Example: Generate Metadata, Stats etc.
     * @param projection Entity projection.
     * @param query TableExport type object.
     * @return output string
     */
    public String postFormat(EntityProjection projection, TableExport query);
}
