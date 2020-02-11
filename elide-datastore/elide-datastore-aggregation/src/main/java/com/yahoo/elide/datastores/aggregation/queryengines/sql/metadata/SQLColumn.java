/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql.metadata;

import com.yahoo.elide.core.Path;

/**
 * Column with physical SQL information like reference and join to path.
 */
public interface SQLColumn {
    String getReference();

    Path getJoinPath();
}
