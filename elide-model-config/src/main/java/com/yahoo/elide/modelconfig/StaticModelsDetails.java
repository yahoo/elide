/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.Type;

import lombok.Data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
/**
 * Custom class with required properties for static models.
 */
public class StaticModelsDetails {

    private final Map<ModelMapKey, ModelMapValue> staticModelsDetailsMap = new HashMap<>();

    @Data
    private static class ModelMapKey {
        private final String modelName;
        private final String modelVersion;
    }

    @Data
    private static class ModelMapValue {
        private final Set<String> fieldNames;
    }

    public void add(EntityDictionary dictionary, Type<?> cls) {

        String modelName = dictionary.getJsonAliasFor(cls);
        String modelVersion = EntityDictionary.getModelVersion(cls);
        Set<String> fieldNames = new HashSet<String>(dictionary.getAllFields(cls));

        staticModelsDetailsMap.put(new ModelMapKey(modelName, modelVersion),
                        new ModelMapValue(fieldNames));
    }

    /**
     * Check if provided (modelName, modelVersion) is defined.
     * @param modelName model name.
     * @param modelVersion model version.
     * @return true is provided (modelName, modelVersion) exists.
     */
    public boolean exists(String modelName, String modelVersion) {
        return staticModelsDetailsMap.containsKey(new ModelMapKey(modelName, modelVersion));
    }

    /**
     * Check if provided (modelName, modelVersion) has provided field.
     * @param modelName model name.
     * @param modelVersion model version.
     * @param fieldName field name.
     * @return true is provided (modelName, modelVersion) has provided field.
     */
    public boolean hasField(String modelName, String modelVersion, String fieldName) {
        ModelMapValue value = staticModelsDetailsMap.get(new ModelMapKey(modelName, modelVersion));
        return value != null ? value.getFieldNames().contains(fieldName) : false;
    }
}
