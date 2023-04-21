/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.resources;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.swagger.OpenApiBuilder;

import org.apache.commons.lang3.tuple.Pair;

import io.swagger.v3.oas.models.OpenAPI;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A convenience endpoint to expose a openapi document.
 */

@Path("/doc")
@Produces("application/json")
public class ApiDocsEndpoint {
    //Maps api version & path to a openapi document.
    protected Map<Pair<String, String>, String> documents;

    @Data
    @AllArgsConstructor
    public static class ApiDocsRegistration {
        private String path;
        private OpenAPI document;
    }

    /**
     * Constructs the resource.
     *
     * @param docs Map of path parameter name to openapi document.
     */
    @Inject
    public ApiDocsEndpoint(@Named("apiDocs") List<ApiDocsRegistration> docs) {
        documents = new HashMap<>();

        docs.forEach(doc -> {
            String apiVersion = doc.document.getInfo().getVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;

            documents.put(Pair.of(apiVersion, apiPath), OpenApiBuilder.getDocument(doc.document));
        });
    }

    @GET
    @Path("/")
    public Response list(@HeaderParam("ApiVersion") String apiVersion) {
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;

        String body = documents.keySet().stream()
                .filter(key -> key.getLeft().equals(safeApiVersion))
                .map(Pair::getRight)
                .map(key -> '"' + key + '"')
                .collect(Collectors.joining(",", "[", "]"));

        return Response.ok(body).build();
    }

    /**
     * Read handler.
     *
     * @param name document name
     * @return response The Swagger JSON document
     */
    @GET
    @Path("/{name}")
    public Response get(@HeaderParam("ApiVersion") String apiVersion, @PathParam("name") String name) {
        String safeApiVersion = apiVersion == null ? NO_VERSION : apiVersion;
        Pair<String, String> lookupKey = Pair.of(safeApiVersion, name);
        if (documents.containsKey(lookupKey)) {
            return Response.ok(documents.get(lookupKey)).build();
        }
        return Response.status(404).entity("Unknown document: " + name).build();
    }
}
