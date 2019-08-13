/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.testhelpers.graphql;

import com.yahoo.elide.contrib.testhelpers.graphql.elements.Argument;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Definition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Document;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.EnumValue;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.ObjectValueWithVariable;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.OperationDefinition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.StringValue;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.TypedOperation;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.ValueWithVariable;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.VariableDefinition;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.VariableDefinitions;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.VariableValue;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Arguments;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Edges;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Node;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.ObjectField;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.ScalarField;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.Selection;
import com.yahoo.elide.contrib.testhelpers.graphql.elements.SelectionSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
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

    /**
     * Serializes expected JSON object to a string.
     */
    static private final Gson GSON_INSTANCE = new GsonBuilder()
            .serializeNulls().create();

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

    /**
     * Creates a typed query definition without query name and variable definitions.
     *
     * @param operationType  The type of the query definition.
     * @param selectionSet  The definition/selection-spec of a multi-definitions GraphQL query for
     *
     * @return a definition of a multi-definitions GraphQL query
     *
     * @see <a href="https://graphql.org/learn/schema/#the-query-and-mutation-types">Typed Query</a>
     */
    public static TypedOperation typedOperation(
            TypedOperation.OperationType operationType,
            SelectionSet selectionSet
    ) {
        return new TypedOperation(operationType, null, null, selectionSet);
    }

    /**
     * Creates a typed query definition
     *
     * @param operationType  The type of the query definition.
     * @param name  An unique identifier of a {@link OperationDefinition operation} in a multi-operations
     * {@link Document GraphQL query}
     * @param variableDefinitions  A collection of GraphQL variable definitions
     * @param selectionSet  The definition/selection-spec of a multi-definitions GraphQL query for
     *
     * @return a definition of a multi-definitions GraphQL query
     *
     * @see <a href="https://graphql.org/learn/schema/#the-query-and-mutation-types">Typed Query</a>
     * @see <a href="https://graphql.org/learn/queries/#variables">Variables</a>
     */
    public static TypedOperation typedOperation(
            TypedOperation.OperationType operationType,
            String name,
            VariableDefinitions variableDefinitions,
            SelectionSet selectionSet
    ) {
        return new TypedOperation(operationType, name, variableDefinitions, selectionSet);
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
     * Creates a single top-level entity(object field) selection without {@link Argument}s.
     *
     * @param name  The name of the selected entity/field that would appear in a GraphQL query
     * @param selectionSet  The fields of the entity that are selected
     *
     * @return a top-level selection
     *
     * @see <a href="https://graphql.org/learn/queries/#fields">Fields</a>
     * @see <a href="https://graphql.org/learn/schema/#object-types-and-fields">Object Types and Fields</a>
     */
    public static Selection entity(String name, SelectionSet selectionSet) {
        return ObjectField.withoutArguments(name, relayWrap(selectionSet));
    }

    /**
     * Creates a top-level single entity(object field) selection.
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
    public static Selection entity(String name, Arguments arguments, SelectionSet selectionSet) {
        return new ObjectField(name, arguments, relayWrap(selectionSet));
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
    public static Selection field(String name, SelectionSet selectionSet) {
        return entity(name, selectionSet);
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
    public static Selection field(String name, Arguments arguments, SelectionSet selectionSet) {
        return entity(name, arguments, selectionSet);
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
        return new ScalarField(name);
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
     * Creates a spec object of an GraphQL {@link Argument argument}.
     *
     * @param name  The name of the argument
     * @param value  An object that models argument value.
     * @return
     *
     * @see <a href="https://graphql.org/learn/queries/#arguments">Arguments</a>
     */
    public static Argument argument(String name, ValueWithVariable value) {
        return new Argument(name, value);
    }

    /**
     * Creates a string value that is quoted in its serialized form
     *
     * @param value  The chars surrounded in a pair of double-quotes
     *
     * @return a GraphQL spec
     */
    public static ValueWithVariable stringValue(String value) {
        return new StringValue(value);
    }

    /**
     * Creates a query spec that represents a variable, whose variable name follows a {@code $} sign.
     * <p>
     * Foe example, {@code $episode}. In this case "episode" is the variable name
     *
     * @param name  A GraphQL variable literal without {@code $} sign
     *
     * @return a GraphQL variable spec
     *
     * @see <a href="https://graphql.org/learn/queries/#variables">Variables</a>
     */
    public static ValueWithVariable variableValue(String name) {
        return new VariableValue(name);
    }

    /**
     * Creates a query spec that represents a object value.
     * <p>
     * For example, "id:\"1\", title:\"update title\"" as in "{ id:\"1\", title:\"update title\" }".
     *
     * @param object  A string representation of the object wrapped
     *
     * @return a GraphQL spec
     */
    public static ValueWithVariable objectValueWithVariable(Object object) {
        return new ObjectValueWithVariable(object);
    }

    /**
     * Creates a string value that is NOT quoted in its serialized form
     *
     * @param enumValue  The same string in the serialized form of the created object.
     *
     * @return a GraphQL spec
     */
    public static ValueWithVariable enumValue(String enumValue) {
        return new EnumValue(enumValue);
    }

    /**
     * Creates an response attribute(scalar field).
     *
     * @param name  The name of the selected attribute that would appear in a GraphQL query.
     * @param value  The attribute value
     *
     * @return a field that represents an non-reltionship entity response attribute
     */
    public static Selection responseField(String name, String value) {
        return com.yahoo.elide.contrib.testhelpers.graphql.elements.response.ScalarField.withQuotedValue(name, value);
    }

    /**
     * Creates an response attribute(scalar field) whose value is {@code null}(unquoted).
     *
     * @param name  The name of the selected attribute that would appear in a GraphQL query.
     *
     * @return a field that represents an non-reltionship entity response attribute whose value is
     * {@code null}(unquoted)
     */
    public static Selection nullResponseField(String name) {
        return com.yahoo.elide.contrib.testhelpers.graphql.elements.response.ScalarField.withUnquotedValue(name, null);
    }

    /**
     * Creates an response attribute(scalar field) whose value is a GSON-serializable object, such as map.
     *
     * @param name  The name of the selected attribute that would appear in a GraphQL query.
     *
     * @return a field that represents an non-reltionship entity response attribute whose value a GSON-serializable
     * object, such as map.
     */
    public static Selection jsonResponseField(String name, Object jsonOjbect) {
        return com.yahoo.elide.contrib.testhelpers.graphql.elements.response.ScalarField.withUnquotedValue(
                name,
                GSON_INSTANCE.toJson(jsonOjbect)
        );
    }

    /**
     * Creates a entity(object field) response attribute.
     *
     * @param name  The name of the selected entity/field that would appear in a GraphQL query
     * @param nodes  All instances of the entity in the response data
     *
     * @return an entity selection instantiation
     */
    public static Selection responseField(String name, SelectionSet... nodes) {
        return new com.yahoo.elide.contrib.testhelpers.graphql.elements.response.ObjectField(
                name,
                Arrays.asList(nodes)
        );
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
    private static SelectionSet relayWrap(SelectionSet selectionSet) {
        Node node = new Node(selectionSet);
        Edges edges = new Edges(node);

        return new SelectionSet(new LinkedHashSet<>(Collections.singleton(edges)));
    }
}
