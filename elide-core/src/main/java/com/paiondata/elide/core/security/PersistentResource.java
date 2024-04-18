/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.security;

import com.paiondata.elide.core.type.Type;

import java.util.Optional;

/**
 * The persistent resource interface passed to change specs.
 * @param <T> resource type
 */
public interface PersistentResource<T> {

    boolean matchesId(String id);

    Optional<String> getUUID();
    String getId();
    String getTypeName();

    T getObject();
    Type<T> getResourceType();
    RequestScope getRequestScope();
}
