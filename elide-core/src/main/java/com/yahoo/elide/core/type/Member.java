/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.type;

/**
 * Base class of fields and methods.
 */
public interface Member {

    /**
     * Get the permission modifiers of the field/method.
     * @return The permission modifiers.
     */
    int getModifiers();
}
