/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.type;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * Elide type for models and their attributes.
 * @param <T> The underlying Java class.
 */
public interface Type<T> extends java.lang.reflect.Type, Serializable {
    static final long serialVersionUID = -51926356467315522L;

    /**
     * Gets the canonical name of the class containing no $ symbols.
     * @return The canonical name.
     */
    String getCanonicalName();

    /**
     * Gets the simple name of the class without package prefix.
     * @return The simple name.
     */
    String getSimpleName();


    /**
     * Get the name of the class including package prefix.
     * @return The name of the class.
     */
    String getName();

    /**
     * Returns a method belonging to a type.
     * @param name The method name.
     * @param parameterTypes The parameter types.
     * @return The method.
     * @throws NoSuchMethodException if no matching method is found.
     */
    Method getMethod(String name, Type<?>... parameterTypes) throws NoSuchMethodException;

    /**
     * Gets the super class associated with the type.
     * @return Super class type or null.
     */
    Type<?> getSuperclass();

    /**
     * Returns all the annotations of a given type.
     * @param annotationClass The annotation type to search for.
     * @param <A> The annotation class.
     * @return An array of found annotations.
     */
    <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationClass);

    /**
     * Gets a specific annotation ignoring inherited annotations.
     * @param annotationClass The class to search for.
     * @param <A> The class to search for.
     * @return The declared annotation or null.
     */
    <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass);

    /**
     * Gets a specific annotation including inherited annotations.
     * @param annotationClass The class to search for.
     * @param <A> The class to search for.
     * @return The declared annotation or null.
     */
    <A extends Annotation> A getAnnotation(Class<A> annotationClass);

    /**
     * Searches for a specific annotation including inherited annotations.
     * @param annotationClass The class to search for.
     * @return true or false.
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

    /**
     * Can this type be assigned to another type.
     * @param cls The type to assign to.
     * @return true or false.
     */
    boolean isAssignableFrom(Type<?> cls);

    /**
     * Is this type a Java primitive?
     * @return true or false.
     */
    boolean isPrimitive();

    /**
     * Gets the package this type belongs to.
     * @return the package.
     */
    Package getPackage();

    /**
     * Gets all the methods found in this type.
     * @return Any array of methods.
     */
    Method[] getMethods();

    /**
     * Gets all the methods found in this type.
     * @return Any array of methods.
     */
    Method[] getDeclaredMethods();

    /**
     * Gets all the fields found in this type.
     * @return Any array of fields.
     */
    Field[] getFields();

    /**
     * Gets all the fields found in this type ignoring inherited fields.
     * @return Any array of fields.
     */
    Field[] getDeclaredFields();

    /**
     * Gets a specific field found in this type ignoring inherited fields.
     * @param name The name of the field to find.
     * @return The found field.
     * @throws NoSuchFieldException if the field is not found.
     */
    Field getDeclaredField(String name) throws NoSuchFieldException;

    /**
     * Returns the full list of type constructors.
     * @return An array of type constructors.
     */
    Method[] getConstructors();

    /**
     * Whether or not this type is parameterized.
     * @return true or false.
     */
    default boolean isParameterized() {
        return false;
    }

    /**
     * Whether or not this type inherits from another type.
     * @return true or false.
     */
    boolean hasSuperType();

    /**
     * Constructs a new instance of the type.
     * @return Thew newly construct instance.
     * @throws InstantiationException If construction failed.
     * @throws IllegalAccessException If the constructor could not be called.
     */
    T newInstance() throws InstantiationException, IllegalAccessException;

    /**
     * If this type represents an enumeration.
     * @return true or false.
     */
    boolean isEnum();

    /**
     * Returns the list of enumeration constants this type represents.
     * @return An array of enumeration constants.
     */
    T[] getEnumConstants();

    /**
     * If this type represents a static Java Class, return the underlying class.
     * @return The Java class or empty.
     */
    Optional<Class<T>> getUnderlyingClass();
}
