package com.yahoo.elide.core.type;

public interface Field extends AccessibleObject {
    default boolean isSynthetic() {
        return false;
    }

    Object get(Object obj) throws IllegalArgumentException, IllegalAccessException;

    Type<?> getType();

    void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException;
}
