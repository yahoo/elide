/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * Dynamic Model Schema validation.
 */
@Slf4j
public class DynamicConfigSchemaValidator {

    private final JsonSchema tableSchema;
    private final JsonSchema securitySchema;
    private final JsonSchema variableSchema;
    private final JsonSchema dbConfigSchema;
    private final JsonSchema namespaceConfigSchema;
    private final ObjectMapper objectMapper;

    public DynamicConfigSchemaValidator() {
        this(new ObjectMapper());
    }

    public DynamicConfigSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        JsonMetaSchema jsonMetaSchema = ElideMetaSchema.getInstance();
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri(jsonMetaSchema.getIri())
                .metaSchema(jsonMetaSchema)
                .build();

        tableSchema = loadSchema(factory, objectMapper, Config.TABLE.getConfigSchema());
        securitySchema = loadSchema(factory, objectMapper, Config.SECURITY.getConfigSchema());
        variableSchema = loadSchema(factory, objectMapper, Config.MODELVARIABLE.getConfigSchema());
        dbConfigSchema = loadSchema(factory, objectMapper, Config.SQLDBConfig.getConfigSchema());
        namespaceConfigSchema = loadSchema(factory, objectMapper, Config.NAMESPACEConfig.getConfigSchema());
    }

    /**
     * Verify config against schema.
     * @param configType {@link Config} type.
     * @param jsonConfig HJSON file content as JSON string.
     * @param fileName Name of HJSON file.
     * @return whether config is valid
     * @throws IOException If an I/O error occurs or processing error occurred during validation.
     */
    public boolean verifySchema(Config configType, String jsonConfig, String fileName)
                    throws IOException {
        Set<ValidationMessage> results = null;
        switch (configType) {
        case TABLE :
            results = this.tableSchema.validate(objectMapper.readTree(jsonConfig));
            break;
        case SECURITY :
            results = this.securitySchema.validate(objectMapper.readTree(jsonConfig));
            break;
        case MODELVARIABLE :
        case DBVARIABLE :
            results = this.variableSchema.validate(objectMapper.readTree(jsonConfig));
            break;
        case SQLDBConfig :
            results = this.dbConfigSchema.validate(objectMapper.readTree(jsonConfig));
            break;
        case NAMESPACEConfig :
            results = this.namespaceConfigSchema.validate(objectMapper.readTree(jsonConfig));
            break;
        default :
            log.error("Not a valid config type :" + configType);
            break;
        }
        if (results != null && !results.isEmpty()) {
            throw new InvalidSchemaException(fileName, results);
        }
        return true;
    }

    private static JsonSchema loadSchema(JsonSchemaFactory factory, ObjectMapper objectMapper, String resource) {
        try (InputStream is = DynamicConfigHelpers.class.getResourceAsStream(resource)) {
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                    .formatAssertionsEnabled(true)
                    .errorMessageKeyword("errorMessage")
                    .build();
            return factory.getSchema(objectMapper.readTree(is), config);
        } catch (IOException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
