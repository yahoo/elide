/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.compile;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

/**
 * ClassLoader for dynamic configuration.
 */
@Slf4j
@Data
@AllArgsConstructor
public class ElideDynamicInMemoryClassLoader extends ClassLoader {

    private Set<String> classNames = Sets.newHashSet();

    public ElideDynamicInMemoryClassLoader(ClassLoader parent, Set<String> classNames) {
        super(parent);
        setClassNames(classNames);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    protected URL findResource(String name) {
        log.debug("Finding Resource " + name + " in "  + classNames);
        if (classNames.contains(name.replace("/", ".").replace(".class", ""))) {
            try {
                log.debug("Returning Resource " + "file://" + name);
                return new URL("file://" + name);
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        return super.findResource(name);
    }
}
