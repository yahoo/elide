/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql;

import com.paiondata.elide.Settings;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BasicExceptionMappers;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.graphql.FilterDialect;
import com.paiondata.elide.graphql.federation.FederationVersion;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Settings for GraphQL.
 * <p>
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class GraphQLSettings implements Settings {
    @Getter
    public static class Federation {
        private final boolean enabled;
        private final FederationVersion version;

        public Federation(boolean enabled, FederationVersion version) {
            this.enabled = enabled;
            this.version = version;
        }

        public static FederationBuilder builder() {
            return new FederationBuilder();
        }

        public static class FederationBuilder {
            private boolean enabled = false;
            private FederationVersion version = FederationVersion.FEDERATION_1_0;

            public FederationBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public FederationBuilder version(FederationVersion version) {
                this.version = version;
                return this;
            }

            public FederationBuilder version(String version) {
                return version(FederationVersion.from(version));
            }

            public Federation build() {
                return new Federation(this.enabled, this.version);
            }
        }
    }

    private final boolean enabled;
    private final String path;
    private final Federation federation;
    private final FilterDialect filterDialect;
    private final GraphQLExceptionHandler graphqlExceptionHandler;
    private final GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer;

    public GraphQLSettings(boolean enabled, String path, Federation federation, FilterDialect filterDialect,
            GraphQLExceptionHandler graphqlExceptionHandler,
            GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer) {
        this.enabled = enabled;
        this.path = path;
        this.federation = federation;
        this.filterDialect = filterDialect;
        this.graphqlExceptionHandler = graphqlExceptionHandler;
        this.graphqlFieldDefinitionCustomizer = graphqlFieldDefinitionCustomizer;
    }

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public GraphQLSettingsBuilder mutate() {
        return new GraphQLSettingsBuilder()
                .enabled(this.enabled)
                .path(this.path)
                .filterDialect(this.filterDialect)
                .federation(newFederation -> newFederation.enabled(this.getFederation().isEnabled())
                        .version(this.getFederation().getVersion()))
                .graphqlExceptionHandler(this.graphqlExceptionHandler);
    }

    /**
     * Returns a mutable {@link GraphQLSettingsBuilder} for building {@link GraphQLSettings}.
     *
     * @return the builder
     */
    public static GraphQLSettingsBuilder builder() {
        return new GraphQLSettingsBuilder();
    }

    /**
     * A mutable builder for building {@link GraphQLSettings}.
     */
    public static class GraphQLSettingsBuilder extends GraphQLSettingsBuilderSupport<GraphQLSettingsBuilder> {
        private Consumer<GraphQLSettingsBuilder> processor = null;

        private GraphQLSettingsBuilder processor(Consumer<GraphQLSettingsBuilder> processor) {
            this.processor = processor;
            return self();
        }

        @Override
        public GraphQLSettings build() {
            if (this.processor != null) {
                this.processor.accept(this);
            }
            return new GraphQLSettings(this.enabled, this.path, this.federation.build(), this.filterDialect,
                    this.graphqlExceptionHandler, this.graphqlFieldDefinitionCustomizer);
        }

        @Override
        protected GraphQLSettingsBuilder self() {
            return this;
        }

        /**
         * Returns a mutable {@link GraphQLSettingsBuilder} for building
         * {@link GraphQLSettings} with default filter dialect if not set.
         *
         * @return the builder
         */
        public static GraphQLSettingsBuilder withDefaults(EntityDictionary entityDictionary) {
            return new GraphQLSettingsBuilder().processor(builder -> {
                if (builder.filterDialect == null) {
                    builder.filterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build());
                }
            });
        }
    }

    public abstract static class GraphQLSettingsBuilderSupport<S> implements SettingsBuilder {
        protected boolean enabled = false;
        protected String path = "/";
        protected final Federation.FederationBuilder federation = Federation.builder();
        protected FilterDialect filterDialect;
        protected GraphQLExceptionHandler graphqlExceptionHandler = new DefaultGraphQLExceptionHandler(
                new Slf4jExceptionLogger(), BasicExceptionMappers.builder().build(), new DefaultGraphQLErrorMapper());
        protected GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer =
                DefaultGraphQLFieldDefinitionCustomizer.INSTANCE;

        protected abstract S self();

        /**
         * Enable GraphQL.
         *
         * @param enabled true for enabled
         * @return the builder
         */
        public S enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        /**
         * Sets the path of the GraphQL endpoint.
         *
         * @param path the GraphQL endpoint path.
         * @return the builder
         */
        public S path(String path) {
            this.path = path;
            return self();
        }

        /**
         * Customize the Federation settings.
         *
         * @param federation the customizer
         * @return the builder
         */
        public S federation(Consumer<Federation.FederationBuilder> federation) {
            federation.accept(this.federation);
            return self();
        }

        /**
         * Sets the {@link FilterDialect}.
         *
         * @param filterDialect the filter dialect
         * @return the builder
         */
        public S filterDialect(FilterDialect filterDialect) {
            this.filterDialect = filterDialect;
            return self();
        }

        /**
         * Sets the {@link GraphQLExceptionHandler}.
         *
         * @param graphqlExceptionHandler the exception handler
         * @return the builder
         */
        public S graphqlExceptionHandler(GraphQLExceptionHandler graphqlExceptionHandler) {
            this.graphqlExceptionHandler = graphqlExceptionHandler;
            return self();
        }

        /**
         * Sets the {@link GraphQLFieldDefinitionCustomizer}.
         *
         * @param graphqlFieldDefinitionCustomizer the customizer
         * @return the builder
         */
        public S graphqlFieldDefinitionCustomizer(GraphQLFieldDefinitionCustomizer graphqlFieldDefinitionCustomizer) {
            this.graphqlFieldDefinitionCustomizer = graphqlFieldDefinitionCustomizer;
            return self();
        }
    }
}
