/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi;

import com.yahoo.elide.Settings;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.exceptions.HttpStatus;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.yahoo.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.yahoo.elide.jsonapi.links.JsonApiLinks;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Settings for JsonApi.
 *
 * Use the static factory {@link #builder()} method to prepare an instance.
 */
@Getter
public class JsonApiSettings implements Settings {
    @Getter
    public static class Links {
        private final boolean enabled;
        private final JsonApiLinks jsonApiLinks;

        public Links(boolean enabled, JsonApiLinks jsonApiLinks) {
            this.enabled = enabled;
            this.jsonApiLinks = jsonApiLinks;
        }

        public static LinksBuilder builder() {
            return new LinksBuilder();
        }

        public static class LinksBuilder {
            private boolean enabled = false;
            private JsonApiLinks jsonApiLinks;

            public LinksBuilder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public LinksBuilder jsonApiLinks(JsonApiLinks jsonApiLinks) {
                this.jsonApiLinks = jsonApiLinks;
                return this;
            }

            public Links build() {
                return new Links(this.enabled, this.jsonApiLinks);
            }
        }
    }

    private final boolean enabled;
    private final String path;
    private final JsonApiMapper jsonApiMapper;
    private final Links links;
    private final int updateStatusCode;
    private final boolean strictQueryParameters;
    private final List<JoinFilterDialect> joinFilterDialects;
    private final List<SubqueryFilterDialect> subqueryFilterDialects;

    public JsonApiSettings(boolean enabled, String path, JsonApiMapper jsonApiMapper, Links links, int updateStatusCode,
            boolean strictQueryParameters, List<JoinFilterDialect> joinFilterDialects,
            List<SubqueryFilterDialect> subqueryFilterDialects) {
        this.enabled = enabled;
        this.path = path;
        this.jsonApiMapper = jsonApiMapper;
        this.links = links;
        this.updateStatusCode = updateStatusCode;
        this.strictQueryParameters = strictQueryParameters;
        this.joinFilterDialects = joinFilterDialects;
        this.subqueryFilterDialects = subqueryFilterDialects;
    }

    public JsonApiSettingsBuilder mutate() {
        JsonApiSettingsBuilder builder = new JsonApiSettingsBuilder();
        builder.enabled = this.enabled;
        builder.path = this.path;
        builder.jsonApiMapper = this.jsonApiMapper;
        builder.links.enabled(this.links.enabled).jsonApiLinks(this.links.jsonApiLinks);
        builder.updateStatusCode = this.updateStatusCode;
        builder.strictQueryParameters = this.strictQueryParameters;
        builder.joinFilterDialects.addAll(this.joinFilterDialects);
        builder.subqueryFilterDialects.addAll(this.subqueryFilterDialects);
        return builder;
    }

    public static JsonApiSettingsBuilder builder() {
        return new JsonApiSettingsBuilder();
    }

    public static class JsonApiSettingsBuilder extends JsonApiSettingsBuilderSupport<JsonApiSettingsBuilder> {
        private Consumer<JsonApiSettingsBuilder> processor = null;

        private JsonApiSettingsBuilder processor(Consumer<JsonApiSettingsBuilder> processor) {
            this.processor = processor;
            return self();
        }

        @Override
        public JsonApiSettings build() {
            if (this.processor != null) {
                this.processor.accept(this);
            }
            return new JsonApiSettings(this.enabled, this.path, this.jsonApiMapper, this.links.build(),
                    this.updateStatusCode, this.strictQueryParameters, this.joinFilterDialects,
                    this.subqueryFilterDialects);
        }

        @Override
        protected JsonApiSettingsBuilder self() {
            return this;
        }

        public static JsonApiSettingsBuilder withDefaults(EntityDictionary entityDictionary) {
            return new JsonApiSettingsBuilder().processor(builder -> {
                if (builder.joinFilterDialects.isEmpty()) {
                    builder.joinFilterDialect(new DefaultFilterDialect(entityDictionary))
                            .joinFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build());
                }
                if (builder.subqueryFilterDialects.isEmpty()) {
                    builder
                    .subqueryFilterDialect(new DefaultFilterDialect(entityDictionary))
                    .subqueryFilterDialect(RSQLFilterDialect.builder().dictionary(entityDictionary).build());
                }
            });
        }
    }

    public abstract static class JsonApiSettingsBuilderSupport<S> implements SettingsBuilder {
        protected boolean enabled = false;
        protected String path = "/";
        protected JsonApiMapper jsonApiMapper = new JsonApiMapper();
        protected Links.LinksBuilder links = Links.builder();
        protected int updateStatusCode = HttpStatus.SC_NO_CONTENT;
        protected boolean strictQueryParameters = true;
        protected List<JoinFilterDialect> joinFilterDialects = new ArrayList<>();
        protected List<SubqueryFilterDialect> subqueryFilterDialects = new ArrayList<>();

        protected abstract S self();

        public S enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        public S path(String path) {
            this.path = path;
            return self();
        }

        public S jsonApiMapper(JsonApiMapper jsonApiMapper) {
            this.jsonApiMapper = jsonApiMapper;
            return self();
        }

        public S links(Consumer<Links.LinksBuilder> links) {
            links.accept(this.links);
            return self();
        }

        public S strictQueryParameters(boolean strictQueryParameters) {
            this.strictQueryParameters = strictQueryParameters;
            return self();
        }

        public S updateStatus200() {
            this.updateStatusCode = HttpStatus.SC_OK;
            return self();
        }

        public S updateStatus204() {
            this.updateStatusCode = HttpStatus.SC_NO_CONTENT;
            return self();
        }

        public S joinFilterDialects(List<JoinFilterDialect> joinFilterDialects) {
            this.joinFilterDialects = joinFilterDialects;
            return self();
        }

        public S joinFilterDialect(JoinFilterDialect joinFilterDialect) {
            this.joinFilterDialects.add(joinFilterDialect);
            return self();
        }

        public S joinFilterDialects(Consumer<List<JoinFilterDialect>> joinFilterDialects) {
            joinFilterDialects.accept(this.joinFilterDialects);
            return self();
        }

        public S subqueryFilterDialects(List<SubqueryFilterDialect> subqueryFilterDialects) {
            this.subqueryFilterDialects = subqueryFilterDialects;
            return self();
        }

        public S subqueryFilterDialect(SubqueryFilterDialect subqueryFilterDialect) {
            this.subqueryFilterDialects.add(subqueryFilterDialect);
            return self();
        }

        public S subqueryFilterDialects(Consumer<List<SubqueryFilterDialect>> subqueryFilterDialects) {
            subqueryFilterDialects.accept(this.subqueryFilterDialects);
            return self();
        }
    }
}
