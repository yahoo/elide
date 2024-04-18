/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql;

import com.paiondata.elide.test.graphql.elements.Argument;
import com.paiondata.elide.test.graphql.elements.Arguments;
import com.paiondata.elide.test.graphql.elements.Definition;
import com.paiondata.elide.test.graphql.elements.Document;
import com.paiondata.elide.test.graphql.elements.Edges;
import com.paiondata.elide.test.graphql.elements.Field;
import com.paiondata.elide.test.graphql.elements.Mutation;
import com.paiondata.elide.test.graphql.elements.Node;
import com.paiondata.elide.test.graphql.elements.Query;
import com.paiondata.elide.test.graphql.elements.Selection;
import com.paiondata.elide.test.graphql.elements.SelectionSet;
import com.paiondata.elide.test.graphql.elements.VariableDefinition;
import com.paiondata.elide.test.graphql.elements.VariableDefinitions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link GraphQLDSL} programmatically construct GraphQL query(excluding query variables)
 * and serializes it to a GraphQL query string.
 * <p>
 * For example,
 * <pre>
 * {@code
 * String graphQLQuery = document(
 *         query(
 *                 "myQuery",
 *                 variableDefinitions(
 *                         variableDefinition("bookId", "[String]")
 *                 ),
 *                 selections(
 *                         field(
 *                                 "book",
 *                                 arguments(
 *                                         argument("ids", "$bookId")
 *                                 ),
 *                                 selections(
 *                                         field("id"),
 *                                         field("title"),
 *                                         field(
 *                                                 "authors",
 *                                                 selection(
 *                                                         field("name")
 *                                                 )
 *                                         )
 *                                 )
 *                         )
 *                 )
 *                 )
 *         ).toQuery();
 * }
 * </pre>
 * will produces the following query
 * <pre>
 * {@code
 * query myQuery($bookId: [String]) {
 *     book(ids: $bookId) {
 *         edges {
 *             node {
 *                 id
 *                 title
 *                 authors {
 *                     edges {
 *                         node {
 *                             name
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * }
 * </pre>
 * In addition, {@link GraphQLDSL} also allows programmatically constructing GraphQL response data. For example,
 * <pre>
 * {@code
 * document(
 *         selection(
 *                 field(
 *                         "book",
 *                         selections(
 *                                 field("id", "3"),
 *                                 field("title", "Doctor Zhivago"),
 *                                 field(
 *                                         "publisher",
 *                                         selection(
 *                                                 field("id", "2")
 *                                         )
 *                                 )
 *                         ),
 *                         selections(
 *                                 field("id", "1"),
 *                                 field("title", "Libro Uno"),
 *                                 field(
 *                                         "publisher",
 *                                         selection(
 *                                                 field("id", "1")
 *                                         )
 *                                 )
 *                         ),
 *                         selections(
 *                                 field("id", "2"),
 *                                 field("title", "Libro Dos"),
 *                                 field(
 *                                         "publisher",
 *                                         selection(
 *                                                 field("id", "1")
 *                                         )
 *                                 )
 *                         )
 *                 )
 *         )
 * ).toResponse();
 * }
 * </pre>
 * produces the following response:
 * <pre>
 * {@code
 * {
 *     "data":{
 *         "book":{
 *             "edges":[
 *                 {
 *                     "node":{
 *                         "id":"3",
 *                         "title":"Doctor Zhivago",
 *                         "publisher":{
 *                             "edges":[
 *                                 {
 *                                     "node":{
 *                                         "id":"2"
 *                                     }
 *                                 }
 *                             ]
 *                         }
 *                     }
 *                 },
 *                 {
 *                     "node":{
 *                         "id":"1",
 *                         "title":"Libro Uno",
 *                         "publisher":{
 *                             "edges":[
 *                                 {
 *                                     "node":{
 *                                         "id":"1"
 *                                     }
 *                                 }
 *                             ]
 *                         }
 *                     }
 *                 },
 *                 {
 *                     "node":{
 *                         "id":"2",
 *                         "title":"Libro Dos",
 *                         "publisher":{
 *                             "edges":[
 *                                 {
 *                                     "node":{
 *                                         "id":"1"
 *                                     }
 *                                 }
 *                             ]
 *                         }
 *                     }
 *                 }
 *             ]
 *         }
 *     }
 * }
 * }
 * </pre>
 * Note that {@link GraphQLDSL} follows Relay's connection pattern(https://graphql.org/learn/pagination/).
 */
public final class GraphQLDSL {

    public static final boolean QUOTE_VALUE = true;
    public static final boolean UNQUOTED_VALUE = false;

    /**
     * Jackson-serializes objects.
     */
    private static final ObjectMapper BASE_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    /**
     * Returns the JSON representation of an object.
     *
     * @param object  Object to be serialized
     *
     * @return a string
     *
     * @throws IllegalStateException
     */
    public static String toJson(Object object) {
        try {
            return BASE_MAPPER.writer().writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    /**
     * Creates a GraphQL query.
     *
     * @param definitions  An variable-lengthed array of {@link Definition}s that composes a complete GraphQL query.
     *
     * @return a serializable GraphQL query object
     */
    public static Document document(Definition... definitions) {
        return new Document(Arrays.asList(definitions));
    }

    /**
     * Creates a single root-entity selection spec.
     *
     * @param selection  The single selection spec object
     *
     * @return a definition of a multi-definitions GraphQL query
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Field Selection</a>
     */
    public static SelectionSet selection(Selection selection) {
        return new SelectionSet(new LinkedHashSet<>(Collections.singleton(selection)));
    }

    /**
     * Creates a multi-root-entities selections spec.
     *
     * @param selections  An variable-sized array of selection spec objects
     *
     * @return a definition of a multi-definitions GraphQL query
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Field Selection</a>
     */
    public static SelectionSet selections(Selection... selections) {
        return new SelectionSet(Arrays.stream(selections).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Constructs a mutation.
     *
     * @param name  Mutation name
     * @param variableDefinitions  Optional variables
     * @param selectionSet  Selected fields
     *
     * @return a mutation
     */
    public static Mutation mutation(String name, VariableDefinitions variableDefinitions, SelectionSet selectionSet) {
        return new Mutation(name, variableDefinitions, selectionSet);
    }

    /**
     * Constructs a mutation.
     *
     * @param selectionSet  Selected fields
     *
     * @return a mutation
     */
    public static Mutation mutation(SelectionSet selectionSet) {
        return new Mutation(null, null, selectionSet);
    }

    /**
     * Constructs a named query.
     *
     * @param name  Query name
     * @param variableDefinitions  Optional variables
     * @param selectionSet  Selected fields
     *
     * @return a named query
     */
    public static Query query(String name, VariableDefinitions variableDefinitions, SelectionSet selectionSet) {
        return new Query(name, variableDefinitions, selectionSet);
    }

    /**
     * Constructs a named query.
     *
     * @param selectionSet  Selected fields
     *
     * @return a named query
     */
    public static Query query(SelectionSet selectionSet) {
        return new Query(null, null, selectionSet);
    }

    /**
     * Creates a collection of GraphQL variable definitions.
     *
     * @param variableDefinitions  A variable-sized array of {@link VariableDefinition}s
     *
     * @return A collection of GraphQL variable definitions
     *
     * @see <a href="https://graphql.org/learn/queries/#variables">Variables</a>
     */
    public static VariableDefinitions variableDefinitions(VariableDefinition... variableDefinitions) {
        return new VariableDefinitions(Arrays.asList(variableDefinitions));
    }

    /**
     * Creates a single GraphQL variable definition without default value.
     *
     * @param variable  A string (without "$" sign) representing the name of the variable
     * @param type  A simplified String representation of the aforementioned type literal.
     *
     * @return A single GraphQL variable definition
     *
     * @see <a href="https://graphql.org/learn/queries/#variables">Variables</a>
     */
    public static VariableDefinition variableDefinition(String variable, String type) {
        return new VariableDefinition(variable, type, null);
    }

    /**
     * Constructs a response field with string value.
     *
     * @param name  Field name
     * @param value  Field value
     *
     * @return a field
     */
    public static Selection field(String name, String value) {
        return new Field(null, name, Arguments.emptyArgument(), Field.quoteValue(value));
    }

    /**
     * Constructs a response field with numeric value.
     *
     * @param name  Field name
     * @param value  Field value
     *
     * @return a field
     */
    public static Selection field(String name, Number value) {
        return new Field(null, name, Arguments.emptyArgument(), value);
    }

    /**
     * Constructs a response field with boolean value.
     *
     * @param name  Field name
     * @param value  Field value
     *
     * @return a field
     */
    public static Selection field(String name, Boolean value) {
        return new Field(null, name, Arguments.emptyArgument(), value);
    }

    /**
     * Constructs a scalar response field.
     *
     * @param name  Field name
     * @param value  Field value
     * @param quoted  Weather value is quoted
     *
     * @return a field
     */
    public static Selection field(String name, String value, boolean quoted) {
        return new Field(null, name, Arguments.emptyArgument(), quoted ? Field.quoteValue(value) : value);
    }

    /**
     * Creates a request selection without {@link Argument}s.
     *
     * @param name  The name of the selected entity/field that would appear in a GraphQL query
     * @param selectionSet  The fields of the entity that are selected
     *
     * @return a top-level selection
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object Types and Fields</a>
     */
    public static Selection field(String name, SelectionSet... selectionSet) {
        return field(null, name, selectionSet);
    }

    public static Selection field(String alias, String name, SelectionSet... selectionSet) {
        List<SelectionSet> ss = ImmutableList.copyOf(selectionSet);
        return new Field(alias, name, Arguments.emptyArgument(), relayWrap(ss));
    }

    /**
     * Creates a single entity(object field) selection.
     *
     * @param name  The name of the selected entity/field that would appear in a GraphQL query
     * @param arguments  The {@link Argument}s that would be applied to the selected entity
     * @param selectionSet  The fields of the entity that are selected
     *
     * @return a top-level selection
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object Types and Fields</a>
     */
    public static Selection field(String name, Arguments arguments, SelectionSet... selectionSet) {
        return new Field(null, name, arguments, relayWrap(Arrays.asList(selectionSet)));
    }

    public static Selection field(String alias, String name, Arguments arguments, SelectionSet... selectionSet) {
        return new Field(alias, name, arguments, relayWrap(Arrays.asList(selectionSet)));
    }

    /**
     * Creates an attribute(scalar field) selection.
     *
     * @param name  The name of the selected attribute that would appear in a GraphQL query.
     *
     * @return a field that represents an non-reltionship entity attribute
     *
     * @see <a href="https://graphql.org/learn/schema/#scalar-types">Scalar Field</a>
     */
    public static Selection field(String name) {
        return Field.scalarField(name);
    }

    /**
     * Creates an attribute field selection with arguments.
     * @param name The name of the field.
     * @param arguments The arguments.
     * @return a field that represents an non-reltionship entity attribute
     */
    public static Selection field(String name, Arguments arguments) {
        return new Field(null, name, arguments, null);
    }

    /**
     * Creates an attribute field selection with arguments and an alias.
     * @param name The name of the field.
     * @param alias The alias name of the field.
     * @param arguments The arguments.
     * @return a field that represents an non-reltionship entity attribute
     */
    public static Selection field(String name, String alias, Arguments arguments) {
        return new Field(alias, name, arguments, null);
    }

    /**
     * Creates a complete set of {@link Argument}s that is passed to a object field selection.
     *
     * @param arguments  A variable-sized group of {@link Argument}s passed to the object field selection
     *
     * @return a complete specification of selection arguments
     *
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     */
    public static Arguments arguments(Argument... arguments) {
        return new Arguments(Arrays.asList(arguments));
    }

    /**
     * Creates a {@link Argument single argument} that is passed to a object field selection.
     *
     * @param argument  The single {@link Argument} passed to the object field selection
     *
     * @return a complete specification of selection argument
     *
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     */
    public static Arguments argument(Argument argument) {
        return new Arguments(Collections.singletonList(argument));
    }

    /**
     * Creates a spec object of an GraphQL {@link Argument argument} whose value is not quoted.
     *
     * @param name  The name of the argument
     * @param value  An object that models argument value.
     *
     * @return a complete specification of selection arguments
     *
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     */
    public static Argument argument(String name, Object value) {
        return argument(name, value, false);
    }

    /**
     * Creates a spec object of an GraphQL {@link Argument argument}.
     *
     * @param name  The name of the argument
     * @param value  An object that models argument value.
     * @param quoted  Whether or not the serialized {@code value} should be double-quote
     *
     * @return a complete specification of selection arguments
     *
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     */
    public static Argument argument(String name, Object value, boolean quoted) {
        if (value instanceof String) {
            value = quoted ? Field.quoteValue(value.toString()) : value;
            return new Argument(name, value);
        }
        // this is an object which needs to be Jackson-serialized
        try {
            return new Argument(
                    name,
                    BASE_MAPPER
                            .configure(
                                    // GraphQL argument name is unquoted; hence quoted field is disabled.
                                    JsonGenerator.Feature.QUOTE_FIELD_NAMES,
                                    false
                            )
                            .writeValueAsString(value)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(String.format("Cannot serialize %s", value), exception);
        }
    }

    /**
     * Wraps a set of field selections inside a Relay connection pattern spec.
     * <p>
     * For example,
     * <pre>
     * {@code
     * book(ids: $bookId) {
     *     id
     *     title
     *     authors {
     *         name
     *     }
     * }
     * }
     * </pre>
     * becomes
     * <pre>
     * {@code
     * book(ids: $bookId) {
     *     edges {
     *         node {
     *             id
     *             title
     *             authors {
     *                 edges {
     *                     node {
     *                         name
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     * }
     * </pre>
     *
     * @param selectionSet  The field selections to be wrapped
     *
     * @return the same GraphQL spec in Relay's annotation
     *
     * @see <a href="https://graphql.org/learn/pagination/">Relay's connection pattern</a>
     */
    private static SelectionSet relayWrap(List<SelectionSet> selectionSet) {
        Edges edges = new Edges(
                selectionSet.stream()
                        .map(Node::new)
                        .collect(Collectors.toList())
        );

        return new SelectionSet(new LinkedHashSet<>(Collections.singleton(edges)));
    }
}
