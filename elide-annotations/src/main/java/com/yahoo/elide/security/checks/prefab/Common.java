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
import com.yahoo.elide.security.checks.OperationCheck;

import java.util.Optional;

/**
 * Checks that are generally applicable.
 */
public class Common {
    /**
     * A check that enables users to update objects or fields during a create operation. This check allows
     * users to be able to set values during object creation which are normally unmodifiable.
     *
     * @param <T> the type of object that this check guards
     */
    public static class UpdateOnCreate<T> extends OperationCheck<T> {
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

    /**
     * A generic check which denies any mutation that sets a field value to anything other than null.
     * The check is handy in case where we want to prevent the sharing of the child entity with a different parent
     * but at the same time allows the removal of the child from the relationship with the existing parent
     * @param <T> the type of object that this check guards
     */
    public static class FieldSetToNull<T> extends CommitCheck<T> {
        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec.map((c) -> { return c.getModified() == null; })
                    .orElse(false);
        }
    }
}
