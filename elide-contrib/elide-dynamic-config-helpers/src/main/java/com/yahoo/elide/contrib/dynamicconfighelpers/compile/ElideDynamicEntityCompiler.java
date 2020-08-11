/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.compile;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;
import com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator;
import com.google.common.collect.Sets;

import org.mdkt.compiler.InMemoryJavaCompiler;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiles dynamic model pojos generated from hjson files.
 *
 */
@Slf4j
public class ElideDynamicEntityCompiler {

    public static ArrayList<String> classNames = new ArrayList<String>();

    public static final String PACKAGE_NAME = "dynamicconfig.models.";
    private Map<String, Class<?>> compiledObjects;

    private InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance().ignoreWarnings();

    private Map<String, String> tableClasses = new HashMap<String, String>();
    private Map<String, String> securityClasses = new HashMap<String, String>();

    /**
     * Parse dynamic config path.
     * @param path : Dynamic config hjsons root location
     * @throws Exception Exception thrown
     */
    public ElideDynamicEntityCompiler(String path) throws Exception {

        if (DynamicConfigHelpers.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("Config path is null");
        }
        HandlebarsHydrator hydrator = new HandlebarsHydrator();

<<<<<<< HEAD
        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator();
        dynamicConfigValidator.readAndValidateConfigs(path);
=======
        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(path);
        dynamicConfigValidator.readAndValidateConfigs();
>>>>>>> elide-5.x

        ElideTableConfig tableConfig = dynamicConfigValidator.getElideTableConfig();
        ElideSecurityConfig securityConfig = dynamicConfigValidator.getElideSecurityConfig();

        tableClasses = hydrator.hydrateTableTemplate(tableConfig);
        securityClasses = hydrator.hydrateSecurityTemplate(securityConfig);

        for (Entry<String, String> entry : tableClasses.entrySet()) {
            classNames.add(PACKAGE_NAME + entry.getKey());
        }

        for (Entry<String, String> entry : securityClasses.entrySet()) {
            classNames.add(PACKAGE_NAME + entry.getKey());
        }

        compiler.useParentClassLoader(
                new ElideDynamicInMemoryClassLoader(ClassLoader.getSystemClassLoader(),
                        Sets.newHashSet(classNames)));
        compile();
    }

    /**
     * Compile table and security model pojos.
     * @throws Exception
     */
    private void compile() throws Exception {

        for (Map.Entry<String, String> tablePojo : tableClasses.entrySet()) {
            log.debug("key: " + tablePojo.getKey() + ", value: " + tablePojo.getValue());
            compiler.addSource(PACKAGE_NAME + tablePojo.getKey(), tablePojo.getValue());
        }

        for (Map.Entry<String, String> secPojo : securityClasses.entrySet()) {
            log.debug("key: " + secPojo.getKey() + ", value: " + secPojo.getValue());
            compiler.addSource(PACKAGE_NAME + secPojo.getKey(), secPojo.getValue());
        }

        compiledObjects = compiler.compileAll();
    }

    /**
     * Get Inmemorycompiler's classloader.
     * @return ClassLoader
     */
    public ClassLoader getClassLoader() {
        return compiler.getClassloader();
    }

    /**
     * Get the class from compiled class lists.
     * @param name name of the class
     * @return Class
     */
    public Class<?> getCompiled(String name) {
        return compiledObjects.get(name);
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
        ArrayList<String> dynamicClasses = classNames;

        for (String dynamicClass : dynamicClasses) {
            Class<?> classz = compiledObjects.get(dynamicClass);
            if (classz.getAnnotation(annotationClass) != null) {
                annotatedClasses.add(classz);
            }
        }

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
}
