/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.utils;

import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Field;
import com.paiondata.elide.core.type.Method;
import com.paiondata.elide.core.type.Type;

/**
 * Utility methods to clone an object.
 */
public class ObjectCloners {
    private ObjectCloners() {
    }

    /**
     * Attempt to shallow clone an object.
     *
     * @param source the object to clone
     * @return cloned object if successful or original object
     */
    public static <T> T clone(T source) {
        return clone(source, ClassType.of(source.getClass()));
    }

    /**
     * Attempt to shallow clone an object.
     *
     * @param source the object to clone
     * @param cls    the type of object
     * @return cloned object if successful or original object
     */
    public static <T> T clone(T source, Type<?> cls) {
        try {
            @SuppressWarnings("unchecked")
            T target = (T) cls.newInstance();
            copyProperties(source, target, cls);
            return target;
        } catch (InstantiationException | IllegalAccessException e) {
            // ignore
        }
        // Failed to clone return original object
        return source;
    }

    /**
     * Attempt to copy properties from the source to the target.
     * @param source the bean to copy from
     * @param target the bean to copy to
     * @param cls the class
     */
    public static void copyProperties(Object source, Object target, Type<?> cls) {
        for (Field field : cls.getFields()) {
            try {
                field.set(target, field.get(source));
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
                // ignore
            }
        }
        for (Field field : cls.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                field.set(target, field.get(source));
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
                // ignore
            }
        }
        for (Method method : cls.getMethods()) {
            if (method.getName().startsWith("set")) {
                try {
                    Method getMethod = cls.getMethod("get" + method.getName().substring(3));
                    method.invoke(target, getMethod.invoke(source));
                } catch (NoSuchMethodException e) {
                    try {
                        Method isMethod = cls.getMethod("is" + method.getName().substring(3));
                        method.invoke(target, isMethod.invoke(source));
                    } catch (IllegalStateException
                            | IllegalArgumentException
                            | ReflectiveOperationException
                            | SecurityException e2) {
                        // ignore
                    }
                } catch (IllegalStateException
                        | IllegalArgumentException
                        | ReflectiveOperationException
                        | SecurityException e) {
                    // ignore
                }
            }
        }
    }
}
