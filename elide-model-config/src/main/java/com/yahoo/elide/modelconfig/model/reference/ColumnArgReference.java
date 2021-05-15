/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.model.reference;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Handlebar Reference to a column argument.
 * eg: {{$$column.args.argName}}
 */
@Getter
@Builder
public class ColumnArgReference implements HandlebarReference {

    @NonNull
    private String argName;
}
