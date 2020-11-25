/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.compile;

import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.DynamicConfigHelpers;
import com.yahoo.elide.modelconfig.model.DBConfig;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiles dynamic model pojos generated from hjson files.
 */
public class ElideDynamicEntityCompiler {

    private final Map<String, Class<?>> compiledObjects;

    private final ElideDynamicInMemoryCompiler compiler = ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings();
    @Getter
    private final Map<String, ConnectionDetails> connectionDetailsMap;

    /**
     * Parse dynamic config path.
     * @param path : Dynamic config hjsons root location
     * @param dbPasswordExtractor : Password Extractor Implementation
     * @throws Exception Exception thrown
     */
    public ElideDynamicEntityCompiler(String path, DBPasswordExtractor dbPasswordExtractor) throws Exception {

        if (DynamicConfigHelpers.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Config path is null");
        }

        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(path);
        dynamicConfigValidator.readAndValidateConfigs();
        dynamicConfigValidator.hydrateAndCompileModelConfigs(compiler);
        dynamicConfigValidator.compileDBConfigs(dbPasswordExtractor);

        this.compiledObjects = dynamicConfigValidator.getCompiledObjects();
        this.connectionDetailsMap = dynamicConfigValidator.getConnectionDetailsMap();
    }

    /**
     * Parse dynamic config path and provides default implementation for DB Password Extractor.
     * @param path : Dynamic config hjsons root location.
     * @throws Exception Exception thrown.
     */
    public ElideDynamicEntityCompiler(String path) throws Exception {
        this(path, new DBPasswordExtractor() {
            @Override
            public String getDBPassword(DBConfig config) {
                return StringUtils.EMPTY;
            }
        });
    }

    /**
     * Get Inmemorycompiler's classloader.
     * @return ClassLoader
     */
    public ClassLoader getClassLoader() {
        return compiler.getClassloader();
    }

    /**
     * Find classes with a particular annotation from dynamic compiler.
     * @param annotationClass Annotation to search for.
     * @return Set of Classes matching the annotation.
     * @throws ClassNotFoundException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Set<Class<?>> findAnnotatedClasses(Class annotationClass)
            throws ClassNotFoundException {

        Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();

        compiledObjects.values().forEach(classz -> {
            if (classz.getAnnotation(annotationClass) != null) {
                annotatedClasses.add(classz);
            }
        });

        return annotatedClasses;
    }

    /**
     * Find classes with a particular annotation from dynamic compiler.
     * @param annotationClass Annotation to search for.
     * @return Set of Classes matching the annotation.
     * @throws ClassNotFoundException
     */
    @SuppressWarnings({ "rawtypes" })
    public List<String> findAnnotatedClassNames(Class annotationClass)
            throws ClassNotFoundException {

        return findAnnotatedClasses(annotationClass)
               .stream()
               .map(Class::getName)
               .collect(Collectors.toList());
    }

    /**
     * Get class names for all dynamically compiled classes.
     * @return A list of all dynamically compiled classes.
     */
    public List<String> getClassNames() {
        return compiledObjects.keySet()
               .stream()
               .collect(Collectors.toList());
    }
}
