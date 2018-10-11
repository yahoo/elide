/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import static graphql.Assert.assertNotNull;

import graphql.AssertException;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Basically the same class as GraphQLInputObjectType except fields can be added after the
 * object is constructed.  This mutable behavior is useful for constructing input objects from
 * object graphs with cycles.
 *
 * Portions of this file are taken from GraphQL-Java which has the following license:
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Andreas Marek and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
