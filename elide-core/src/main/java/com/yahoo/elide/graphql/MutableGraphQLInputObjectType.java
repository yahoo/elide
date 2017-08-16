/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import graphql.AssertException;
import graphql.schema.BuilderFunction;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;


/**
 * Basically the same class as GraphQLInputObjectType except fields can be added after the
 * object is constructed.  This mutable behavior is useful for constructing input objects from
 * object graphs with cycles.
 */
public class MutableGraphQLInputObjectType extends GraphQLInputObjectType {

    private final Map<String, GraphQLInputObjectField> fieldMap = new LinkedHashMap<String, GraphQLInputObjectField>();
    private String name;

    public MutableGraphQLInputObjectType(String name, String description, List<GraphQLInputObjectField> fields) {
        super(name, description, fields);
        this.name = name;
        buildMap(fields);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void buildMap(List<GraphQLInputObjectField> fields) {
        for (GraphQLInputObjectField field : fields) {
            String name = field.getName();
            if (fieldMap.containsKey(name)) {
                throw new AssertException("field " + name + " redefined");
            }
            fieldMap.put(name, field);
        }
    }

    public void setField(String name, GraphQLInputObjectField field) {
        fieldMap.put(name, field);
    }

    @Override
    public List<GraphQLInputObjectField> getFields() {
        return new ArrayList<GraphQLInputObjectField>(fieldMap.values());
    }

    public GraphQLInputObjectField getField(String name) {
        return fieldMap.get(name);
    }

    public static Builder newMutableInputObject() {
        return new Builder();
    }

    /**
     * Builder for constructing MutableGraphQLInputObjectType
     */
    public static class Builder {
        private String name;
        private String description;
        private List<GraphQLInputObjectField> fields = new ArrayList<GraphQLInputObjectField>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder field(GraphQLInputObjectField field) {
            assertNotNull(field, "field can't be null");
            fields.add(field);
            return this;
        }

        /**
         * Take a field builder in a function definition and apply. Can be used in a jdk8 lambda
         * e.g.:
         * <pre>
         *     {@code
         *      field(f -> f.name("fieldName"))
         *     }
         * </pre>
         *
         * @param builderFunction a supplier for the builder impl
         * @return this
         */
        public Builder field(BuilderFunction<GraphQLInputObjectField.Builder> builderFunction) {
            assertNotNull(builderFunction, "builderFunction should not be null");
            GraphQLInputObjectField.Builder builder = GraphQLInputObjectField.newInputObjectField();
            builder = builderFunction.apply(builder);
            return field(builder);
        }

        /**
         * Same effect as the field(GraphQLFieldDefinition). Builder.build() is called
         * from within
         *
         * @param builder an un-built/incomplete GraphQLFieldDefinition
         * @return this
         */
        public Builder field(GraphQLInputObjectField.Builder builder) {
            this.fields.add(builder.build());
            return this;
        }

        public Builder fields(List<GraphQLInputObjectField> fields) {
            for (GraphQLInputObjectField field : fields) {
                field(field);
            }
            return this;
        }

        public MutableGraphQLInputObjectType build() {
            return new MutableGraphQLInputObjectType(name, description, fields);
        }
    }
}
