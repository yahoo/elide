/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter;

import com.yahoo.elide.core.Path;

import java.util.Collections;

public class HasNoMemberPredicate extends FilterPredicate {

    public HasNoMemberPredicate(Path path, Object value) {
        super(path, Operator.HASNOMEMBER, Collections.singletonList(value));
    }
    public HasNoMemberPredicate(Path.PathElement pathElement, Object value) {
        super(pathElement, Operator.HASNOMEMBER, Collections.singletonList(value));
    }
}
