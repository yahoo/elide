/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.model.ElideDBConfig;
import com.yahoo.elide.modelconfig.model.ElideNamespaceConfig;
import com.yahoo.elide.modelconfig.model.ElideSecurityConfig;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.parser.handlebars.HandlebarsHydrator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.hjson.JsonValue;
import org.hjson.ParseException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    /**
     * format config file path.
     * @param basePath : path to hjson config.
     * @return formatted file path.
     */
    public static String formatFilePath(String basePath) {
        if (StringUtils.isNotBlank(basePath) && !basePath.endsWith("/")) {
            basePath += "/";
        }
        return basePath;
    }

    /**
     * converts variables hjson string to map of variables.
     * @param config HJSON file content.
     * @param schemaValidator JSON schema validator.
     * @return Map of Variables
     * @throws IOException If an I/O error or a processing error occurs.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> stringToVariablesPojo(String fileName, String config,
                    DynamicConfigSchemaValidator schemaValidator) throws IOException {
        Map<String, Object> variables = new HashMap<>();
        String jsonConfig = hjsonToJson(config);
        try {
            if (schemaValidator.verifySchema(Config.MODELVARIABLE, jsonConfig, fileName)) {
                variables = getModelPojo(jsonConfig, Map.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating Variable config : " + e.getMessage());
            throw new IOException(e);
        }
        return variables;
    }

    /**
     * Generates ElideTableConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @param schemaValidator JSON schema validator.
     * @return ElideTableConfig Pojo
     * @throws IOException If an I/O error or a processing error occurs.
     */
    public static ElideTableConfig stringToElideTablePojo(String fileName, String content,
                    Map<String, Object> variables, DynamicConfigSchemaValidator schemaValidator) throws IOException {
        ElideTableConfig table = new ElideTableConfig();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.TABLE, jsonConfig, fileName)) {
                table = getModelPojo(jsonConfig, ElideTableConfig.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating Table config : " + e.getMessage());
            throw new IOException(e);
        }
        return table;
    }

    /**
     * Generates ElideDBConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @param schemaValidator JSON schema validator.
     * @return ElideDBConfig Pojo
     * @throws IOException If an I/O error or a processing error occurs.
     */
    public static ElideDBConfig stringToElideDBConfigPojo(String fileName, String content,
                    Map<String, Object> variables, DynamicConfigSchemaValidator schemaValidator) throws IOException {
        ElideDBConfig dbconfig = new ElideDBConfig();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.SQLDBConfig, jsonConfig, fileName)) {
                dbconfig = getModelPojo(jsonConfig, ElideDBConfig.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating DB config : " + e.getMessage());
            throw new IOException(e);
        }
        return dbconfig;
    }

    /**
     * Generates ElideNamespaceConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @param schemaValidator JSON schema validator.
     * @return ElideNamespaceConfig Pojo
     * @throws IOException If an I/O error or a processing error occurs.
     */
    public static ElideNamespaceConfig stringToElideNamespaceConfigPojo(String fileName, String content,
                    Map<String, Object> variables, DynamicConfigSchemaValidator schemaValidator) throws IOException {
        ElideNamespaceConfig namespaceconfig = new ElideNamespaceConfig();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.NAMESPACEConfig, jsonConfig, fileName)) {
                namespaceconfig = getModelPojo(jsonConfig, ElideNamespaceConfig.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating DB config : " + e.getMessage());
            throw new IOException(e);
        }
        return namespaceconfig;
    }

    /**
     * Generates ElideSecurityConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @param schemaValidator JSON schema validator.
     * @return ElideSecurityConfig Pojo
     * @throws IOException If an I/O error or a processing error occurs.
     */
    public static ElideSecurityConfig stringToElideSecurityPojo(String fileName, String content,
                    Map<String, Object> variables, DynamicConfigSchemaValidator schemaValidator) throws IOException {
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.SECURITY, jsonConfig, fileName)) {
                return getModelPojo(jsonConfig, ElideSecurityConfig.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating Security config : " + e.getMessage());
            throw new IOException(e);
        }
        return null;
    }

    /**
     * resolves variables in table and security config.
     * @param jsonConfig of table or security
     * @param variables map from config
     * @return json string with resolved variables
     * @throws IOException If an I/O error or a processing error occurs.
     */
    public static String resolveVariables(String jsonConfig, Map<String, Object> variables) throws IOException {
        HandlebarsHydrator hydrator = new HandlebarsHydrator();
        return hydrator.hydrateConfigTemplate(jsonConfig, variables);
    }

    private static String hjsonToJson(String hjson) {
        try {
            return JsonValue.readHjson(hjson).toString();
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid Hjson Syntax: " + e.getMessage());
        }
    }

    private static <T> T getModelPojo(String jsonConfig, final Class<T> configPojo) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        return objectMapper.readValue(jsonConfig, configPojo);
    }
}
