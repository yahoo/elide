/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Methods for working with object properties.
 */
public class ObjectProperties {

    /**
     * Gets the property value from the bean.
     *
     * @param <T> the property type
     * @param bean the bean
     * @param name the property name
     * @return the value
     */
    public static <T> T getProperty(final Object bean, final String name) {
        return getProperty(bean, name, bean.getClass());
    }


    /**
     * Gets the property value from the bean.
     *
     * @param <T> the property type
     * @param bean the bean
     * @param name the property name
     * @param clazz the bean class
     * @return the value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(final Object bean, final String name, final Class<?> clazz) {
        try {
            Field field = clazz.getField(name);
            return (T) field.get(bean);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // ignore
        }

        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(bean);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            // ignore
        }

        String methodName = name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
        String getMethodName = "get" + methodName;
        try {
            Method method = clazz.getMethod(getMethodName);
            return (T) method.invoke(bean);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            // ignore
        }

        String isMethodName = "is" + methodName;
        try {
            Method method = clazz.getMethod(isMethodName);
            return (T) method.invoke(bean);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            // ignore
        }

        try {
            Method method = clazz.getDeclaredMethod(getMethodName);
            method.setAccessible(true);
            return (T) method.invoke(bean);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            // ignore
        }

        try {
            Method method = clazz.getDeclaredMethod(isMethodName);
            method.setAccessible(true);
            return (T) method.invoke(bean);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            // ignore
        }
        return null;
    }
}
