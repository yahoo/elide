/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.core.EntityDictionary;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * Contains methods that convert from a class to a GraphQL input or query type.
 */
@Slf4j
public class GraphQLConversionUtils {
    protected static final String MAP = "Map";
    protected static final String KEY = "key";
    protected static final String VALUE = "value";

    protected NonEntityDictionary nonEntityDictionary = new NonEntityDictionary();
    protected EntityDictionary entityDictionary;

    public GraphQLConversionUtils(EntityDictionary dictionary) {
        this.entityDictionary = dictionary;
    }

    /**
     * Keeps track of which non-primitive classes have been converted to GraphQL already.
     * This is primarily used to detect and avoid cycles.
     */
    public static class ConversionLedger {
        private Set<Class<?>> generatedTypes = new HashSet<>();

        /**
         * @param candidate The class to check if it has already been converted.
         * @return
         */
        public boolean isAlreadyConverted(Class<?> candidate) {
            log.warn("Cycle detected: {}", candidate);
            return generatedTypes.contains(candidate);
        }

        /**
         * Marks that a given entity has been converted to GraphQL.
         * @param converted - The entity class which was just converted to GraphQL.
         */
        public void recordConversion(Class<?> converted) {
            if (ClassUtils.isPrimitiveOrWrapper(converted) || String.class.isAssignableFrom(converted)) {
                return;
            }

            generatedTypes.add(converted);
        }
    }

    /**
     * Converts any non-entity type to a GraphQLType
     * @param clazz - the non-entity type.
     * @return the GraphQLType or null if there is a problem with the underlying model.
     */
    public GraphQLScalarType classToScalarType(Class<?> clazz) {
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            return Scalars.GraphQLInt;
        } else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            return Scalars.GraphQLBoolean;
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            return Scalars.GraphQLLong;
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            return Scalars.GraphQLFloat;
        } else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            return Scalars.GraphQLFloat;
        } else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            return Scalars.GraphQLShort;
        } else if (clazz.equals(String.class)) {
            return Scalars.GraphQLString;
        }

        return null;
    }

    /**
     * Converts an enum to a GraphQLEnumType.
     * @param enumClazz the Enum to convert
     * @return A new GraphQLEnum type.
     */
    public GraphQLEnumType classToEnumType(Class<?> enumClazz) {
        Enum [] values = (Enum []) enumClazz.getEnumConstants();

        GraphQLEnumType.Builder builder = newEnum()
            .name(enumClazz.getName());

        for (Enum value : values) {
            builder.value(value.name(), value);
        }

        return builder.build();
    }

    /**
     * Creates a GraphQL Map Query type.  GraphQL doesn't support this natively.  We mimic
     * maps by creating a list of key/value pairs.
     * @param keyClazz The map key class
     * @param valueClazz The map value class
     * @param fetcher The Datafetcher to assign to the created GraphQL object.
     * @param ledger Used to detect cycles.
     * @return The created type.
     */
    public  GraphQLList classToQueryMap(Class<?> keyClazz,
                                        Class<?> valueClazz,
                                        DataFetcher fetcher,
                                        ConversionLedger ledger) {
        return new GraphQLList(
            newObject()
                .name(keyClazz.getName() + valueClazz.getCanonicalName() + MAP)
                .field(newFieldDefinition()
                        .name(KEY)
                        .dataFetcher(fetcher)
                        .type(classToQueryObject(keyClazz, ledger, fetcher)))
                .field(newFieldDefinition()
                        .name(VALUE)
                        .dataFetcher(fetcher)
                        .type(classToQueryObject(valueClazz, ledger, fetcher)))
                .build()
        );
    }

    /**
     * Creates a GraphQL Map Input type.  GraphQL doesn't support this natively.  We mimic
     * maps by creating a list of key/value pairs.
     * @param keyClazz The map key class
     * @param valueClazz The map value class
     * @param ledger Used to detect cycles.
     * @return The created type.
     */
    public GraphQLList classToInputMap(Class<?> keyClazz,
                                       Class<?> valueClazz,
                                       ConversionLedger ledger) {
        return new GraphQLList(
            newInputObject()
                .name(keyClazz.getName() + valueClazz.getCanonicalName() + MAP)
                .field(newInputObjectField()
                        .name(KEY)
                        .type(classToInputObject(keyClazz, ledger)))
                .field(newInputObjectField()
                        .name(VALUE)
                        .type(classToInputObject(valueClazz, ledger)))
                .build()
        );
    }

    /**
     * Converts an attribute of an Elide entity to a GraphQL Query Type.
     * @param parentClass The elide entity class.
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @param fetcher The data fetcher to associated with the newly created GraphQL Query Type
     * @return A newly created GraphQL Query Type or null if the underlying type cannot be converted.
     */
    public GraphQLOutputType attributeToQueryObject(Class<?> parentClass,
                                                    Class<?> attributeClass,
                                                    String attribute,
                                                    DataFetcher fetcher) {
        return attributeToQueryObject(
                parentClass,
                attributeClass,
                attribute,
                fetcher,
                entityDictionary,
                new ConversionLedger());
    }

    /**
     * Helper function which converts an attribute of an entity to a GraphQL Query Type.
     * @param parentClass The parent entity class (Can either be an elide entity or not).
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @param fetcher The data fetcher to associated with the newly created GraphQL Query Type
     * @param dictionary The dictionary that contains the runtime type information for the parent class.
     * @param ledger Keeps track of which entities have already been converted.
     *
     * @return A newly created GraphQL Query Type or null if the underlying type cannot be converted.
     */
    protected GraphQLOutputType attributeToQueryObject(Class<?> parentClass,
                                                    Class<?> attributeClass,
                                                    String attribute,
                                                    DataFetcher fetcher,
                                                    EntityDictionary dictionary,
                                                    ConversionLedger ledger) {

        /* Detect cycles */
        if (ledger.isAlreadyConverted(attributeClass)) {
            return null;
        }

        /* We don't support Class */
        if (Class.class.isAssignableFrom(attributeClass)) {
            return null;
        }

        /* If the attribute is a map */
        if (Map.class.isAssignableFrom(attributeClass)) {

            /* Extract the key and value types */
            Class<?> keyType = dictionary.getParameterizedType(parentClass, attribute, 0);
            Class<?> valueType = dictionary.getParameterizedType(parentClass, attribute, 1);

            /* Detect cycles on key and value types */
            if (ledger.isAlreadyConverted(keyType) || ledger.isAlreadyConverted(valueType)) {
                return null;
            }

            return classToQueryMap(keyType, valueType, fetcher, ledger);

        /* If the attribute is a collection */
        } else if (Collection.class.isAssignableFrom(attributeClass)) {
            /* Extract the collection type */
            Class<?> listType = dictionary.getParameterizedType(parentClass, attribute, 0);

            /* Detect cycles */
            if (ledger.isAlreadyConverted(listType)) {
                return null;
            }

            return (new GraphQLList(classToQueryObject(listType, ledger, fetcher)));

        /* If the attribute is an enum */
        } else if (attributeClass.isEnum()) {
            return classToEnumType(attributeClass);
        } else {

            /* Attempt to convert a scalar type */
            GraphQLOutputType outputType = classToScalarType(attributeClass);
            if (outputType == null) {

                /* Attempt to convert an object type */
                outputType = classToQueryObject(attributeClass, ledger, fetcher);
            }
            return outputType;
        }
    }

    /**
     * Converts an attribute of an Elide entity to a GraphQL Input Type.
     * @param parentClass The elide entity class.
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @return A newly created GraphQL Input Type or null if the underlying type cannot be converted.
     */
    public GraphQLInputType attributeToInputObject(Class<?> parentClass,
                                                   Class<?> attributeClass,
                                                   String attribute) {
        return attributeToInputObject(
                parentClass,
                attributeClass,
                attribute,
                entityDictionary,
                new ConversionLedger());
    }

    /**
     * Helper function which converts an attribute of an entity to a GraphQL Input Type.
     * @param parentClass The parent entity class (Can either be an elide entity or not).
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @param dictionary The dictionary that contains the runtime type information for the parent class.
     * @param ledger Keeps track of which entities have already been converted.
     *
     * @return A newly created GraphQL Input Type or null if the underlying type cannot be converted.
     */
    protected GraphQLInputType attributeToInputObject(Class<?> parentClass,
                                                   Class<?> attributeClass,
                                                   String attribute,
                                                   EntityDictionary dictionary,
                                                   ConversionLedger ledger) {

        /* Detect cycles */
        if (ledger.isAlreadyConverted(attributeClass)) {
            return null;
        }

        /* We don't support Class */
        if (Class.class.isAssignableFrom(attributeClass)) {
            return null;
        }

        /* If the attribute is a map */
        if (Map.class.isAssignableFrom(attributeClass)) {

            /* Extract the key and value types */
            Class<?> keyType = dictionary.getParameterizedType(parentClass, attribute, 0);
            Class<?> valueType = dictionary.getParameterizedType(parentClass, attribute, 1);

            /* Detect cycles on key and value types */
            if (ledger.isAlreadyConverted(keyType) || ledger.isAlreadyConverted(valueType)) {
                return null;
            }

            return classToInputMap(keyType, valueType, ledger);

        /* If the attribute is a collection */
        } else if (Collection.class.isAssignableFrom(attributeClass)) {

            /* Extract the collection type */
            Class<?> listType = dictionary.getParameterizedType(parentClass, attribute, 0);

            /* Detect Cycles */
            if (ledger.isAlreadyConverted(listType)) {
                return null;
            }

            return new GraphQLList(classToInputObject(listType, ledger));

        /* If the attribute is an enum */
        } else if (attributeClass.isEnum()) {
                return classToEnumType(attributeClass);
        } else {

            /* Attempt to convert a scalar type */
            GraphQLInputType inputType = classToScalarType(attributeClass);
            if (inputType == null) {

                /* Attempt to convert an object type */
                inputType = classToInputObject(attributeClass, ledger);
            }
            return inputType;
        }
    }

    /**
     * Converts a non Elide object into a GraphQL Query object.  Any attribute which cannot be converted is skipped.
     * @param clazz The non Elide object class
     * @param ledger Keeps track of cycles
     * @param fetcher The data fetcher to assign the newly created GraphQL object
     * @return A newly created GraphQL object.
     */
    public GraphQLObjectType classToQueryObject(Class<?> clazz, ConversionLedger ledger, DataFetcher fetcher) {
        log.info("Building query object for type: {}", clazz.getName());
        if (ledger.isAlreadyConverted(clazz)) {
            throw new IllegalArgumentException("Cycle detected when generating type for class" + clazz.getName());
        }
        ledger.recordConversion(clazz);

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        GraphQLObjectType.Builder objectBuilder = newObject();
        objectBuilder.name(clazz.getName());

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            Class<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(fetcher);

            GraphQLOutputType attributeType =
                    attributeToQueryObject(clazz, attributeClass, attribute, fetcher, nonEntityDictionary, ledger);
            if (attributeType == null) {
                continue;
            }

            fieldBuilder.type(attributeType);

            objectBuilder.field(fieldBuilder);
        }
        return objectBuilder.build();
    }

    /**
     * Converts a non Elide object into a Input Query object.  Any attribute which cannot be converted is skipped.
     * @param clazz The non Elide object class
     * @param ledger Keeps track of cycles
     * @return A newly created GraphQL object.
     */
    public GraphQLInputObjectType classToInputObject(Class<?> clazz, ConversionLedger ledger) {
        log.info("Building input object for type: {}", clazz.getName());
        if (ledger.isAlreadyConverted(clazz)) {
            throw new IllegalArgumentException("Cycle2 detected when generating type for class" + clazz.getName());
        }
        ledger.recordConversion(clazz);

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        GraphQLInputObjectType.Builder objectBuilder = newInputObject();
        objectBuilder.name(clazz.getName() + "Input");

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            log.info("Building input object attribute: {}", attribute);
            Class<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            GraphQLInputObjectField.Builder fieldBuilder = newInputObjectField()
                    .name(attribute);

            GraphQLInputType attributeType =
                    attributeToInputObject(clazz, attributeClass, attribute, nonEntityDictionary, ledger);

            if (attributeType == null) {
                continue;
            }
            fieldBuilder.type(attributeType);
            objectBuilder.field(fieldBuilder);
        }

        return objectBuilder.build();
    }
}
