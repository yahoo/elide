/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import static com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers.isNullOrEmpty;
import static com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHelper.NEWLINE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.library.Library;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.msgsimple.bundle.MessageBundle;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Dynamic Model Schema validation.
 */
@Slf4j
public class DynamicConfigSchemaValidator {

    private JsonSchema tableSchema;
    private JsonSchema securitySchema;
    private JsonSchema variableSchema;
    private JsonSchema dbConfigSchema;

    public DynamicConfigSchemaValidator() {

        Library library = new DraftV4LibraryWithElideFormatAttr().getLibrary();

        MessageBundle bundle = new MessageBundleWithElideMessages().getMsgBundle();

        ValidationConfiguration cfg = ValidationConfiguration.newBuilder()
                        .setDefaultLibrary("http://my.site/myschema#", library)
                        .setValidationMessages(bundle)
                        .freeze();

        JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
                        .setValidationConfiguration(cfg)
                        .freeze();

        tableSchema = loadSchema(factory, Config.TABLE.getConfigSchema());
        securitySchema = loadSchema(factory, Config.SECURITY.getConfigSchema());
        variableSchema = loadSchema(factory, Config.MODELVARIABLE.getConfigSchema());
        dbConfigSchema = loadSchema(factory, Config.SQLDBConfig.getConfigSchema());
    }

    /**
     * Verify config against schema.
     * @param configType
     * @param jsonConfig
     * @param fileName
     * @return whether config is valid
     * @throws IOException
     * @throws ProcessingException
     */
    public boolean verifySchema(Config configType, String jsonConfig, String fileName)
                    throws IOException, ProcessingException {
        ProcessingReport results = null;
        boolean isSuccess = false;

        switch (configType) {
        case TABLE :
            results = this.tableSchema.validate(new ObjectMapper().readTree(jsonConfig), true);
            break;
        case SECURITY :
            results = this.securitySchema.validate(new ObjectMapper().readTree(jsonConfig), true);
            break;
        case MODELVARIABLE :
        case DBVARIABLE :
            results = this.variableSchema.validate(new ObjectMapper().readTree(jsonConfig), true);
            break;
        case SQLDBConfig :
            results = this.dbConfigSchema.validate(new ObjectMapper().readTree(jsonConfig), true);
            break;
        default :
            log.error("Not a valid config type :" + configType);
            break;
        }
        isSuccess = (results == null ? false : results.isSuccess());

        if (!isSuccess) {
            throw new IllegalStateException("Schema validation failed for: " + fileName + getErrorMessages(results));
        }
        return isSuccess;
    }

    private static String getErrorMessages(ProcessingReport report) {
        List<String> list = new ArrayList<String>();
        report.forEach(msg -> addEmbeddedMessages(msg.asJson(), list, 0));

        return NEWLINE + String.join(NEWLINE, list);
    }

    private static void addEmbeddedMessages(JsonNode root, List<String> list, int depth) {

        if (root.has("level") && root.has("message")) {
            String level = root.get("level").asText();

            if (level.equalsIgnoreCase(LogLevel.ERROR.name()) || level.equalsIgnoreCase(LogLevel.FATAL.name())) {
                String msg = root.get("message").asText();
                String instancePointer = extractPointer(root, "instance");
                String schemaPointer = extractPointer(root, "schema");

                if (!(isNullOrEmpty(instancePointer) || isNullOrEmpty(schemaPointer))) {
                    msg = "Instance[" + instancePointer + "] failed to validate against schema[" + schemaPointer + "]. "
                                    + msg;
                }
                list.add((depth == 0) ? "[ERROR]" + NEWLINE + msg
                                : String.format("%" + (4 * depth + msg.length()) + "s", msg));

                if (root.has("reports")) {
                    Iterator<Entry<String, JsonNode>> fields = root.get("reports").fields();
                    while (fields.hasNext()) {
                        ArrayNode arrayNode = (ArrayNode) fields.next().getValue();
                        for (int i = 0; i < arrayNode.size(); i++) {
                            addEmbeddedMessages(arrayNode.get(i), list, depth + 1);
                        }
                    }
                }
            }
        }
    }

    private static String extractPointer(JsonNode root, String fieldName) {
        String pointer = null;
        if (root.has(fieldName)) {
            JsonNode node = root.get(fieldName);
            if (node.has("pointer")) {
                pointer = node.get("pointer").asText();
            }
        }

        return pointer;
    }

    private static JsonSchema loadSchema(JsonSchemaFactory factory, String resource) {

        try {
            return factory.getJsonSchema(
                            new ObjectMapper().readTree(DynamicConfigHelpers.class.getResourceAsStream(resource)));
        } catch (IOException | ProcessingException e) {
            log.error("Error loading schema file " + resource + " to verify");
            throw new IllegalStateException(e.getMessage());
        }
    }
}
