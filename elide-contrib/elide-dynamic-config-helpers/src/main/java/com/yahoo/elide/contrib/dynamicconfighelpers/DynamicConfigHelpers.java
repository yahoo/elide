/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars.HandlebarsHydrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import org.apache.commons.io.FileUtils;
import org.hjson.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    public static final String TABLE_CONFIG_PATH = "tables" + File.separator;
    public static final String SECURITY_CONFIG_PATH = "security.hjson";
    public static final String VARIABLE_CONFIG_PATH = "variables.hjson";
    public static final String NEW_LINE = "\n";
    public static final String TABLE = "table";
    public static final String SECURITY = "security";
    public static final String VARIABLE = "variable";

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
     * converts variable.hjson to map of variables.
     * @param basePath : root path to model dir
     * @return Map of variables
     * @throws JsonProcessingException
     * @throws ProcessingException
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getVariablesPojo(String basePath)
            throws JsonProcessingException, ProcessingException {
        Map<String, Object> variables = null;
        String filePath = basePath + VARIABLE_CONFIG_PATH;
        File variableFile = new File(filePath);
        if (variableFile.exists()) {
            String jsonConfig = hjsonToJson(readConfigFile(variableFile));
            if (DynamicConfigSchemaValidator.verifySchema(VARIABLE, jsonConfig)) {
                variables = getModelPojo(jsonConfig, Map.class);
            }
        } else {
            log.info("Variables config file not found at " + filePath);
        }
        return (variables == null ? new HashMap<String, Object>() : variables);
    }

    /**
     * converts all available table config to ElideTableConfig Pojo.
     * @param basePath : root path to model dir
     * @param variables : variables to resolve.
     * @return ElideTableConfig pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig getElideTablePojo(String basePath, Map<String, Object> variables)
            throws IOException, ProcessingException {
        return getElideTablePojo(basePath, variables, TABLE_CONFIG_PATH);
    }

    /**
     * converts all available table config to ElideTableConfig Pojo.
     * @param basePath : root path to model dir
     * @param variables : variables to resolve.
     * @param tableDirName : dir name for table configs
     * @return ElideTableConfig pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig getElideTablePojo(String basePath, Map<String, Object> variables,
            String tableDirName) throws IOException, ProcessingException {
        Collection<File> tableConfigs = FileUtils.listFiles(new File(basePath + tableDirName),
                new String[] {"hjson"}, false);
        Set<Table> tables = new HashSet<>();
        for (File tableConfig : tableConfigs) {
            ElideTableConfig table = stringToElideTablePojo(readConfigFile(tableConfig), variables);
            tables.addAll(table.getTables());
        }
        ElideTableConfig elideTableConfig = new ElideTableConfig();
        elideTableConfig.setTables(tables);
        return elideTableConfig;
    }

    /**
     * Generates ElideTableConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @return ElideTableConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideTableConfig stringToElideTablePojo(String content, Map<String, Object> variables)
            throws IOException, ProcessingException {
        ElideTableConfig table = null;
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        if (DynamicConfigSchemaValidator.verifySchema(TABLE, jsonConfig)) {
            table = getModelPojo(jsonConfig, ElideTableConfig.class);
        }
        return table;
    }

    /**
     * converts security.hjson to ElideSecurityConfig Pojo.
     * @param basePath : root path to model dir.
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideSecurityConfig getElideSecurityPojo(String basePath, Map<String, Object> variables)
            throws IOException, ProcessingException {
        ElideSecurityConfig security = null;
        String filePath = basePath + SECURITY_CONFIG_PATH;
        File securityFile = new File(filePath);
        if (securityFile.exists()) {
            security = stringToElideSecurityPojo(readConfigFile(securityFile), variables);
        } else {
            log.info("Security config file not found at " + filePath);
        }
        return security;
    }

    /**
     * Generates ElideSecurityConfig Pojo from input String.
     * @param content : input string
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     * @throws ProcessingException
     */
    public static ElideSecurityConfig stringToElideSecurityPojo(String content, Map<String, Object> variables)
            throws IOException, ProcessingException {
        String jsonConfig = hjsonToJson(resolveVariables(content, variables));
        if (DynamicConfigSchemaValidator.verifySchema(SECURITY, jsonConfig)) {
            return getModelPojo(jsonConfig, ElideSecurityConfig.class);
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

    private static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    private static <T> T getModelPojo(String jsonConfig, final Class<T> configPojo) throws JsonProcessingException {
        return new ObjectMapper().readValue(jsonConfig, configPojo);
    }
}
