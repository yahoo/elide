/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.resources;

import javax.ws.rs.core.SecurityContext;
import java.util.function.Function;

/**
 * Placeholder for injection frameworks.
 */
@FunctionalInterface
public interface DefaultOpaqueUserFunction extends Function<SecurityContext, Object> {
    // Empty
}
