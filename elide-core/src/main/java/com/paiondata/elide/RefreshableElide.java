/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide;

import lombok.Getter;

/**
 * Wraps an Elide instance that can be hot reloaded at runtime.  This class is restricted to
 * a single access method (getElide) to eliminate state issues across reloads.
 */
public class RefreshableElide {
    @Getter
    private Elide elide;

    public RefreshableElide(Elide elide) {
        this.elide = elide;
    }
}
