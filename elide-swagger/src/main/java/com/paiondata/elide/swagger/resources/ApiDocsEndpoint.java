/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.swagger.resources;

import static com.paiondata.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.paiondata.elide.Elide;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.request.route.BasicApiVersionValidator;
import com.paiondata.elide.core.request.route.FlexibleRouteResolver;
import com.paiondata.elide.core.request.route.NullRouteResolver;
import com.paiondata.elide.core.request.route.Route;
import com.paiondata.elide.core.request.route.RouteResolver;
import com.paiondata.elide.swagger.OpenApiDocument;
import com.paiondata.elide.swagger.OpenApiDocument.MediaType;

import org.apache.commons.lang3.tuple.Pair;

import io.swagger.v3.oas.models.OpenAPI;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A convenience endpoint to expose a openapi document.
 */

@Path("/")
public class ApiDocsEndpoint {
    //Maps api version & path to a openapi document.
    protected Map<Pair<String, String>, OpenApiDocument> documents;
    protected final RouteResolver routeResolver;

    @Data
    @AllArgsConstructor
    public static class ApiDocsRegistration {
        private String path;
        private Supplier<OpenAPI> document;

        /**
         * The OpenAPI Specification Version.
         */
        private String version;

        /**
         * The API version.
         */
        private String apiVersion;
    }

    /**
     * Constructs the resource.
     *
     * @param docs Map of path parameter name to openapi document.
     */
    @Inject
    public ApiDocsEndpoint(@Named("apiDocs") List<ApiDocsRegistration> docs,
            @Named("elide") Elide elide, Optional<RouteResolver> optionalRouteResolver
            ) {
        documents = new HashMap<>();

        docs.forEach(doc -> {
            String apiVersion = doc.getApiVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;
            documents.put(Pair.of(apiVersion, apiPath),
                    new OpenApiDocument(doc.document, OpenApiDocument.Version.from(doc.version)));
        });

        this.routeResolver = optionalRouteResolver.orElseGet(() -> {
            Set<String> apiVersions = elide.getElideSettings().getEntityDictionary().getApiVersions();
            if (apiVersions.size() == 1 && apiVersions.contains(EntityDictionary.NO_VERSION)) {
                return new NullRouteResolver();
            } else {
                return new FlexibleRouteResolver(new BasicApiVersionValidator(), elide.getElideSettings()::getBaseUrl);
            }
        });
    }

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJson(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers
            ) {
        Route route = routeResolver.resolve(MediaType.APPLICATION_JSON, "", path, headers.getRequestHeaders(),
                uriInfo.getQueryParameters());
        String name = route.getPath();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            return list(route.getApiVersion(), MediaType.APPLICATION_JSON);
        } else {
            return get(route.getApiVersion(), name, MediaType.APPLICATION_JSON);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJson(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers
            ) {
        return listJson("", uriInfo, headers);
    }

    @GET
    @Path("{path:.*}")
    @Produces(MediaType.APPLICATION_YAML)
    public Response listYaml(
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers
    ) {
        Route route = routeResolver.resolve(MediaType.APPLICATION_YAML, "", path, headers.getRequestHeaders(),
                uriInfo.getQueryParameters());
        String name = route.getPath();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            return list(route.getApiVersion(), MediaType.APPLICATION_YAML);
        } else {
            return get(route.getApiVersion(), name, MediaType.APPLICATION_YAML);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_YAML)
    public Response listYaml(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers
            ) {
        return listYaml("", uriInfo, headers);
    }

    public Response list(String apiVersion, String mediaType) {
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;

        final List<String> documentPaths = documents.keySet().stream()
                .filter(key -> key.getLeft().equals(safeApiVersion)).map(Pair::getRight).toList();

        if (documentPaths.size() == 1) {
            Optional<Pair<String, String>> pair = documents.keySet().stream()
                    .filter(key -> key.getLeft().equals(safeApiVersion)).findFirst();
            if (pair.isPresent()) {
                return Response.ok(documents.get(pair.get()).ofMediaType(mediaType)).build();
            }
        }

        String body = documentPaths.stream().map(key -> '"' + key + '"')
                .collect(Collectors.joining(",", "[", "]"));

        return Response.ok(body).build();
    }

    public Response get(String apiVersion, String name, String mediaType) {
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        Pair<String, String> lookupKey = Pair.of(safeApiVersion, name);
        if (documents.containsKey(lookupKey)) {
            return Response.ok(documents.get(lookupKey).ofMediaType(mediaType)).build();
        }
        return Response.status(404).entity("Unknown document: " + name).build();
    }
}
