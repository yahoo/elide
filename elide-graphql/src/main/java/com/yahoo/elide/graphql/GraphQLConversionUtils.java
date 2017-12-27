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
import java.util.Date;
import java.util.HashMap;
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

    private final Map<Class, GraphQLObjectType> outputConversions = new HashMap<>();
    private final Map<Class, GraphQLInputObjectType> inputConversions = new HashMap<>();
    private final Map<Class, GraphQLEnumType> enumConversions = new HashMap<>();
    private final Map<String, GraphQLList> mapConversions = new HashMap<>();

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
            if (generatedTypes.contains(candidate)) {
                log.warn("Cycle detected: {}", candidate);
                return true;
            }
            return false;
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
        } else if (Date.class.isAssignableFrom(clazz)) {
            return GraphQLScalars.GRAPHQL_DATE_TYPE;
        }

        return null;
    }

    /**
     * Converts an enum to a GraphQLEnumType.
     * @param enumClazz the Enum to convert
     * @return A GraphQLEnum type for class.
     */
    public GraphQLEnumType classToEnumType(Class<?> enumClazz) {
        if (enumConversions.containsKey(enumClazz)) {
            return enumConversions.get(enumClazz);
        }

        Enum [] values = (Enum []) enumClazz.getEnumConstants();

        GraphQLEnumType.Builder builder = newEnum().name(toValidNameName(enumClazz.getName()));

        for (Enum value : values) {
            builder.value(toValidNameName(value.name()), value);
        }

        GraphQLEnumType enumResult = builder.build();

        enumConversions.put(enumClazz, enumResult);

        return enumResult;
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
        String mapName = toValidNameName(keyClazz.getName() + valueClazz.getCanonicalName() + MAP);

        if (mapConversions.containsKey(mapName)) {
            return mapConversions.get(mapName);
        }

        GraphQLOutputType keyType = keyClazz.isEnum()
                ? classToEnumType(keyClazz) : classToQueryObject(keyClazz, ledger, fetcher);
        GraphQLOutputType valueType = valueClazz.isEnum()
                ? classToEnumType(valueClazz) : classToQueryObject(valueClazz, ledger, fetcher);
        GraphQLList outputMap = new GraphQLList(
            newObject()
                .name(mapName)
                .field(newFieldDefinition()
                        .name(KEY)
                        .dataFetcher(fetcher)
                        .type(keyType))
                .field(newFieldDefinition()
                        .name(VALUE)
                        .dataFetcher(fetcher)
                        .type(valueType))
                .build()
        );

        mapConversions.put(mapName, outputMap);

        return mapConversions.get(mapName);
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
        String mapName = toValidNameName("__input__" + keyClazz.getName() + valueClazz.getCanonicalName() + MAP);

        if (mapConversions.containsKey(mapName)) {
            return mapConversions.get(mapName);
        }

        GraphQLInputType keyType = keyClazz.isEnum() ? classToEnumType(keyClazz) : classToInputObject(keyClazz, ledger);
        GraphQLInputType valueType = valueClazz.isEnum()
                ? classToEnumType(valueClazz) : classToInputObject(valueClazz, ledger);
        GraphQLList inputMap = new GraphQLList(
            newInputObject()
                .name(mapName)
                .field(newInputObjectField()
                        .name(KEY)
                        .type(keyType))
                .field(newInputObjectField()
                        .name(VALUE)
                        .type(valueType))
                .build()
        );

        mapConversions.put(mapName, inputMap);

        return inputMap;
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
     * @return A GraphQL Query Type (either newly created or singleton instance for class) or
     *         null if the underlying type cannot be converted.
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

        /* Determine if we've already processed this item. */
        if (outputConversions.containsKey(attributeClass)) {
            return outputConversions.get(attributeClass);
        }

        if (enumConversions.containsKey(attributeClass)) {
            return enumConversions.get(attributeClass);
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

            // If this is a collection of a boxed type scalar, we want to unwrap it properly
            GraphQLOutputType outputType;

            if (listType.isEnum()) {
                outputType = classToEnumType(listType);
            } else {
                outputType = fetchScalarOrObjectOutput(listType, ledger, fetcher);
            }

            return new GraphQLList(outputType);

        /* If the attribute is an enum */
        } else if (attributeClass.isEnum()) {
            return classToEnumType(attributeClass);
        } else {
            return fetchScalarOrObjectOutput(attributeClass, ledger, fetcher);
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
     * Helper function converts a string into a valid name.
     *
     * @param input Input string
     * @return Sanitized form of input string
     */
    protected String toValidNameName(String input) {
        return input
                .replace(".", "_") // Replaces package qualifier on class names
                .replace("$", "__1") // Replaces inner-class qualifier
                .replace("[", "___2"); // Replaces primitive list qualifier ([B == array of bytes)
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

        /* Determine if we've already processed this attribute */
        if (inputConversions.containsKey(attributeClass)) {
            return inputConversions.get(attributeClass);
        }

        if (enumConversions.containsKey(attributeClass)) {
            return enumConversions.get(attributeClass);
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

            GraphQLInputType inputType;

            if (listType.isEnum()) {
                inputType = classToEnumType(listType);
            } else {
                inputType = fetchScalarOrObjectInput(listType, ledger);
            }

            return new GraphQLList(inputType);

        /* If the attribute is an enum */
        } else if (attributeClass.isEnum()) {
                return classToEnumType(attributeClass);
        } else {
            return fetchScalarOrObjectInput(attributeClass, ledger);
        }
    }

    /**
     * Converts a non Elide object into a GraphQL Query object.  Any attribute which cannot be converted is skipped.
     * @param clazz The non Elide object class
     * @param ledger Keeps track of cycles
     * @param fetcher The data fetcher to assign the newly created GraphQL object
     * @return A newly created GraphQL object.
     */
    public GraphQLObjectType classToQueryObject(
            Class<?> clazz,
            ConversionLedger ledger,
            DataFetcher fetcher) {
        log.info("Building query object for type: {}", clazz.getName());
        if (ledger.isAlreadyConverted(clazz)) {
            throw new IllegalArgumentException("Cycle detected when generating type for class" + clazz.getName());
        }
        ledger.recordConversion(clazz);

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        /* Check if we've already converted this */
        if (outputConversions.containsKey(clazz)) {
            return outputConversions.get(clazz);
        }

        GraphQLObjectType.Builder objectBuilder = newObject();
        objectBuilder.name(toValidNameName(clazz.getName()));

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            Class<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(fetcher);

            GraphQLOutputType attributeType =
                    attributeToQueryObject(clazz,
                            attributeClass, attribute, fetcher, nonEntityDictionary, ledger);
            if (attributeType == null) {
                continue;
            }

            fieldBuilder.type(attributeType);

            objectBuilder.field(fieldBuilder);
        }

        GraphQLObjectType object = objectBuilder.build();

        outputConversions.put(clazz, object);

        return object;
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

        /* Check if we've already converted this */
        if (inputConversions.containsKey(clazz)) {
            return inputConversions.get(clazz);
        }

        GraphQLInputObjectType.Builder objectBuilder = newInputObject();
        objectBuilder.name(toValidNameName("__input__" + clazz.getName()));

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

        GraphQLInputObjectType object = objectBuilder.build();

        inputConversions.put(clazz, object);

        return object;
    }

    private GraphQLOutputType fetchScalarOrObjectOutput(Class<?> conversionClass,
                                                        ConversionLedger ledger,
                                                        DataFetcher fetcher) {
        /* Attempt to convert a scalar type */
        GraphQLOutputType outputType = classToScalarType(conversionClass);
        if (outputType == null) {
                /* Attempt to convert an object type */
            outputType = classToQueryObject(conversionClass, ledger, fetcher);
        }
        return outputType;
    }

    private GraphQLInputType fetchScalarOrObjectInput(Class<?> conversionClass,
                                                      ConversionLedger ledger) {
        /* Attempt to convert a scalar type */
        GraphQLInputType inputType = classToScalarType(conversionClass);
        if (inputType == null) {

                /* Attempt to convert an object type */
            inputType = classToInputObject(conversionClass, ledger);
        }
        return inputType;
    }
}
