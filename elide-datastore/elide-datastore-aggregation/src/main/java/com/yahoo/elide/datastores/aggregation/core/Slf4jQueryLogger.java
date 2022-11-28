/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.core;

import com.yahoo.elide.core.security.User;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Default Slf4j Logging Implementation of QueryLogger for Elide.
 */
@Slf4j
public class Slf4jQueryLogger implements QueryLogger {

    private static final String ID = "id";

    private final ObjectMapper mapper;
    private final Logger logger;

    @FunctionalInterface
    public interface Logger {
        void log(String template, JsonNode value);
    }

    public Slf4jQueryLogger() {
        mapper = new ObjectMapper();
        this.logger = this::log;
    }


    public Slf4jQueryLogger(Logger logger) {
        mapper = new ObjectMapper();
        this.logger = logger;
    }

    @Override
    public void acceptQuery(UUID queryId, User user, Map<String, String> headers, String apiVer,
                            MultivaluedMap<String, String> queryParams, String path) {
        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put(ID, queryId.toString());
        rootNode.put("user", (user != null && user.getName() != null) ? user.getName() : "Unknown");
        if (!queryParams.isEmpty()) {
            ObjectNode queryParamNode = rootNode.putObject("queryParams");
            queryParams.forEach((key, values) -> {
                ArrayNode listNode = queryParamNode.putArray(key);
                values.stream().forEach(listNode::add);
            });
        }

        rootNode.put("apiVersion", apiVer);
        rootNode.put("path", path);
        ObjectNode headerNode = rootNode.putObject("headers");
        headers.forEach(headerNode::put);

        logger.log("QUERY ACCEPTED: {}", rootNode);
    }

    @Override
    public void processQuery(UUID queryId, Query query, List<String> apiQuery, boolean isCached) {
        ObjectNode rootNode = mapper.createObjectNode();

        rootNode.put(ID, queryId.toString());
        ArrayNode dbQueryNode = rootNode.putArray("queries");
        apiQuery.stream().forEach(dbQueryNode::add);
        rootNode.put("isCached", isCached);
        logger.log("QUERY RUNNING: {}", rootNode);
    }

    @Override
    public void cancelQuery(UUID queryId) {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(ID, queryId.toString());
        logger.log("QUERY CANCELED: {}", rootNode);
    }

    @Override
    public void completeQuery(UUID queryId, QueryResponse response) {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put(ID, queryId.toString());
        rootNode.put("status", response.getResponseCode());
        rootNode.put("error", response.getErrorMessage());
        logger.log("QUERY COMPLETE: {}", rootNode);
    }

    protected void log(String template, JsonNode value) {
        log.debug(template, value);
    }
}
