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
 * Generates Objects
 */
@Slf4j
public class ObjectGenerator {
    protected NonEntityDictionary nonEntityDictionary = new NonEntityDictionary();
    protected EntityDictionary entityDictionary;

    public ObjectGenerator(EntityDictionary dictionary) {
        this.entityDictionary = dictionary;
    }

    /**
     * Context
     */
    public class GeneratorContext {
        private Set<Class<?>> generatedTypes = new HashSet<>();

        public boolean hasConflict(Class<?> candidate) {
            return generatedTypes.contains(candidate);
        }

        public boolean addConflict(Class<?> candidate) {
            if (ClassUtils.isPrimitiveOrWrapper(candidate) || String.class.isAssignableFrom(candidate)) {
                return false;
            }

            generatedTypes.add(candidate);
            return true;
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

    public GraphQLEnumType classToEnumType(Class<?> enumClazz) {
        Enum [] values = (Enum []) enumClazz.getEnumConstants();

        GraphQLEnumType.Builder builder = newEnum()
                .name(enumClazz.getName());

        for (Enum value : values) {
            builder.value(value.name(), value);
        }

        return builder.build();
    }

    public  GraphQLList classToQueryMap(Class<?> keyClazz,
                                        Class<?> valueClazz,
                                        DataFetcher fetcher,
                                        GeneratorContext ctx) {
        return new GraphQLList(
            GraphQLObjectType.newObject()
                .name(keyClazz.getName() + valueClazz.getCanonicalName() + "Map")
                .field(newFieldDefinition()
                        .name("key")
                        .dataFetcher(fetcher)
                        .type(classToQueryObject(keyClazz, ctx, fetcher)))
                .field(newFieldDefinition()
                        .name("value")
                        .dataFetcher(fetcher)
                        .type(classToQueryObject(valueClazz, ctx, fetcher)))
                .build()
        );
    }

    public GraphQLList classToInputMap(Class<?> keyClazz,
                                       Class<?> valueClazz,
                                       GeneratorContext ctx) {
        return new GraphQLList(
            newInputObject()
                .name(keyClazz.getName() + valueClazz.getCanonicalName() + "Map")
                .field(newInputObjectField()
                        .name("key")
                        .type(classToInputObject(keyClazz, ctx)))
                .field(newInputObjectField()
                        .name("value")
                        .type(classToInputObject(valueClazz, ctx)))
                .build()
        );
    }

    public GraphQLOutputType classToQueryType(Class<?> parentClass,
                                              Class<?> attributeClass,
                                              String attribute,
                                              DataFetcher fetcher) {

        GeneratorContext ctx = new GeneratorContext();

        if (Map.class.isAssignableFrom(attributeClass)) {
            Class<?> keyType = entityDictionary.getParameterizedType(parentClass, attribute, 0);
            Class<?> valueType = entityDictionary.getParameterizedType(parentClass, attribute, 1);

            return classToQueryMap(keyType, valueType, fetcher, ctx);
        } else if (Collection.class.isAssignableFrom(attributeClass)) {
            Class<?> listType = entityDictionary.getParameterizedType(parentClass, attribute, 0);

            return (new GraphQLList(classToQueryObject(listType, ctx, fetcher)));
        } else if (attributeClass.isEnum()) {
            return classToEnumType(attributeClass);
        } else {
            GraphQLOutputType outputType = classToScalarType(attributeClass);
            if (outputType == null) {
                outputType = classToQueryObject(attributeClass, ctx, fetcher);
            }
            return outputType;
        }
    }

    public GraphQLInputType classToInputType(Class<?> parentClass,
                                             Class<?> attributeClass,
                                             String attribute) {

        GeneratorContext ctx = new GeneratorContext();

        if (Map.class.isAssignableFrom(attributeClass)) {
                Class<?> keyType = entityDictionary.getParameterizedType(parentClass, attribute, 0);
                Class<?> valueType = entityDictionary.getParameterizedType(parentClass, attribute, 1);

                return classToInputMap(keyType, valueType, ctx);
        } else if (Collection.class.isAssignableFrom(attributeClass)) {
                Class<?> listType = entityDictionary.getParameterizedType(parentClass, attribute, 0);

                return new GraphQLList(classToInputObject(listType, ctx));
        } else if (attributeClass.isEnum()) {
                return classToEnumType(attributeClass);
        } else {
                GraphQLInputType inputType = classToScalarType(attributeClass);
                if (inputType == null) {
                    inputType = classToInputObject(attributeClass, ctx);
                }
                return inputType;
        }
    }

    public GraphQLObjectType classToQueryObject(Class<?> clazz, GeneratorContext ctx, DataFetcher fetcher) {
        log.info("Building query object for type: {}", clazz.getName());
        if (ctx.hasConflict(clazz)) {
            throw new IllegalArgumentException("Cycle detected when generating type for class" + clazz.getName());
        }
        ctx.addConflict(clazz);

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        GraphQLObjectType.Builder objectBuilder = newObject();
        objectBuilder.name(clazz.getName());

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            Class<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            if (ctx.hasConflict(attributeClass)) {
                log.error("Cycle detected: {}", attributeClass);
                continue;
            }

            if (Class.class.isAssignableFrom(attributeClass)) {
                continue;
            }

            GraphQLFieldDefinition.Builder fieldBuilder = newFieldDefinition()
                    .name(attribute)
                    .dataFetcher(fetcher);

            if (Map.class.isAssignableFrom(attributeClass)) {
                Class<?> keyType = nonEntityDictionary.getParameterizedType(clazz, attribute, 0);
                Class<?> valueType = nonEntityDictionary.getParameterizedType(clazz, attribute, 1);

                if (ctx.hasConflict(keyType) || ctx.hasConflict(valueType)) {
                    log.error("Cycle detected: Map {} {}", keyType, valueType);
                    continue;
                }

                fieldBuilder.type(classToQueryMap(keyType, valueType, fetcher, ctx));
            } else if (Collection.class.isAssignableFrom(attributeClass)) {
                Class<?> listType = nonEntityDictionary.getParameterizedType(clazz, attribute, 0);

                if (ctx.hasConflict(listType)) {
                    log.error("Cycle detected: {}", listType);
                    continue;
                }

                fieldBuilder.type(new GraphQLList(classToQueryObject(listType, ctx, fetcher)));
            } else if (attributeClass.isEnum()) {
                fieldBuilder.type(classToEnumType(attributeClass));
            } else {
                GraphQLOutputType outputType = classToScalarType(attributeClass);
                if (outputType == null) {
                    outputType = classToQueryObject(attributeClass, ctx, fetcher);
                }
                fieldBuilder.type(outputType);
            }

            objectBuilder.field(fieldBuilder);
        }
        return objectBuilder.build();
    }

    public GraphQLInputObjectType classToInputObject(Class<?> clazz, GeneratorContext ctx) {
        log.info("Building input object for type: {}", clazz.getName());
        if (ctx.hasConflict(clazz)) {
            throw new IllegalArgumentException("Cycle2 detected when generating type for class" + clazz.getName());
        }
        ctx.addConflict(clazz);

        if (!nonEntityDictionary.hasBinding(clazz)) {
            nonEntityDictionary.bindEntity(clazz);
        }

        GraphQLInputObjectType.Builder objectBuilder = newInputObject();
        objectBuilder.name(clazz.getName() + "Input");

        for (String attribute : nonEntityDictionary.getAttributes(clazz)) {
            log.info("Building input object attribute: {}", attribute);
            Class<?> attributeClass = nonEntityDictionary.getType(clazz, attribute);

            if (ctx.hasConflict(attributeClass)) {
                log.error("Cycle detected: {}", attributeClass);
                continue;
            }

            if (Class.class.isAssignableFrom(attributeClass)) {
                continue;
            }

            GraphQLInputObjectField.Builder fieldBuilder = newInputObjectField()
                    .name(attribute);

            if (Map.class.isAssignableFrom(attributeClass)) {
                Class<?> keyType = nonEntityDictionary.getParameterizedType(clazz, attribute, 0);
                Class<?> valueType = nonEntityDictionary.getParameterizedType(clazz, attribute, 1);
                if (ctx.hasConflict(keyType) || ctx.hasConflict(valueType)) {
                    log.error("Cycle detected: Map {} {}", keyType, valueType);
                    continue;
                }

                fieldBuilder.type(classToInputMap(keyType, valueType, ctx));
            } else if (Collection.class.isAssignableFrom(attributeClass)) {
                Class<?> listType = nonEntityDictionary.getParameterizedType(clazz, attribute, 0);
                if (ctx.hasConflict(listType)) {
                    log.error("Cycle detected: {}", listType);
                    continue;
                }

                fieldBuilder.type(new GraphQLList(classToInputObject(listType, ctx)));
            } else if (attributeClass.isEnum()) {
                fieldBuilder.type(classToEnumType(attributeClass));
            } else {
                GraphQLInputType inputType = classToScalarType(attributeClass);
                if (inputType == null) {
                    inputType = classToInputObject(attributeClass, ctx);
                }
                fieldBuilder.type(inputType);
            }

            objectBuilder.field(fieldBuilder);
        }

        return objectBuilder.build();
    }
}
