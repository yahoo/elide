/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideDBConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import org.hjson.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    /**
     * Checks whether input is null or empty.
     * @param input : input string
     * @return true or false
     */
    public static boolean isNullOrEmpty(String input) {
        return (input == null || input.trim().length() == 0);
    }

    /**
     * format config file path.
     * @param basePath : path to hjson config.
     * @return formatted file path.
     */
    public static String formatFilePath(String basePath) {
        if (isNullOrEmpty(basePath) || basePath.endsWith(File.separator)) {
            return basePath;
        } else {
            return basePath += File.separator;
        }
    }

    /**
     * converts variables hjson string to map of variables.
     * @param config
     * @return Map of Variables
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> stringToVariablesPojo(String config)
            throws IOException {
        DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
        Map<String, Object> variables = new HashMap<>();
        String jsonConfig = hjsonToJson(config);
        try {
            if (schemaValidator.verifySchema(Config.MODELVARIABLE, jsonConfig)) {
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
     * @return ElideTableConfig Pojo
     * @throws IOException
     */
    public static ElideTableConfig stringToElideTablePojo(String content, Map<String, Object> variables)
            throws IOException {
        DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
        ElideTableConfig table = new ElideTableConfig();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.TABLE, jsonConfig)) {
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
     * @return ElideDBConfig Pojo
     * @throws IOException
     */
    public static ElideDBConfig stringToElideDBConfigPojo(String content, Map<String, Object> variables)
            throws IOException {
        DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
        ElideDBConfig dbconfig = new ElideDBConfig();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.SQLDBConfig, jsonConfig)) {
                dbconfig = getModelPojo(jsonConfig, ElideDBConfig.class);
            }
        } catch (ProcessingException e) {
            log.error("Error Validating DB config : " + e.getMessage());
            throw new IOException(e);
        }
        return dbconfig;
    }

    /**
     * Generates ElideSecurityConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     */
    public static ElideSecurityConfig stringToElideSecurityPojo(String content, Map<String, Object> variables)
            throws IOException {
        DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        try {
            if (schemaValidator.verifySchema(Config.SECURITY, jsonConfig)) {
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
     * @throws IOException
     */
    public static String resolveVariables(String jsonConfig, Map<String, Object> variables) throws IOException {
        HandlebarsHydrator hydrator = new HandlebarsHydrator();
        return hydrator.hydrateConfigTemplate(jsonConfig, variables);
    }

    private static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    private static <T> T getModelPojo(String jsonConfig, final Class<T> configPojo) throws JsonProcessingException {
        return new ObjectMapper().readValue(jsonConfig, configPojo);
    }
}
