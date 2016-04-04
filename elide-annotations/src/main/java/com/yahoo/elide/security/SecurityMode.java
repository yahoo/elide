/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

/**
 * Enable or disable security.
 *
 * This functionality is deprecated and will soon be unavailable. At time of writing, SecurityMode's
 * are almost entirely non-functional within Elide with few exceptions that will remain until this enum
 * is removed.
 *
 * @deprecated Since 2.1, instead use the {@code Elide.Builder} with an appropriate {@code PermissionExecutor}
 */
@Deprecated
public enum SecurityMode {
    SECURITY_INACTIVE,
    SECURITY_ACTIVE_VERBOSE,
    SECURITY_ACTIVE
}
