/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.compile;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideConfigParser;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;

import com.google.common.collect.Sets;

import org.mdkt.compiler.InMemoryJavaCompiler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
     * @throws IOException IOException thrown
     */
    public ElideDynamicEntityCompiler(String path) throws IOException {

        ElideTableConfig tableConfig = new ElideTableConfig();
        ElideSecurityConfig securityConfig = new ElideSecurityConfig();
        ElideConfigParser elideConfigParser = new ElideConfigParser(path);
        HandlebarsHydrator hydrator = new HandlebarsHydrator();

        tableConfig = elideConfigParser.getElideTableConfig();
        securityConfig = elideConfigParser.getElideSecurityConfig();
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
    }

    /**
     * Compile table and security model pojos.
     * @throws Exception
     */
    public void compile() throws Exception {

        for (Map.Entry<String, String> tablePojo : tableClasses.entrySet()) {
            log.debug("key: " + tablePojo.getKey() + ", value: " + tablePojo.getValue());
            compiler.addSource(PACKAGE_NAME + tablePojo.getKey(), tablePojo.getValue());
        }

        for (Map.Entry<String, String> secPojo : securityClasses.entrySet()) {
            log.debug("key: " + secPojo.getKey() + ", value: " + secPojo.getValue());
            compiler.addSource(PACKAGE_NAME + secPojo.getKey(), secPojo.getValue());
        }

        try {
            compiledObjects = compiler.compileAll();
        } catch (Exception e) {
            log.error("Unable to compile dynamic classes in memory ");
        }

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
            Class<?> classz = getClassLoader().loadClass(dynamicClass);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<String> findAnnotatedClassNames(Class annotationClass)
            throws ClassNotFoundException {

        List<String> annotatedClasses = new ArrayList<String>();
        ArrayList<String> dynamicClasses = classNames;

        for (String dynamicClass : dynamicClasses) {
            Class<?> classz = getClassLoader().loadClass(dynamicClass);
            if (classz.getAnnotation(annotationClass) != null) {
                annotatedClasses.add(classz.getName());
            }
        }

        return annotatedClasses;
    }
}
