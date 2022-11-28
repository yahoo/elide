/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.scalars.ExtendedScalars.GraphQLBigDecimal;
import static graphql.scalars.ExtendedScalars.GraphQLBigInteger;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;

import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import com.yahoo.elide.core.utils.coerce.converters.ElideTypeConverter;

import graphql.Scalars;
import graphql.scalars.java.JavaPrimitives;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains methods that convert from a class to a GraphQL input or query type.
 */
@Slf4j
public class GraphQLConversionUtils {
    protected static final String KEY = "key";
    protected static final String VALUE = "value";
    protected static final String ERROR_MESSAGE = "Value should either be integer, String or float";

    private final Map<Type<?>, GraphQLScalarType> scalarMap = new HashMap<>();

    protected NonEntityDictionary nonEntityDictionary;
    protected EntityDictionary entityDictionary;

    private final Map<Type, GraphQLObjectType> outputConversions = new HashMap<>();
    private final Map<Type, GraphQLInputObjectType> inputConversions = new HashMap<>();
    private final Map<Type, GraphQLEnumType> enumConversions = new HashMap<>();
    private final Map<String, GraphQLList> mapConversions = new HashMap<>();
    private final GraphQLNameUtils nameUtils;

    public GraphQLConversionUtils(
            EntityDictionary entityDictionary,
            NonEntityDictionary nonEntityDictionary
    ) {
        this.entityDictionary = entityDictionary;
        this.nonEntityDictionary = nonEntityDictionary;
        this.nameUtils = new GraphQLNameUtils(entityDictionary);
        registerCustomScalars();
    }

    private void registerCustomScalars() {
        CoerceUtil.getSerdes().forEach((type, serde) -> {
            SerdeCoercing<?, ?> serdeCoercing = new SerdeCoercing<>(ERROR_MESSAGE, serde);
            ElideTypeConverter elideTypeConverter = serde.getClass().getAnnotation(ElideTypeConverter.class);
            String name = elideTypeConverter != null ? elideTypeConverter.name() : type.getSimpleName();
            String description = elideTypeConverter != null ? elideTypeConverter.description() : type.getSimpleName();
            scalarMap.put(ClassType.of(type), GraphQLScalarType.newScalar()
                            .name(name)
                            .description(description)
                            .coercing(serdeCoercing)
                            .build());

        });
    }

    /**
     * Converts any non-entity type to a GraphQLType.
     * @param clazz - the non-entity type.
     * @return the GraphQLType or null if there is a problem with the underlying model.
     */
    public GraphQLScalarType classToScalarType(Type<?> clazz) {
        if (clazz.equals(ClassType.of(int.class)) || clazz.equals(ClassType.of(Integer.class))) {
            return Scalars.GraphQLInt;
        } else if (clazz.equals(ClassType.of(boolean.class)) || clazz.equals(ClassType.of(Boolean.class))) {
            return Scalars.GraphQLBoolean;
        } else if (clazz.equals(ClassType.of(long.class)) || clazz.equals(ClassType.of(Long.class))) {
            return GraphQLBigInteger;
        } else if (clazz.equals(ClassType.of(float.class)) || clazz.equals(ClassType.of(Float.class))) {
            return Scalars.GraphQLFloat;
        } else if (clazz.equals(ClassType.of(double.class)) || clazz.equals(ClassType.of(Double.class))) {
            return GraphQLBigDecimal;
        } else if (clazz.equals(ClassType.of(short.class)) || clazz.equals(ClassType.of(Short.class))) {
            return Scalars.GraphQLInt;
        } else if (clazz.equals(ClassType.of(String.class)) || clazz.equals(ClassType.of(Object.class))) {
            return Scalars.GraphQLString;
        } else if (clazz.equals(ClassType.of(BigDecimal.class))) {
            return JavaPrimitives.GraphQLBigDecimal;
        }
        return otherClassToScalarType(clazz);
    }

    private GraphQLScalarType otherClassToScalarType(Type<?> clazz) {
        if (scalarMap.containsKey(clazz)) {
            return scalarMap.get(clazz);
        }
        if (ClassType.DATE_TYPE.isAssignableFrom(clazz)) {
            return GraphQLScalars.GRAPHQL_DATE_TYPE;
        }
        return null;
    }

    /**
     * Converts an enum to a GraphQLEnumType.
     * @param enumClazz the Enum to convert
     * @return A GraphQLEnum type for class.
     */
    public GraphQLEnumType classToEnumType(Type<?> enumClazz) {
        if (enumConversions.containsKey(enumClazz)) {
            return enumConversions.get(enumClazz);
        }

        Enum [] values = (Enum []) enumClazz.getEnumConstants();

        GraphQLEnumType.Builder builder = newEnum().name(nameUtils.toOutputTypeName(enumClazz));

        for (Enum value : values) {
            builder.value(value.toString(), value);
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
     * @return The created type.
     */
    public GraphQLList classToQueryMap(Type<?> keyClazz, Type<?> valueClazz, DataFetcher fetcher) {
        String mapName = nameUtils.toMapEntryOutputName(keyClazz, valueClazz);

        if (mapConversions.containsKey(mapName)) {
            return mapConversions.get(mapName);
        }

        GraphQLOutputType keyType = fetchScalarOrObjectOutput(keyClazz, fetcher);
        GraphQLOutputType valueType = fetchScalarOrObjectOutput(valueClazz, fetcher);

        GraphQLObjectType mapType = newObject()
                .name(mapName)
                .field(newFieldDefinition()
                        .name(KEY)
                        .type(keyType))
                .field(newFieldDefinition()
                        .name(VALUE)
                        .type(valueType))
                .build();

        GraphQLList outputMap = new GraphQLList(mapType);

        mapConversions.put(mapName, outputMap);

        return mapConversions.get(mapName);
    }

    /**
     * Creates a GraphQL Map Input type.  GraphQL doesn't support this natively.  We mimic
     * maps by creating a list of key/value pairs.
     * @param keyClazz The map key class
     * @param valueClazz The map value class
     * @return The created type.
     */
    public GraphQLList classToInputMap(Type<?> keyClazz,
                                       Type<?> valueClazz) {
        String mapName = nameUtils.toMapEntryInputName(keyClazz, valueClazz);

        if (mapConversions.containsKey(mapName)) {
            return mapConversions.get(mapName);
        }

        GraphQLInputType keyType = fetchScalarOrObjectInput(keyClazz);
        GraphQLInputType valueType = fetchScalarOrObjectInput(valueClazz);

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
    public GraphQLOutputType attributeToQueryObject(Type<?> parentClass,
                                                    Type<?> attributeClass,
                                                    String attribute,
                                                    DataFetcher fetcher) {
        return attributeToQueryObject(
                parentClass,
                attributeClass,
                attribute,
                fetcher,
                entityDictionary);
    }

    /**
     * Helper function which converts an attribute of an entity to a GraphQL Query Type.
     * @param parentClass The parent entity class (Can either be an elide entity or not).
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @param fetcher The data fetcher to associated with the newly created GraphQL Query Type
     *
     * @return A GraphQL Query Type (either newly created or singleton instance for class) or
     *         null if the underlying type cannot be converted.
     */
    protected GraphQLOutputType attributeToQueryObject(Type<?> parentClass,
                                                       Type<?> attributeClass,
                                                       String attribute,
                                                       DataFetcher fetcher,
                                                       EntityDictionary dictionary) {

        /* Determine if we've already processed this item. */
        if (outputConversions.containsKey(attributeClass)) {
            return outputConversions.get(attributeClass);
        }

        if (enumConversions.containsKey(attributeClass)) {
            return enumConversions.get(attributeClass);
        }

        /* We don't support Class */
        if (ClassType.CLASS_TYPE.isAssignableFrom(attributeClass)) {
            return null;
        }

        /* If the attribute is a map */
        if (ClassType.MAP_TYPE.isAssignableFrom(attributeClass)) {

            /* Extract the key and value types */
            Type<?> keyType = dictionary.getParameterizedType(parentClass, attribute, 0);
            Type<?> valueType = dictionary.getParameterizedType(parentClass, attribute, 1);

            return classToQueryMap(keyType, valueType, fetcher);

        /* If the attribute is a collection */
        } else if (ClassType.COLLECTION_TYPE.isAssignableFrom(attributeClass)) {
            /* Extract the collection type */
            Type<?> listType = dictionary.getParameterizedType(parentClass, attribute, 0);

            // If this is a collection of a boxed type scalar, we want to unwrap it properly
            return new GraphQLList(fetchScalarOrObjectOutput(listType, fetcher));
        }
        return fetchScalarOrObjectOutput(attributeClass, fetcher);
    }

    /**
     * Converts an attribute of an Elide entity to a GraphQL Input Type.
     * @param parentClass The elide entity class.
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @return A newly created GraphQL Input Type or null if the underlying type cannot be converted.
     */
    public GraphQLInputType attributeToInputObject(Type<?> parentClass,
                                                   Type<?> attributeClass,
                                                   String attribute) {
        return attributeToInputObject(
                parentClass,
                attributeClass,
                attribute,
                entityDictionary);
    }

    /**
     * Helper function which converts an attribute of an entity to a GraphQL Input Type.
     * @param parentClass The parent entity class (Can either be an elide entity or not).
     * @param attributeClass The attribute class.
     * @param attribute The name of the attribute.
     * @param dictionary The dictionary that contains the runtime type information for the parent class.
     *
     * @return A newly created GraphQL Input Type or null if the underlying type cannot be converted.
     */
    protected GraphQLInputType attributeToInputObject(Type<?> parentClass,
                                                      Type<?> attributeClass,
                                                      String attribute,
                                                      EntityDictionary dictionary) {

        /* Determine if we've already processed this attribute */
        if (inputConversions.containsKey(attributeClass)) {
            return inputConversions.get(attributeClass);
        }

        if (enumConversions.containsKey(attributeClass)) {
            return enumConversions.get(attributeClass);
        }

        /* We don't support Class */
        if (ClassType.CLASS_TYPE.isAssignableFrom(attributeClass)) {
            return null;
        }

        /* If the attribute is a map */
        if (ClassType.MAP_TYPE.isAssignableFrom(attributeClass)) {

            /* Extract the key and value types */
            Type<?> keyType = dictionary.getParameterizedType(parentClass, attribute, 0);
            Type<?> valueType = dictionary.getParameterizedType(parentClass, attribute, 1);

            return classToInputMap(keyType, valueType);

        /* If the attribute is a collection */
        } else if (ClassType.COLLECTION_TYPE.isAssignableFrom(attributeClass)) {

            /* Extract the collection type */
            Type<?> listType = dictionary.getParameterizedType(parentClass, attribute, 0);

            return new GraphQLList(fetchScalarOrObjectInput(listType));
        }
        return fetchScalarOrObjectInput(attributeClass);
    }

    /**
     * Converts a non Elide object into a GraphQL Query object.  Any attribute which cannot be converted is skipped.
     * @param clazz The non Elide object class
     * @param fetcher The data fetcher to assign the newly created GraphQL object
     * @return A newly created GraphQL object.
     */
    public GraphQLObjectType classToQueryObject(
            Type<?> clazz,
            DataFetcher fetcher) {
        log.info("Building query object for type: {}", clazz.getName());

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        /* Check if we've already converted this */
        if (outputConversions.containsKey(clazz)) {
            return outputConversions.get(clazz);
        }

        GraphQLObjectType.Builder objectBuilder = newObject();
        objectBuilder.name(nameUtils.toNonElideOutputTypeName(clazz));

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            Type<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition()
                    .name(attribute);

            GraphQLOutputType attributeType =
                    attributeToQueryObject(clazz,
                            attributeClass, attribute, fetcher, nonEntityDictionary);
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
     * @return A newly created GraphQL object.
     */
    public GraphQLInputObjectType classToInputObject(Type<?> clazz) {
        log.info("Building input object for type: {}", clazz.getName());

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        /* Check if we've already converted this */
        if (inputConversions.containsKey(clazz)) {
            return inputConversions.get(clazz);
        }

        GraphQLInputObjectType.Builder objectBuilder = newInputObject();
        objectBuilder.name(nameUtils.toNonElideInputTypeName(clazz));

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            log.info("Building input object attribute: {}", attribute);
            Type<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            GraphQLInputObjectField.Builder fieldBuilder = newInputObjectField()
                    .name(attribute);

            GraphQLInputType attributeType =
                    attributeToInputObject(clazz, attributeClass, attribute, nonEntityDictionary);

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

    /**
     * Build an Argument list object for the given attribute.
     * @param entityClass The Entity class to which this attribute belongs to.
     * @param attribute The name of the attribute.
     * @param fetcher The data fetcher to associated with the newly created GraphQL Query Type.
     * @return Newly created GraphQLArgument Collection for the given attribute.
     */
    public List<GraphQLArgument> attributeArgumentToQueryObject(Type<?> entityClass,
                                                                String attribute,
                                                                DataFetcher fetcher) {
        return attributeArgumentToQueryObject(entityClass, attribute, fetcher, entityDictionary);
    }

    /**
     * Build an Argument list object for the given attribute.
     * @param entityClass The Entity class to which this attribute belongs to.
     * @param attribute The name of the attribute.
     * @param fetcher The data fetcher to associated with the newly created GraphQL Query Type
     * @param dictionary The dictionary that contains the runtime type information for the parent class.
     * @return Newly created GraphQLArgument Collection for the given attribute
     */
    public List<GraphQLArgument> attributeArgumentToQueryObject(Type<?> entityClass,
                                                                String attribute,
                                                                DataFetcher fetcher,
                                                                EntityDictionary dictionary) {
        return dictionary.getAttributeArguments(entityClass, attribute)
                .stream()
                .map(argumentType -> newArgument()
                        .name(argumentType.getName())
                        .type(fetchScalarOrObjectInput(argumentType.getType()))
                        .defaultValue(argumentType.getDefaultValue())
                        .build())
                .collect(Collectors.toList());

    }

    /**
     * Build an Argument list object for the given entity.
     * @param entityClass The Entity class to which this attribute belongs to.
     * @param dictionary The dictionary that contains the runtime type information for the parent class.
     * @return Newly created GraphQLArgument Collection for the given entity
     */
    public List<GraphQLArgument> entityArgumentToQueryObject(Type<?> entityClass,
                                                                EntityDictionary dictionary) {
        return dictionary.getEntityArguments(entityClass)
                .stream()
                .map(argumentType -> newArgument()
                        .name(argumentType.getName())
                        .type(fetchScalarOrObjectInput(argumentType.getType()))
                        .defaultValue(argumentType.getDefaultValue())
                        .build())
                .collect(Collectors.toList());

    }


    private GraphQLOutputType fetchScalarOrObjectOutput(Type<?> conversionClass,
                                                        DataFetcher fetcher) {
        /* If class is enum, provide enum type */
        if (conversionClass.isEnum()) {
            return classToEnumType(conversionClass);
        }

        /* Attempt to convert a scalar type */
        GraphQLOutputType outputType = classToScalarType(conversionClass);
        if (outputType == null) {
                /* Attempt to convert an object type */
            outputType = classToQueryObject(conversionClass, fetcher);
        }
        return outputType;
    }

    private GraphQLInputType fetchScalarOrObjectInput(Type<?> conversionClass) {
        /* If class is enum, provide enum type */
        if (conversionClass.isEnum()) {
            return classToEnumType(conversionClass);
        }

        /* Attempt to convert a scalar type */
        GraphQLInputType inputType = classToScalarType(conversionClass);
        if (inputType == null) {

                /* Attempt to convert an object type */
            inputType = classToInputObject(conversionClass);
        }
        return inputType;
    }

    public Set<GraphQLObjectType> getObjectTypes() {
        Set<GraphQLObjectType> allObjects = mapConversions.values().stream()
                .map(GraphQLList::getWrappedType)
                .filter(type -> type instanceof GraphQLObjectType)
                .map(GraphQLObjectType.class::cast)
                .collect(Collectors.toSet());

        allObjects.addAll(outputConversions.values());
        return allObjects;
    }
}
