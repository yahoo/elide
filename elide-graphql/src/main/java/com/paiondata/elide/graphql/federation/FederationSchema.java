/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;


import com.apollographql.federation.graphqljava.Federation;
import com.apollographql.federation.graphqljava.FederationDirectives;

import graphql.language.SchemaExtensionDefinition;
import graphql.language.StringValue;
import graphql.schema.GraphQLAppliedDirective;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Federation Schema.
 */
public class FederationSchema {
    private final GraphQLSchema schema;
    private final FederationVersion version;

    public FederationSchema(GraphQLSchema schema, FederationVersion version) {
        this.schema = schema;
        this.version = version;
    }

    /**
     * Returns the {@link GraphQLSchema}.
     *
     * @return the schema
     */
    public GraphQLSchema getSchema() {
        return this.schema;
    }

    /**
     * Returns the federation specification version.
     *
     * @return the federation specification version
     */
    public FederationVersion getVersion() {
        return this.version;
    }

    /**
     * Returns the federation directive by name which may be namespaced.
     *
     * @param name the directive name
     * @return the directive
     */
    public GraphQLDirective getDirective(String name) {
        GraphQLDirective directive = this.schema.getDirective(name);
        if (directive == null) {
            directive = this.schema.getDirective(FederationDefinitions.namespace + name);
        }
        return directive;
    }

    /**
     * Gets the @key applied directive.
     *
     * @param fields the fields
     * @return the @key applied directive
     */
    public GraphQLAppliedDirective key(String fields) {
        GraphQLDirective key = getDirective(FederationDirectives.keyName);
        GraphQLArgument argument = key.getArgument(FederationDirectives.fieldsArgumentName);
        return key.toAppliedDirective().transform(directive -> directive
                .argument(argument.toAppliedArgument().transform(arg -> arg.valueLiteral(StringValue.of(fields)))));
    }

    /**
     * Gets the @shareable applied directive in Federation 2.
     *
     * @return the @shareable applied directive if present
     */
    public Optional<GraphQLAppliedDirective> shareable() {
        GraphQLDirective shareable = getDirective(FederationDefinitions.shareableName);
        if (shareable == null) {
            return Optional.empty();
        }
        return Optional.of(shareable.toAppliedDirective());
    }

    /**
     * Returns a mutable @{link FederationSchemaBuilder} for building a
     * {@link FederationSchema}.
     *
     * @return a mutable builder
     */
    public static FederationSchemaBuilder builder() {
        return new FederationSchemaBuilder();
    }

    /**
     * The mutable builder for {@link FederationSchema}.
     */
    public static class FederationSchemaBuilder {
        private FederationVersion version = FederationVersion.FEDERATION_1_0;
        private List<String> imports = new ArrayList<>();

        /**
         * Sets the federation specification version.
         *
         * @param version the version
         * @return the builder
         */
        public FederationSchemaBuilder version(FederationVersion version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the imports for instance @key or @shareable.
         *
         * @param imports the imports
         * @return the builder
         */
        public FederationSchemaBuilder imports(String... imports) {
            this.imports.addAll(List.of(imports));
            return this;
        }

        /**
         * Builds the {@link FederationSchema}.
         *
         * @return the federation schema
         */
        public FederationSchema build() {
            GraphQLSchema schema = getSchema(version, imports.toArray(String[]::new));
            return new FederationSchema(schema, this.version);
        }
    }

    /**
     * Gets the federation schema given the version and imports.
     *
     * @param version the federation specification version
     * @param imports the imports eg. @key, @shareable
     * @return the federation schema
     */
    public static GraphQLSchema getSchema(FederationVersion version, String... imports) {
        String specification = toSpecification(version);
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        if (specification != null) {
            typeDefinitionRegistry.add(
                    SchemaExtensionDefinition.newSchemaExtensionDefinition()
                            .directive(FederationDefinitions.link(specification, imports)).build());
        }
        return Federation.transform(typeDefinitionRegistry).build();
    }

    /**
     * Determine the federation specification url from the version.
     *
     * @param version the version
     * @return the federation specification url
     */
    public static String toSpecification(FederationVersion version) {
        switch (version) {
        case FEDERATION_2_5:
            return Federation.FEDERATION_SPEC_V2_5;
        case FEDERATION_2_4:
            return Federation.FEDERATION_SPEC_V2_4;
        case FEDERATION_2_3:
            return Federation.FEDERATION_SPEC_V2_3;
        case FEDERATION_2_2:
            return Federation.FEDERATION_SPEC_V2_2;
        case FEDERATION_2_1:
            return Federation.FEDERATION_SPEC_V2_1;
        case FEDERATION_2_0:
            return Federation.FEDERATION_SPEC_V2_0;
        case FEDERATION_1_1:
            return null;
        case FEDERATION_1_0:
            return null;
        }
        throw new IllegalArgumentException("Unsupported Federation Version " + version);
    }
}
