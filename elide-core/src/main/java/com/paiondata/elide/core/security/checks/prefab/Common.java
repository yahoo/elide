/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security.checks.prefab;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.OperationCheck;

import java.util.Optional;

/**
 * Checks that are generally applicable.
 */
public class Common {
    /**
     * A generic check which denies any mutation that sets a field value to anything other than null.
     * The check is handy in case where we want to prevent the sharing of the child entity with a different parent
     * but at the same time allows the removal of the child from the relationship with the existing parent
     * @param <T> the type of object that this check guards
     */
    public static class FieldSetToNull<T> extends OperationCheck<T> {
        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.filter(c -> c.getModified() == null).isPresent();
        }
    }
}
