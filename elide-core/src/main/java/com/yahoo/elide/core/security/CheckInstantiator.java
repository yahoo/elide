/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.security;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.security.checks.Check;

/**
 * Get new instances of a check from a check identifier in an expression.
 */
public interface CheckInstantiator {

    /**
     * Gets a check instance by first checking the entity dictionary for a mapping on the provided identifier.
     * In the event that no such mapping is found the identifier is used as a canonical name.
     * @param dictionary the entity dictionary to search for a mapping
     * @param checkName the identifier of the check to instantiate
     * @return the check instance
     * @throws IllegalArgumentException if there is no mapping for {@code checkName} and {@code checkName} is not
     *         a canonical identifier
     */
    default Check getCheck(EntityDictionary dictionary, String checkName) {
        return dictionary.getCheckInstance(checkName);
    }
}
