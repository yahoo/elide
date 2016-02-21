/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.security;

import lombok.Getter;

/**
 * Wrapper for opaque user passed in every request.
 */
public class User {
    @Getter private final Object opaqueUser;

    public User(Object opaqueUser) {
        this.opaqueUser = opaqueUser;
    }
}
