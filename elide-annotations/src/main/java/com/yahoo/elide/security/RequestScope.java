/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import java.util.Set;

/**
 * The request scope interface passed to checks.
 */
public interface RequestScope {
    User getUser();
    Set<PersistentResource> getNewResources();
}
