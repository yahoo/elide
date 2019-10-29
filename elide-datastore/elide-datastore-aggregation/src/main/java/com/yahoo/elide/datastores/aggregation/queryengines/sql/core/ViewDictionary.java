/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.core;

import com.yahoo.elide.Injector;
import com.yahoo.elide.core.EntityBinding;
import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.core.exceptions.DuplicateMappingException;
import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.View;
import com.yahoo.elide.security.checks.Check;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.persistence.Entity;

/**
 * Extension of {@link EntityDictionary} that manage view models.
 */
@Slf4j
public class ViewDictionary extends EntityDictionary {
    private final ConcurrentHashMap<Class<?>, ViewBinding> viewBindings = new ConcurrentHashMap<>();
    private final static ViewBinding EMPTY_BINDING = new ViewBinding();

    public ViewDictionary(Map<String, Class<? extends Check>> checks) {
        super(checks);
    }

    public ViewDictionary(Map<String, Class<? extends Check>> checks, Injector injector) {
        super(checks, injector);
    }

    /**
     * Follow for this class or super-class for {@link View} annotation.
     *
     * @param objClass provided class
     * @return class with view annotation
     */
    public Class<?> lookupViewClass(Class<?> objClass) {
        for (Class<?> cls = objClass; cls != null; cls = cls.getSuperclass()) {
            ViewBinding binding = viewBindings.getOrDefault(cls, EMPTY_BINDING);
            if (binding != EMPTY_BINDING) {
                return binding.entityClass;
            }
            if (cls.isAnnotationPresent(View.class)) {
                return cls;
            }
        }
        throw new IllegalArgumentException("Unknown View " + objClass);
    }

    /**
     * Add given view bean to dictionary.
     *
     * @param cls view bean class
     */
    public void bindView(Class<?> cls) {
        if (viewBindings.getOrDefault(lookupViewClass(cls), EMPTY_BINDING) != EMPTY_BINDING) {
            return;
        }

        if (cls.isAnnotationPresent(Entity.class)) {
            log.trace("{} is an Entity, not a view.", cls.getName());
            return;
        }

        Annotation annotation = getFirstAnnotation(cls, Collections.singletonList(View.class));
        View view = annotation instanceof View ? (View) annotation : null;
        if (view == null) {
            log.trace("Missing view definition {}", cls.getName());
            return;
        }

        String viewName = !"".equals(view.name())
                    ? view.name()
                    : StringUtils.uncapitalize(cls.getSimpleName());

        Class<?> duplicate = bindJsonApiToEntity.put(viewName, cls);
        if (duplicate != null && !duplicate.equals(cls)) {
            log.error("Duplicate binding {} for {}, {}", viewName, cls, duplicate);
            throw new DuplicateMappingException(viewName + " " + cls.getName() + ":" + duplicate.getName());
        }

        viewBindings.putIfAbsent(lookupViewClass(cls), new ViewBinding(cls, viewName));
    }

    /**
     * Get binding for a view or entity.
     *
     * @param entityClass cls to lookup
     * @return corresponding binding
     */
    @Override
    protected EntityBinding getEntityBinding(Class<?> entityClass) {
        if (isMappedInterface(entityClass)) {
            return super.getEntityBinding(entityClass);
        }
        try {
            ViewBinding viewBinding = viewBindings.getOrDefault(lookupViewClass(entityClass), EMPTY_BINDING);
            return viewBinding == EMPTY_BINDING ? super.getEntityBinding(entityClass) : viewBinding;
        } catch (IllegalArgumentException e) {
            return super.getEntityBinding(entityClass);
        }
    }

    /**
     * Get all bindings.
     *
     * @return the bindings
     */
    @Override
    public Set<Class<?>> getBindings() {
        return Sets.union(entityBindings.keySet(), viewBindings.keySet());
    }

    /**
     * Check whether an entity is defined as a view.
     *
     * @param cls class to check
     * @return True if this class is bound as view
     */
    public boolean isView(Class<?> cls) {
        return viewBindings.containsKey(cls);
    }

    /**
     * Check whether an entity is defined as a view.
     *
     * @param entityName entity class name
     * @return True if this class is bound as view
     */
    public boolean isView(String entityName) {
        return viewBindings.containsKey(getEntityClass(entityName));
    }

    /**
     * Gets id. Views don't have id field.
     *
     * @param value the value
     * @return the id
     */
    @Override
    public String getId(Object value) {
        return value == null || isView(value.getClass()) ? null : super.getId(value);
    }

    /**
     * Get a list of elide-bound relationships. Exclude the view relationships.
     *
     * @param entityClass Entity class to find relationships for
     * @return List of elide-bound relationship names.
     */
    public List<String> getElideBoundRelationships(Class<?> entityClass) {
        return getRelationships(entityClass).stream()
                .filter(relationName -> getBindings().contains(getParameterizedType(entityClass, relationName)))
                .filter(relationName -> !isView(getParameterizedType(entityClass, relationName)))
                .collect(Collectors.toList());
    }
}
