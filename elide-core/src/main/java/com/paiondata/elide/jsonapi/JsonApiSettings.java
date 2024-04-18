/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.Settings;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.BasicExceptionMappers;
import com.paiondata.elide.core.exceptions.HttpStatus;
import com.paiondata.elide.core.exceptions.Slf4jExceptionLogger;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.DefaultFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.JoinFilterDialect;
import com.paiondata.elide.core.filter.dialect.jsonapi.SubqueryFilterDialect;
import com.paiondata.elide.jsonapi.links.JsonApiLinks;

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
    private final JsonApiExceptionHandler jsonApiExceptionHandler;

    public JsonApiSettings(boolean enabled, String path, JsonApiMapper jsonApiMapper, Links links, int updateStatusCode,
            boolean strictQueryParameters, List<JoinFilterDialect> joinFilterDialects,
            List<SubqueryFilterDialect> subqueryFilterDialects,
            JsonApiExceptionHandler jsonApiExceptionHandler) {
        this.enabled = enabled;
        this.path = path;
        this.jsonApiMapper = jsonApiMapper;
        this.links = links;
        this.updateStatusCode = updateStatusCode;
        this.strictQueryParameters = strictQueryParameters;
        this.joinFilterDialects = joinFilterDialects;
        this.subqueryFilterDialects = subqueryFilterDialects;
        this.jsonApiExceptionHandler = jsonApiExceptionHandler;
    }

    /**
     * Returns a builder with the current values.
     *
     * @return the builder to mutate
     */
    public JsonApiSettingsBuilder mutate() {
        JsonApiSettingsBuilder builder = new JsonApiSettingsBuilder()
                .enabled(this.enabled)
                .path(this.path)
                .jsonApiMapper(this.jsonApiMapper)
                .links(newLinks -> newLinks.enabled(this.getLinks().isEnabled())
                        .jsonApiLinks(this.getLinks().getJsonApiLinks()))
                .strictQueryParameters(this.isStrictQueryParameters())
                .jsonApiExceptionHandler(this.jsonApiExceptionHandler);

        builder.updateStatusCode = this.updateStatusCode;
        builder.joinFilterDialects.addAll(this.joinFilterDialects);
        builder.subqueryFilterDialects.addAll(this.subqueryFilterDialects);

        return builder;
    }

    /**
     * Returns a mutable {@link JsonApiSettingsBuilder} for building {@link JsonApiSettings}.
     *
     * @return the builder
     */
    public static JsonApiSettingsBuilder builder() {
        return new JsonApiSettingsBuilder();
    }

    /**
     * A mutable builder for building {@link JsonApiSettings}.
     */
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
                    this.subqueryFilterDialects, this.jsonApiExceptionHandler);
        }

        @Override
        protected JsonApiSettingsBuilder self() {
            return this;
        }

        /**
         * Returns a mutable {@link JsonApiSettingsBuilder} for building
         * {@link JsonApiSettings} with default filter dialects if not set.
         *
         * @return the builder
         */
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
        protected JsonApiExceptionHandler jsonApiExceptionHandler = new DefaultJsonApiExceptionHandler(
                new Slf4jExceptionLogger(), BasicExceptionMappers.builder().build(), new DefaultJsonApiErrorMapper());

        protected abstract S self();

        /**
         * Enable JSON API.
         *
         * @param enabled true for enabled
         * @return the builder
         */
        public S enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        /**
         * Sets the path of the JSON API endpoint.
         *
         * @param path the JSON API endpoint path.
         * @return the builder
         */
        public S path(String path) {
            this.path = path;
            return self();
        }

        /**
         * Sets the {@link JsonApiMapper}.
         *
         * @param jsonApiMapper the mapper
         * @return the builder
         */
        public S jsonApiMapper(JsonApiMapper jsonApiMapper) {
            this.jsonApiMapper = jsonApiMapper;
            return self();
        }

        /**
         * Customize the JSON API Links.
         *
         * @param links the customizer
         * @return the builder
         */
        public S links(Consumer<Links.LinksBuilder> links) {
            links.accept(this.links);
            return self();
        }

        /**
         * Enable strict query parameters.
         *
         * @param strictQueryParameters true for strict
         * @return the builder
         */
        public S strictQueryParameters(boolean strictQueryParameters) {
            this.strictQueryParameters = strictQueryParameters;
            return self();
        }

        /**
         * Return 200 OK on update.
         *
         * @return the builder
         */
        public S updateStatus200() {
            this.updateStatusCode = HttpStatus.SC_OK;
            return self();
        }

        /**
         * Return 204 No Content on update.
         *
         * @return the builder
         */
        public S updateStatus204() {
            this.updateStatusCode = HttpStatus.SC_NO_CONTENT;
            return self();
        }

        /**
         * Sets the {@link JoinFilterDialect}.
         *
         * @param joinFilterDialects the dialects to set
         * @return the builder
         */
        public S joinFilterDialects(List<JoinFilterDialect> joinFilterDialects) {
            this.joinFilterDialects = joinFilterDialects;
            return self();
        }

        /**
         * Add the {@link JoinFilterDialect}.
         *
         * @param joinFilterDialect the dialect to add
         * @return the builder
         */
        public S joinFilterDialect(JoinFilterDialect joinFilterDialect) {
            this.joinFilterDialects.add(joinFilterDialect);
            return self();
        }

        /**
         * Customize the {@link JoinFilterDialect}.
         *
         * @param joinFilterDialects the customizer
         * @return the builder
         */
        public S joinFilterDialects(Consumer<List<JoinFilterDialect>> joinFilterDialects) {
            joinFilterDialects.accept(this.joinFilterDialects);
            return self();
        }

        /**
         * Sets the {@link SubqueryFilterDialect}.
         *
         * @param subqueryFilterDialects the dialects to set
         * @return the builder
         */
        public S subqueryFilterDialects(List<SubqueryFilterDialect> subqueryFilterDialects) {
            this.subqueryFilterDialects = subqueryFilterDialects;
            return self();
        }

        /**
         * Add the {@link SubqueryFilterDialect}.
         *
         * @param subqueryFilterDialect the dialect to add
         * @return the builder
         */
        public S subqueryFilterDialect(SubqueryFilterDialect subqueryFilterDialect) {
            this.subqueryFilterDialects.add(subqueryFilterDialect);
            return self();
        }

        /**
         * Customize the {@link SubqueryFilterDialect}.
         *
         * @param subqueryFilterDialects the customizer
         * @return the builder
         */
        public S subqueryFilterDialects(Consumer<List<SubqueryFilterDialect>> subqueryFilterDialects) {
            subqueryFilterDialects.accept(this.subqueryFilterDialects);
            return self();
        }

        /**
         * Sets the {@link JsonApiExceptionHandler}.
         *
         * @param jsonApiExceptionHandler the exception handler
         * @return
         */
        public S jsonApiExceptionHandler(JsonApiExceptionHandler jsonApiExceptionHandler) {
            this.jsonApiExceptionHandler = jsonApiExceptionHandler;
            return self();
        }
    }
}
