/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.graphql;

import com.yahoo.elide.Settings;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.graphql.FilterDialect;

import lombok.Getter;

import java.util.function.Consumer;

/**
 * Settings for GraphQL.
 *
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class GraphQLSettings implements Settings {
    @Getter
    public static class Federation {
        private final boolean enabled;

        public Federation(boolean enabled) {
            this.enabled = enabled;
        }

        public static FederationBuilder builder() {
            return new FederationBuilder();
        }

        public static class FederationBuilder {
            private boolean enabled = false;

            public FederationBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Federation build() {
                return new Federation(this.enabled);
            }
        }
    }

    private final boolean enabled;
    private final String path;
    private final Federation federation;
    private final FilterDialect filterDialect;

    public GraphQLSettings(boolean enabled, String path, Federation federation, FilterDialect filterDialect) {
        this.enabled = enabled;
        this.path = path;
        this.federation = federation;
        this.filterDialect = filterDialect;
    }

    public GraphQLSettingsBuilder mutate() {
        GraphQLSettingsBuilder builder = new GraphQLSettingsBuilder();
        builder.enabled = this.enabled;
        builder.path = this.path;
        builder.federation.enabled(this.federation.enabled);
        builder.filterDialect = this.filterDialect;
        return builder;
    }

    public static GraphQLSettingsBuilder builder() {
        return new GraphQLSettingsBuilder();
    }

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
            return new GraphQLSettings(this.enabled, this.path, this.federation.build(), this.filterDialect);
        }

        @Override
        protected GraphQLSettingsBuilder self() {
            return this;
        }

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

        protected abstract S self();

        public S enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        public S path(String path) {
            this.path = path;
            return self();
        }

        public S federation(Consumer<Federation.FederationBuilder> federation) {
            federation.accept(this.federation);
            return self();
        }

        public S filterDialect(FilterDialect filterDialect) {
            this.filterDialect = filterDialect;
            return self();
        }
    }
}
