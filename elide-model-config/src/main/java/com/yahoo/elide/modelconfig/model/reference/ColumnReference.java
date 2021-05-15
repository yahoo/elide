/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.model.reference;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Set;

/**
 * Handlebar Reference to another column.
 * eg: {{colB}}, {{join1.join2.colC}}
 */
@Getter
@Builder
public class ColumnReference implements HandlebarReference {

    @NonNull
    private final String name;

    private final Set<String> fixedArguments;
}
