/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks.prefab;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;

import java.util.Collection;
import java.util.Optional;

/**
 * A check designed to look at the changeSpec to determine whether a value was added to a Collection
 *
 * @param <T> type parameter
 */

public class AddToCollectionCheck<T> extends CommitCheck<T> {

    @Override
    public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return (changeSpec.isPresent() && changeSpec.get().getModified() instanceof Collection)
                && ((Collection) changeSpec.get().getModified()).containsAll(
                (Collection) changeSpec.get().getOriginal());
    }
}
