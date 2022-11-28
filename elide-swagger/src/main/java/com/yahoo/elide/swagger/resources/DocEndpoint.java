/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.swagger.resources;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;

import com.yahoo.elide.swagger.SwaggerBuilder;
import org.apache.commons.lang3.tuple.Pair;

import io.swagger.models.Swagger;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * A convenience endpoint to expose a swagger document.
 */

@Path("/doc")
@Produces("application/json")
public class DocEndpoint {
    //Maps api version & path to a swagger document.
    protected Map<Pair<String, String>, String> documents;

    @Data
    @AllArgsConstructor
    public static class SwaggerRegistration {
        private String path;
        private Swagger document;
    }

    /**
     * Constructs the resource.
     *
     * @param docs Map of path parameter name to swagger document.
     */
    @Inject
    public DocEndpoint(@Named("swagger") List<SwaggerRegistration> docs) {
        documents = new HashMap<>();

        docs.forEach((doc) -> {
            String apiVersion = doc.document.getInfo().getVersion();
            apiVersion = apiVersion == null ? NO_VERSION : apiVersion;
            String apiPath = doc.path;

            documents.put(Pair.of(apiVersion, apiPath), SwaggerBuilder.getDocument(doc.document));
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
