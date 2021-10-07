/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.dictionary;

/**
 * Lens through which you look at a dictionaries bindings.
 */
@FunctionalInterface
public interface EntityBindingLens {

    /**
     * Filters only the bindings you want to see.
     * @param binding A binding to consider.
     * @return True if you want to see the binding.  False otherwise.
     */
    boolean filter(EntityBinding binding);
}
