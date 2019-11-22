/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core;

import com.yahoo.elide.Injector;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.security.checks.Check;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Non entity dictionary manages both JPA Entities and other Elide entities with {@link Include} annotation.
 */
@Slf4j
public class NonEntityDictionary extends EntityDictionary {
    public NonEntityDictionary() {
        super(new HashMap<>());
    }

    public NonEntityDictionary(Map<String, Class<? extends Check>> checks) {
        super(checks);
    }

    public NonEntityDictionary(Map<String, Class<? extends Check>> checks, Injector injector) {
        super(checks, injector);
    }

    /**
     * Add given class to dictionary.
     *
     * @param cls Entity bean class
     */
    @Override
    public void bindEntity(Class<?> cls) {
        Optional<Include> includeNeeded;
        if (isJPAEntity(cls)) {
            super.bindEntity(cls);
        } else {
            // this class is not a JPA entity
            includeNeeded = needsInclude(cls, null);
            if (!includeNeeded.isPresent()) {
                return;
            }

            Include include = includeNeeded.get();
            String type = StringUtils.uncapitalize(cls.getSimpleName());

            Class<?> duplicate = bindJsonApiToEntity.put(type, cls);

            if (duplicate != null && !duplicate.equals(cls)) {
                log.error("Duplicate binding {} for {}, {}", type, cls, duplicate);
                throw new DuplicateMappingException(type + " " + cls.getName() + ":" + duplicate.getName());
            }

            entityBindings.put(cls, new EntityBinding(this, cls, type, type));

            if (include.rootLevel()) {
                bindEntityRoots.add(cls);
            }
        }
    }

    /**
     * Get a list of elide-bound relationships. Exclude relationship to Non-JPA entities.
     *
     * @param entityClass Entity class to find relationships for
     * @return List of elide-bound relationship names.
     */
    @Override
    public List<String> getElideBoundRelationships(Class<?> entityClass) {
        return getRelationships(entityClass).stream()
                .filter(relationName -> {
                    Class<?> relationshipClass = getParameterizedType(entityClass, relationName);
                    return isJPAEntity(relationshipClass);
                })
                .collect(Collectors.toList());
    }
}
