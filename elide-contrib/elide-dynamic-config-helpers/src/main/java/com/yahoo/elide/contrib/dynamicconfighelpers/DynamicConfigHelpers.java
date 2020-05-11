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

import org.apache.commons.io.FileUtils;
import org.hjson.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
/**
 * Util class for Dynamic config helper module.
 */
public class DynamicConfigHelpers {

    private static String TABLE_CONFIG_PATH = "tables/";
    private static final String SECURITY_CONFIG_PATH = "security.hjson";
    private static final String VARIABLE_CONFIG_PATH = "variables.hjson";
    private static final String NEW_LINE = "\n";

    public static void setTableConfigPath(String input) {
        DynamicConfigHelpers.TABLE_CONFIG_PATH = input;
    }

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
        if (!basePath.endsWith(File.separator)) {
            basePath += File.separator;
        }
        return basePath;
    }

    /**
     * converts variable.hjson to map of variables.
     * @param basePath : root path to model dir
     * @return Map of variables
     * @throws JsonProcessingException
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getVariablesPojo(String basePath) throws JsonProcessingException {
        String filePath = basePath + VARIABLE_CONFIG_PATH;
        File variableFile = new File(filePath);
        if (variableFile.exists()) {
            String jsonConfig = hjsonToJson(readConfigFile(variableFile));
            return getModelPojo(jsonConfig, Map.class);
        } else {
            log.info("Variables config file not found at " + filePath);
            return null;
        }
    }

    /**
     * converts all avaiable table config to ElideTableConfig Pojo.
     * @param basePath : root path to model dir
     * @param variables : variables to resolve.
     * @return ElideTableConfig pojo
     * @throws IOException
     */
    public static ElideTableConfig getElideTablePojo(String basePath, Map<String, Object> variables)
            throws IOException {
        Collection<File> tableConfigs = FileUtils.listFiles(new File(basePath + TABLE_CONFIG_PATH),
                new String[] {"hjson"}, false);
        Set<Table> tables = new HashSet<>();
        for (File tableConfig : tableConfigs) {
            String jsonConfig = hjsonToJson(resolveVariables(readConfigFile(tableConfig), variables));
            ElideTableConfig table = getModelPojo(jsonConfig, ElideTableConfig.class);
            tables.addAll(table.getTables());
        }
        ElideTableConfig elideTableConfig = new ElideTableConfig();
        elideTableConfig.setTables(tables);
        return elideTableConfig;
    }

    /**
     * converts security.hjson to ElideSecurityConfig Pojo.
     * @param basePath : root path to model dir.
     * @param variables : variables to resolve.
     * @return ElideSecurityConfig Pojo
     * @throws IOException
     */
    public static ElideSecurityConfig getElideSecurityPojo(String basePath, Map<String, Object> variables)
            throws JsonProcessingException {
        String filePath = basePath + SECURITY_CONFIG_PATH;
        File securityFile = new File(filePath);
        if (securityFile.exists()) {
            String jsonConfig = hjsonToJson(resolveVariables(readConfigFile(securityFile), variables));
            return getModelPojo(jsonConfig, ElideSecurityConfig.class);
        } else {
            log.info("Security config file not found at " + filePath);
            return null;
        }
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

    private static String readConfigFile(File configFile) {
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
}
