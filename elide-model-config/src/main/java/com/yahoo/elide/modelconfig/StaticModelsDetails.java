/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.core.dictionary.EntityDictionary;

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
        private final String className;
        private final String classImport;
        private final Set<String> fieldNames;
    }

    public void add(EntityDictionary dictionary, Class<?> cls) {

        String modelName = dictionary.getJsonAliasFor(cls);
        String modelVersion = EntityDictionary.getModelVersion(cls);
        String className = cls.getSimpleName();
        String pkgName = cls.getPackage().getName();
        Set<String> fieldNames = new HashSet<String>(dictionary.getAllFields(cls));

        staticModelsDetailsMap.put(new ModelMapKey(modelName, modelVersion),
                        new ModelMapValue(className, prepareImport(pkgName, className), fieldNames));
    }

    /**
     * Prepare Import statement.
     * @param pkgName package name.
     * @param className class name.
     * @return complete import statement.
     */
    private static String prepareImport(String pkgName, String className) {
        return "import " + pkgName + "." + className + ";";
    }

    /**
     * Get Class Name for provided (modelName, modelVersion).
     * @param modelName model name.
     * @param modelVersion model version.
     * @param defaultValue default value.
     * @return class name for provided (modelName, modelVersion) if available else default value.
     */
    public String getClassName(String modelName, String modelVersion, String defaultValue) {
        ModelMapValue value = staticModelsDetailsMap.get(new ModelMapKey(modelName, modelVersion));
        return value != null ? value.getClassName() : defaultValue;
    }

    /**
     * Get Class Import for provided (modelName, modelVersion).
     * @param modelName model name.
     * @param modelVersion model version.
     * @param defaultValue default value.
     * @return import statement for provided (modelName, modelVersion) if available else default value.
     */
    public String getClassImport(String modelName, String modelVersion, String defaultValue) {
        ModelMapValue value = staticModelsDetailsMap.get(new ModelMapKey(modelName, modelVersion));
        return value != null ? value.getClassImport() : defaultValue;
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
