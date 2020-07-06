/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

@Slf4j
public class DynamicConfigSchemaValidator {

    private static final String TABLE_SCHEMA = "/elideTableSchema.json";
    private static final String SECURITY_SCHEMA = "/elideSecuritySchema.json";
    private static final String VARIABLE_SCHEMA = "/elideVariableSchema.json";

    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.byDefault();
    private static JsonSchema tableSchema;
    private static JsonSchema securitySchema;
    private static JsonSchema variableSchema;

    static {
        tableSchema = loadSchema(TABLE_SCHEMA);
        securitySchema = loadSchema(SECURITY_SCHEMA);
        variableSchema = loadSchema(VARIABLE_SCHEMA);
    }

    public static boolean verifySchema(String configType, String jsonConfig)
            throws JsonMappingException, JsonProcessingException, ProcessingException {
        ProcessingReport results = null;

        switch (configType) {
        case DynamicConfigHelpers.TABLE :
            results = tableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case DynamicConfigHelpers.SECURITY :
            results = securitySchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case DynamicConfigHelpers.VARIABLE :
            results = variableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        default :
            return false;
        }
        return results.isSuccess();
    }

    private static JsonSchema loadSchema(String resource) {
        ObjectMapper objectMapper = new ObjectMapper();
        Reader reader = new InputStreamReader(DynamicConfigHelpers.class.getResourceAsStream(resource));
        try {
            return FACTORY.getJsonSchema(objectMapper.readTree(reader));
        } catch (IOException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new IllegalStateException(e.getMessage());
        } catch (ProcessingException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new IllegalStateException(e.getMessage());
        }
    }
}
