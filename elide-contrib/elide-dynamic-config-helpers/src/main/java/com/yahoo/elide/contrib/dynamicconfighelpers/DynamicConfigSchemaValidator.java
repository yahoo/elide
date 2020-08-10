/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Dynamic Model Schema validation.
 */
@Slf4j
public class DynamicConfigSchemaValidator {

    private static final JsonSchemaFactory FACTORY = JsonSchemaFactory.byDefault();
    private JsonSchema tableSchema;
    private JsonSchema securitySchema;
    private JsonSchema variableSchema;
    private JsonSchema dbConfigSchema;

    public DynamicConfigSchemaValidator() {
        tableSchema = loadSchema(Config.TABLE.getConfigSchema());
        securitySchema = loadSchema(Config.SECURITY.getConfigSchema());
        variableSchema = loadSchema(Config.MODELVARIABLE.getConfigSchema());
        dbConfigSchema = loadSchema(Config.SQLDBConfig.getConfigSchema());
    }
    /**
     *  Verify config against schema.
     * @param configType
     * @param jsonConfig
     * @return whether config is valid
     * @throws IOException
     * @throws ProcessingException
     */
    public boolean verifySchema(Config configType, String jsonConfig) throws IOException, ProcessingException {
        ProcessingReport results = null;
        boolean isSuccess = false;

        switch (configType) {
        case TABLE :
            results = this.tableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case SECURITY :
            results = this.securitySchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case MODELVARIABLE :
        case DBVARIABLE :
            results = this.variableSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        case SQLDBConfig :
        case NONSQLDBConfig :
            results = this.dbConfigSchema.validate(new ObjectMapper().readTree(jsonConfig));
            break;
        default :
            log.error("Not a valid config type :" + configType);
            break;
        }
        isSuccess = (results == null ? false : results.isSuccess());

        if (!isSuccess) {
            throw new ProcessingException("Schema validation failed");
        }
        return isSuccess;
    }

    private JsonSchema loadSchema(String resource) {
        ObjectMapper objectMapper = new ObjectMapper();
        Reader reader = new InputStreamReader(DynamicConfigHelpers.class.getResourceAsStream(resource));
        try {
            return FACTORY.getJsonSchema(objectMapper.readTree(reader));
        } catch (IOException | ProcessingException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new IllegalStateException(e.getMessage());
        }
    }
}
