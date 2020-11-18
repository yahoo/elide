/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * A set of reflection utilities for non-Elide entities.
 */
@Slf4j
public class NonEntityDictionary extends EntityDictionary {
    public NonEntityDictionary() {
        super(new HashMap<>());
    }

    /**
     * Add given class to dictionary.
     *
     * @param cls Entity bean class
     */
    @Override
    public void bindEntity(Class<?> cls) {
        String type = WordUtils.uncapitalize(cls.getSimpleName());

        Class<?> duplicate = bindJsonApiToEntity.put(Pair.of(type, NO_VERSION), cls);

        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
        }

        entityBindings.put(cls, new EntityBinding(this, cls, type));
    }
}
