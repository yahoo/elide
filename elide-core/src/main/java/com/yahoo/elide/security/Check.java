/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import com.yahoo.elide.core.RequestScope;

import java.util.Optional;

/**
 * Custom security access that verifies whether a user belongs to a role.
 * Permissions are assigned as a set of checks that grant access to the permission.
 * @param <T> Type of record for Check
 */
public interface Check<T> {

    /**
     * Determines whether the user can access the resource. This method is called specifically directly before the
     * commit. That is, if full/reliable information about an object (based on this request) is required, this is the
     * method which should be implemented.
     *
     * <b>NOTE:</b> The object on this method is only complete for Create, Update, and Share permissions. If used
     *              in a Read or Delete permission, the final object values are not guaranteed since
     *              they will not be deferred until commit time. That is, this is treated as an additional
     *              <i>operation check</i> in the cases of Read and Delete permissions. Moreover, it is joined in
     *              conjunction with the result from the proper operation check in such a scenario.
     *
     * @param object Fully modified object
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if allowed
     */
    default boolean ok(T object, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
        return ok(requestScope, changeSpec);
    }

    /**
     * Determines whether the user can access the resource. This method is called before attempting to stage the
     * operation. This method should be used when a complete and consistent representation is not required to perform
     * a proper check.
     *
     * <b>NOTE:</b> This method is executed for all permission types.
     *
     * @param requestScope Request scope object
     * @param changeSpec Summary of modifications
     * @return true if allowed
     */
    boolean ok(RequestScope requestScope, Optional<ChangeSpec> changeSpec);
}
