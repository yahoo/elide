/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.ElideHjsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * The ElideVariableToPojo class allows conversion of Variable config.
 * Converts HJSON format to a Elide Security POJO.
 */
@Slf4j
public class ElideVariableToPojo {

    /**
     * Parse the variable config to POJO.
     * @param jsonConfig hjson string variable config
     * @return a Map of Key Values representing the variable names and values
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> populateVariableMap(String json) throws Exception {
        if (ElideHjsonUtil.validateDataWithSchema(ElideHjsonUtil.SCHEMA_TYPE_VARIABLE, json)) {
            return new ObjectMapper().readValue(json, Map.class);
        }
        else {
            log.error(ElideHjsonUtil.INVALID_ERROR_MSG);
            return null;
        }
    }
    /**
     * Pass File path containing hjson config; http or local.
     * @param configFilePath : File Path to hjson config
     * @return Map of Variable key value pairs
     */
    public Map<String, Object> parseVariableConfigFile(String configFilePath) {
        try {
            if (ElideHjsonUtil.isNullOrEmpty(configFilePath)) {
                return null;
            }
            return populateVariableMap(ElideHjsonUtil.hjsonToJson(ElideHjsonUtil.readConfigFile(configFilePath)));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
    /**
     * Pass hjson config string as input.
     * @param hjsonConfig : hjson config String
     * @return Map of variable key value pairs
     */
    public Map<String, Object> parseVariableConfig(String hjsonConfig) {
        try {
            if (ElideHjsonUtil.isNullOrEmpty(hjsonConfig)) {
                return null;
            }
            return populateVariableMap(ElideHjsonUtil.hjsonToJson(hjsonConfig));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}
