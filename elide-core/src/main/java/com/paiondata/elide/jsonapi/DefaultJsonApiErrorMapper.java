/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi;

import com.paiondata.elide.ElideError;
import com.paiondata.elide.jsonapi.models.JsonApiError;
import com.paiondata.elide.jsonapi.models.JsonApiError.JsonApiErrorBuilder;
import com.paiondata.elide.jsonapi.models.JsonApiError.Links;
import com.paiondata.elide.jsonapi.models.JsonApiError.Links.LinksBuilder;
import com.paiondata.elide.jsonapi.models.JsonApiError.Source;
import com.paiondata.elide.jsonapi.models.JsonApiError.Source.SourceBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Default {@link JsonApiErrorMapper}.
 */
public class DefaultJsonApiErrorMapper implements JsonApiErrorMapper {

    @Override
    public JsonApiError toJsonApiError(ElideError error) {
        JsonApiErrorBuilder jsonApiError = JsonApiError.builder();
        if (error.getMessage() != null) {
            // This will be encoded by the JsonApiErrorSerializer
            jsonApiError.detail(error.getMessage());
        }
        if (error.getAttributes() != null && !error.getAttributes().isEmpty()) {
            Map<String, Object> meta = new LinkedHashMap<>(error.getAttributes());
            attribute("id", meta, value -> {
                jsonApiError.id(value.toString());
                return true;
            });
            attribute("status", meta, value -> {
                jsonApiError.status(value.toString());
                return true;
            });
            attribute("code", meta, value -> {
                jsonApiError.code(value.toString());
                return true;
            });
            attribute("title", meta, value -> {
                jsonApiError.title(value.toString());
                return true;
            });
            attribute("source", meta, value -> {
                if (value instanceof Source source) {
                    jsonApiError.source(source);
                } else if (value instanceof Map map) {
                    jsonApiError.source(toSource(map));
                }
                return true;
            });
            attribute("links", meta, value -> {
                if (value instanceof Links links) {
                    jsonApiError.links(links);
                } else if (value instanceof Map map) {
                    jsonApiError.links(toLinks(map));
                }
                return true;
            });
            if (!meta.isEmpty()) {
                jsonApiError.meta(meta);
            }
        }
        return jsonApiError.build();
    }

    protected Links toLinks(Map<?, ?> map) {
        LinksBuilder builder = Links.builder();
        get("about", map).ifPresent(builder::about);
        get("type", map).ifPresent(builder::type);
        return builder.build();
    }

    protected Source toSource(Map<?, ?> map) {
        SourceBuilder builder = Source.builder();
        get("pointer", map).ifPresent(builder::pointer);
        get("parameter", map).ifPresent(builder::parameter);
        get("header", map).ifPresent(builder::header);
        return builder.build();
    }

    protected Optional<String> get(String key, Map<?, ?> map) {
        if (map.get(key) instanceof String value) {
            return Optional.of(value);
        }
        return Optional.empty();
    }

    protected void attribute(String key, Map<String, Object> map, Predicate<Object> processor) {
        if (map.containsKey(key) && processor.test(map.get(key))) {
            map.remove(key);
        }
    }
}
