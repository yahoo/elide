/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a JSON API Error Object.
 */
@Builder
@Getter
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"id", "links", "status", "code", "source", "title", "detail", "meta"})
public class JsonApiError {
    @Builder
    @Getter
    @JsonInclude(Include.NON_NULL)
    @JsonPropertyOrder({"about", "type"})
    public static class Links {
        private final String about;
        private final String type;

        @JsonCreator
        public Links(@JsonProperty("about") String about, @JsonProperty("type") String type) {
            this.about = about;
            this.type = type;
        }
    }

    @Builder
    @Getter
    @JsonInclude(Include.NON_NULL)
    @JsonPropertyOrder({"pointer", "parameter", "header"})
    public static class Source {
        private final String pointer;
        private final String parameter;
        private final String header;

        @JsonCreator
        public Source(@JsonProperty("pointer") String pointer, @JsonProperty("parameter") String parameter,
                @JsonProperty("header") String header) {
            this.pointer = pointer;
            this.parameter = parameter;
            this.header = header;
        }
    }

    private final String id;
    private final Links links;
    private final String status;
    private final String code;
    private final Source source;
    private final String title;
    private final String detail;
    private final Object meta;

    @JsonCreator
    public JsonApiError(@JsonProperty("id") String id, @JsonProperty("links") Links links,
            @JsonProperty("status") String status, @JsonProperty("code") String code,
            @JsonProperty("source") Source source, @JsonProperty("title") String title,
            @JsonProperty("detail") String detail, @JsonProperty("meta") Object meta) {
        this.id = id;
        this.links = links;
        this.status = status;
        this.code = code;
        this.source = source;
        this.title = title;
        this.detail = detail;
        this.meta = meta;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMeta() {
        return (T) this.meta;
    }

    /**
     * A mutable builder for building {@link JsonApiError}.
     */
    public static class JsonApiErrorBuilder {
        /**
         * Sets the {@link Links}.
         *
         * @param links the links
         * @return the builder
         */
        public JsonApiErrorBuilder links(Links links) {
            this.links = links;
            return this;
        }

        /**
         * Customize the {@link Links}.
         *
         * @param links the customizer
         * @return the builder
         */
        public JsonApiErrorBuilder links(Consumer<Links.LinksBuilder> links) {
            Links.LinksBuilder builder = new Links.LinksBuilder();
            links.accept(builder);
            this.links = builder.build();
            return this;
        }

        /**
         * Sets the {@link Source}.
         *
         * @param source the source
         * @return the builder
         */
        public JsonApiErrorBuilder source(Source source) {
            this.source = source;
            return this;
        }

        /**
         * Customize the {@link Source}.
         *
         * @param source the customizer
         * @return the builder
         */
        public JsonApiErrorBuilder source(Consumer<Source.SourceBuilder> source) {
            Source.SourceBuilder builder = new Source.SourceBuilder();
            source.accept(builder);
            this.source = builder.build();
            return this;
        }

        /**
         * Customize the meta.
         *
         * @param meta the customizer
         * @return the builder
         */
        public JsonApiErrorBuilder meta(Consumer<Map<String, Object>> meta) {
            Map<String, Object> builder = new LinkedHashMap<>();
            meta.accept(builder);
            return meta(builder);
        }

        /**
         * Sets the meta.
         *
         * @param meta the meta
         * @return the builder
         */
        public <M> JsonApiErrorBuilder meta(M meta) {
            this.meta = meta;
            return this;
        }
    }
}
