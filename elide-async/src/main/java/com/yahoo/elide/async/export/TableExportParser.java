/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export;

import com.yahoo.elide.async.models.AsyncQuery;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;

/**
 * Interface for parsing TableExport request.
 */
public interface TableExportParser {

    /**
     * Parses the AsyncQuery to generate EntityProjection.
     * @param query AsyncQuery Object.
     * @return EntityProjection parsed.
     * @throws BadRequestException Exception thrown.
     */
    public EntityProjection parse(AsyncQuery query) throws BadRequestException;
}
