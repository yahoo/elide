/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;

/**
 * Interface which is used to format PersistentResource to output format.
 */
public interface TableExportFormatter {

    /**
     * Format PersistentResource.
     * @param resource PersistentResource to format
     * @return output string
     */
    public String format(PersistentResource resource, Integer recordNumber);
}
