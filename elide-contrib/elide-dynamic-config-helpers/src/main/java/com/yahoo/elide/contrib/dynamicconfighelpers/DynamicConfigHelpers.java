/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

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

    private static final String TABLE_CONFIG_PATH = "tables/";
    private static final String SECURITY_CONFIG_PATH = "security.hjson";
    private static final String VARIABLE_CONFIG_PATH = "variables.hjson";
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
    public static Map<String, Object> getVaribalesPojo(String basePath) throws JsonProcessingException {
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
     * @return ElideTableConfig pojo
     * @throws JsonProcessingException
     */
    public static ElideTableConfig getElideTablePojo(String basePath) throws JsonProcessingException {
        Collection<File> tableConfigs = FileUtils.listFiles(new File(basePath + TABLE_CONFIG_PATH),
                new String[] {"hjson", "json"}, false);
        Set<Table> tables = new HashSet<>();
        for (File tableConfig : tableConfigs) {
            String jsonConfig = hjsonToJson(readConfigFile(tableConfig));
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
     * @return ElideSecurityConfig Pojo
     * @throws JsonProcessingException
     */
    public static ElideSecurityConfig getElideSecurityPojo(String basePath) throws JsonProcessingException {
        String filePath = basePath + SECURITY_CONFIG_PATH;
        File securityFile = new File(filePath);
        if (securityFile.exists()) {
            String jsonConfig = hjsonToJson(readConfigFile(securityFile));
            return getModelPojo(jsonConfig, ElideSecurityConfig.class);
        } else {
            log.info("Security config file not found at " + filePath);
            return null;
        }
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
