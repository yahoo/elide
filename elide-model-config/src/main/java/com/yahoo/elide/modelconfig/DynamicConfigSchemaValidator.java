/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.Dialect;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Dynamic Model Schema validation.
 */
@Slf4j
public class DynamicConfigSchemaValidator {

    private final Schema tableSchema;
    private final Schema securitySchema;
    private final Schema variableSchema;
    private final Schema dbConfigSchema;
    private final Schema namespaceConfigSchema;

    public DynamicConfigSchemaValidator() {
        Dialect jsonMetaSchema = ElideDialect.getInstance();
        SchemaRegistryConfig schemaRegistryConfig = SchemaRegistryConfig.builder().formatAssertionsEnabled(true)
                .errorMessageKeyword("errorMessage").build();
        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(jsonMetaSchema,
                builder -> builder.schemaRegistryConfig(schemaRegistryConfig));

        tableSchema = loadSchema(schemaRegistry, Config.TABLE.getConfigSchema());
        securitySchema = loadSchema(schemaRegistry, Config.SECURITY.getConfigSchema());
        variableSchema = loadSchema(schemaRegistry, Config.MODELVARIABLE.getConfigSchema());
        dbConfigSchema = loadSchema(schemaRegistry, Config.SQLDBConfig.getConfigSchema());
        namespaceConfigSchema = loadSchema(schemaRegistry, Config.NAMESPACEConfig.getConfigSchema());
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
        List<Error> results = null;
        switch (configType) {
        case TABLE :
            results = this.tableSchema.validate(jsonConfig, InputFormat.JSON);
            break;
        case SECURITY :
            results = this.securitySchema.validate(jsonConfig, InputFormat.JSON);
            break;
        case MODELVARIABLE :
        case DBVARIABLE :
            results = this.variableSchema.validate(jsonConfig, InputFormat.JSON);
            break;
        case SQLDBConfig :
            results = this.dbConfigSchema.validate(jsonConfig, InputFormat.JSON);
            break;
        case NAMESPACEConfig :
            results = this.namespaceConfigSchema.validate(jsonConfig, InputFormat.JSON);
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

    private static Schema loadSchema(SchemaRegistry factory, String resource) {
        try (InputStream is = DynamicConfigHelpers.class.getResourceAsStream(resource)) {
            return factory.getSchema(is);
        } catch (IOException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }
}
