/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.Argument;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Definition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Document;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Field;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Mutation;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Query;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.VariableDefinition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.VariableDefinitions;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Arguments;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Edges;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Node;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Selection;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.SelectionSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link GraphQLDSL} programmatically construct GraphQL query(excluding query variables) and serializes it to a GraphQL
 * query string.
 * <p>
 * For example,
 * <pre>
 * {@code
 * String graphQLQuery = document(
 *         typedOperation(
 *                 TypedOperation.OperationType.QUERY,
 *                 "myQuery",
 *                 variableDefinitions(
 *                         variableDefinition("bookId", "[String]")
 *                 ),
 *                 selections(
 *                         entity(
 *                                 "book",
 *                                 arguments(
 *                                         argument("ids", variableValue("bookId"))
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
 *         )
 * ).toQuery();
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
 *                 responseField(
 *                         "book",
 *                         selections(
 *                                 responseField("id", "3"),
 *                                 responseField("title", "Doctor Zhivago"),
 *                                 responseField(
 *                                         "publisher",
 *                                         selection(
 *                                                 responseField("id", "2")
 *                                         )
 *                                 )
 *                         ),
 *                         selections(
 *                                 responseField("id", "1"),
 *                                 responseField("title", "Libro Uno"),
 *                                 responseField(
 *                                         "publisher",
 *                                         selection(
 *                                                 responseField("id", "1")
 *                                         )
 *                                 )
 *                         ),
 *                         selections(
 *                                 responseField("id", "2"),
 *                                 responseField("title", "Libro Dos"),
 *                                 responseField(
 *                                         "publisher",
 *                                         selection(
 *                                                 responseField("id", "1")
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
     * Serializes expected JSON object to a string.
     */
    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .serializeNulls().create();

    /**
     * Jackson-serializes entities.
     * <p>
     * GraphQL argument name is unquoted; hence quoted field is disabled.
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .configure(
                    JsonGenerator.Feature.QUOTE_FIELD_NAMES,
                    false
            )
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    /**
     * Constructor.
     * <p>
     * Suppress default constructor for noninstantiability.
     */
    private GraphQLDSL() {
        throw new AssertionError();
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

    public static Mutation mutation(String name, VariableDefinitions variableDefinitions,SelectionSet selectionSet) {
        return new Mutation(name, variableDefinitions, selectionSet);
    }

    public static Mutation mutation(SelectionSet selectionSet) {
        return new Mutation(null, null, selectionSet);
    }

    public static Query query(String name, VariableDefinitions variableDefinitions,SelectionSet selectionSet) {
        return new Query(name, variableDefinitions, selectionSet);
    }

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

    public static Selection field(String name, String value) {
        return new Field(name, Arguments.emptyArgument(), Field.quoteValue(value), false);
    }

    public static Selection field(String name, String value, boolean quoted) {
        return new Field(name, Arguments.emptyArgument(), quoted ? Field.quoteValue(value) : value, false);
    }

    public static Selection field(String name, Object value) {
        return new Field(name, Arguments.emptyArgument(), GSON_INSTANCE.toJson(value), false);
    }

    /**
     * Creates a entity(object field) selection without {@link Argument}s.
     *
     * @param name  The name of the selected entity/field that would appear in a GraphQL query
     * @param selectionSet  The fields of the entity that are selected
     *
     * @return a top-level selection
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object Types and Fields</a>
     */
    public static Selection field(boolean isQuery, String name, SelectionSet... selectionSet) {
        return new Field(name, Arguments.emptyArgument(), relayWrap(Arrays.asList(selectionSet), isQuery), isQuery);
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
    public static Selection field(boolean isQuery, String name, Arguments arguments, SelectionSet... selectionSet) {
        return new Field(name, arguments, relayWrap(Arrays.asList(selectionSet), isQuery), isQuery);
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
            value = quoted ? String.format("\"%s\"", value) : value;
            return new Argument(name, value);
        } else {
            // this is an object which needs to be Jackson-serialized
            try {
                return new Argument(name, JSON_MAPPER.writeValueAsString(value));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException(String.format("Cannot serialize %s", value), exception);
            }
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
    private static SelectionSet relayWrap(List<SelectionSet> selectionSet, boolean isQuery) {
        Edges edges = new Edges(
                selectionSet.stream()
                        .map(set -> new Node(set, isQuery))
                        .collect(Collectors.toList()),
                isQuery);

        return new SelectionSet(new LinkedHashSet<>(Collections.singleton(edges)));
    }
}
