/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security.checks.prefab;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.PersistentResource;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;

import java.util.Optional;

/**
 * A check designed to enable functionality during a create. In general, this is used when wanting users
 * to be able to update values upon object creation, but not after.
 *
 * @param <T> type parameter
 */
public class UpdateOnCreateCheck<T> extends CommitCheck<T> {
    @Override
    public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        for (PersistentResource resource : requestScope.getNewResources()) {
            if (record == resource.getObject()) {
                return true;
            }
        }
        return false;
    }
}
