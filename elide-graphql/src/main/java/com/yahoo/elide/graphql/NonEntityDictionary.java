/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.dictionary.EntityBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.function.Function;

/**
 * A set of reflection utilities for non-Elide entities.
 */
@Slf4j
public class NonEntityDictionary extends EntityDictionary {

    public NonEntityDictionary(ClassScanner scanner, Function<Class, Serde> serdeLookup) {
        super(
                Collections.emptyMap(), //Checks
                Collections.emptyMap(), //Role checks
                DEFAULT_INJECTOR,
                serdeLookup,
                Collections.emptySet(), //Entity excludes
                scanner);
    }

    /**
     * Add given class to dictionary.
     *
     * @param cls Entity bean class
     */
    @Override
    public void bindEntity(Type<?> cls) {
        String type = WordUtils.uncapitalize(cls.getSimpleName());

        Type<?> duplicate = bindJsonApiToEntity.put(Pair.of(type, NO_VERSION), cls);

        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
            throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
        }

        entityBindings.put(cls, new EntityBinding(getInjector(), cls, type));
    }
}
