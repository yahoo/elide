/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.model;

import com.yahoo.elide.modelconfig.model.reference.HandlebarReference;
import com.yahoo.elide.modelconfig.model.reference.HandlebarReferenceParser;

import java.util.Collections;
import java.util.List;

/**
 * Interface with common methods for {@link Dimension}, {@link Measure} and {@link Join}.
 */
public interface Column extends Named {

    final HandlebarReferenceParser REF_PARSER = new HandlebarReferenceParser();

    String getDefinition();

    default List<Argument> getArguments() {
        return Collections.emptyList();
    }

    /**
     * Get the list of {@link HandlebarReference} found in column's definition.
     * @return List of {@link HandlebarReference} found in column's definition.
     */
    default List<HandlebarReference> getHandlebarReferences() {
        return REF_PARSER.parse(getDefinition());
    }

    /**
     * Checks if this dimension or measure has provided argument.
     * @param argName Name of the {@link Argument} to  check for.
     * @return true if this column has provided argument.
     */
    default boolean hasArgument(String argName) {
        return hasName(getArguments(), argName);
    }
}
