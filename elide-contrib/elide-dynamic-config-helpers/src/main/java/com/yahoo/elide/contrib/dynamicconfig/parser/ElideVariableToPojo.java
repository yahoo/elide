/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfig.parser;

import com.yahoo.elide.contrib.dynamicconfig.ElideDynamicConfigConstants;
import com.yahoo.elide.contrib.dynamicconfig.ElideHjsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class ElideVariableToPojo {

    @SuppressWarnings("unchecked")
    private Map<String, Object> populateVariableMap(String json) throws Exception {
        if (ElideHjsonUtil.validateDataWithSchema(ElideDynamicConfigConstants.SCHEMA_TYPE_VARIABLE, json)) {
            return new ObjectMapper().readValue(json, Map.class);
        }
        else {
            log.error(ElideDynamicConfigConstants.INVALID_ERROR_MSG);
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
            if (ElideHjsonUtil.isNull(configFilePath)) {
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
            if (ElideHjsonUtil.isNull(hjsonConfig)) {
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
