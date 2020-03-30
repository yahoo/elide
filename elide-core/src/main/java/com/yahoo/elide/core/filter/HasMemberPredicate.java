/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;

import java.util.Collections;

public class HasMemberPredicate extends FilterPredicate {

    public HasMemberPredicate(Path path, Object value) {
        super(path, Operator.HASMEMBER, Collections.singletonList(value));
    }
    public HasMemberPredicate(Path.PathElement pathElement, Object value) {
        super(pathElement, Operator.HASMEMBER, Collections.singletonList(value));
    }
}
