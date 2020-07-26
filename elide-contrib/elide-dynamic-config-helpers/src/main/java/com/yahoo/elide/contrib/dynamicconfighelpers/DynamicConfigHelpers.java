/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import org.apache.commons.io.FileUtils;
import org.hjson.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    private static final String NEW_LINE = "\n";

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
        String path = basePath;
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
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
            if (schemaValidator.verifySchema(Config.VARIABLE, jsonConfig)) {
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

    /**
     * Read hjson config file.
     * @param configFile : hjson file to read
     * @return hjson file content
     */
    public static String readConfigFile(File configFile) {
        StringBuffer sb = new StringBuffer();
        try {
            for (String line : FileUtils.readLines(configFile, StandardCharsets.UTF_8)) {
                sb.append(line);
                sb.append(NEW_LINE);
            }
        } catch (IOException e) {
            log.error("error while reading config file " + configFile.getName());
            log.error(e.getMessage());
        }
        return sb.toString();
    }

    /**
     * Read config from classpath.
     * @param resourcePath : path to resource
     * @return content of resource
     * @throws IOException
     */
    public static String readResource(String resourcePath) throws IOException {
        InputStream stream = null;
        BufferedReader reader = null;
        String content = null;
        try {
            stream = DynamicConfigHelpers.class.getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            content =  reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
        return content;
    }

    private static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    private static <T> T getModelPojo(String jsonConfig, final Class<T> configPojo) throws JsonProcessingException {
        return new ObjectMapper().readValue(jsonConfig, configPojo);
    }
}
