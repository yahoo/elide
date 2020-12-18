package com.yahoo.elide.core.type;

import java.lang.reflect.InvocationTargetException;

public interface Method extends AccessibleObject {
    String getName();

    int getParameterCount();

    Object invoke(Object obj, Object... args) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

    Type<?> getReturnType();

    Class<?>[] getParameterTypes();
}
