/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.utils.coerce.converters.Serde;
import graphql.schema.Coercing;

public interface ElideCoercing<I, O> extends Coercing<I, O> {
    default Serde getSerde() {
        return null;
    }
}
