/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.converter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * ModelConverterContext that does not attempt to unwrap the reference for dynamic types.
 * @see io.swagger.v3.core.converter.ModelConverterContextImpl
 */
@SuppressWarnings("rawtypes")
public class DynamicModelConverterContext implements ModelConverterContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicModelConverterContext.class);

    private final List<ModelConverter> converters;
    private final Map<String, Schema> modelByName;
    private final HashMap<AnnotatedType, Schema> modelByType;
    private final Set<AnnotatedType> processedTypes;

    public DynamicModelConverterContext(List<ModelConverter> converters) {
        this.converters = converters;
        modelByName = new TreeMap<>();
        modelByType = new HashMap<>();
        processedTypes = new HashSet<>();
    }

    public DynamicModelConverterContext(ModelConverter converter) {
        this(new ArrayList<>());
        converters.add(converter);
    }

    @Override
    public Iterator<ModelConverter> getConverters() {
        return converters.iterator();
    }

    @Override
    public void defineModel(String name, Schema model) {
        AnnotatedType aType = null;
        defineModel(name, model, aType, null);
    }

    @Override
    public void defineModel(String name, Schema model, Type type, String prevName) {
        defineModel(name, model, new AnnotatedType().type(type), prevName);
    }
    @Override
    public void defineModel(String name, Schema model, AnnotatedType type, String prevName) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("defineModel %s %s", name, model));
        }
        modelByName.put(name, model);

        if (StringUtils.isNotBlank(prevName) && !prevName.equals(name)) {
            modelByName.remove(prevName);
        }

        if (type != null && type.getType() != null) {
            modelByType.put(type, model);
        }
    }

    @Override
    public Map<String, Schema> getDefinedModels() {
        return Collections.unmodifiableMap(modelByName);
    }

    @Override
    public Schema resolve(AnnotatedType type) {

//        AnnotatedType aType = ReferenceTypeUtils.unwrapReference(type);
//        if (aType != null) {
//            return resolve(aType);
//        }

        if (processedTypes.contains(type)) {
            return modelByType.get(type);
        } else {
            processedTypes.add(type);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("resolve %s", type.getType()));
        }
        Iterator<ModelConverter> converters = this.getConverters();
        Schema resolved = null;
        if (converters.hasNext()) {
            ModelConverter converter = converters.next();
            LOGGER.trace("trying extension {}", converter);
            resolved = converter.resolve(type, this, converters);
        }
        if (resolved != null) {
            modelByType.put(type, resolved);

            Schema resolvedImpl = resolved;
            if (resolvedImpl.getName() != null) {
                modelByName.put(resolvedImpl.getName(), resolved);
            }
        } else {
            processedTypes.remove(type);
        }

        return resolved;
    }
}
