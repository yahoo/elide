/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.aggregation.queryengines.sql.calcite;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class SupportedOperation {
    @NonNull
    private String name;
}
