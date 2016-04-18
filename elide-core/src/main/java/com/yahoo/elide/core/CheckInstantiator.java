/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.security.checks.Check;

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
        Class<? extends Check> checkCls = dictionary.getCheck(checkName);
        return instantiateCheck(checkCls);
    }

    /**
     * Instantiates a new instance of a check.
     * @param checkCls the check class to instantiate
     * @return the instance of the check
     * @throws IllegalArgumentException if the check class cannot be instantiated with a zero argument constructor
     */
    default Check instantiateCheck(Class<? extends Check> checkCls) {
        try {
            return checkCls.newInstance();
        } catch (InstantiationException | IllegalAccessException | NullPointerException e) {
            String checkName = (checkCls != null) ? checkCls.getName() : "null";
            throw new IllegalArgumentException("Could not instantiate specified check '" + checkName + "'.", e);
        }
    }
}
