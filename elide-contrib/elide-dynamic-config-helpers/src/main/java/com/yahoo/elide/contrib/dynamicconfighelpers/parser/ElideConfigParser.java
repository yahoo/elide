/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.Util;

import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElideConfigParser {

    private Object parse(String jsonConfig, String inputConfigType) throws Exception {
        JSONObject configType = Util.schemaToJsonObject(inputConfigType);

        if (configType != null && Util.validateDataWithSchema(configType, jsonConfig)) {
            return Util.getModelPojo(inputConfigType, jsonConfig);
        }
        else {
            log.error(Util.INVALID_ERROR_MSG);
            return null;
        }
    }

    /**
     * Pass File path containing hjson config; http or local.
     * @param configFilePath : File Path to hjson config
     * @return ElideTable pojo
     */
    public Object parseConfigFile(String configFilePath, String configType) {

        try {
            if (Util.isNullOrEmpty(configFilePath)) {
                return null;
            }
            return parse(Util.hjsonToJson(Util.readConfigFile(configFilePath)), configType);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Pass hjson config string as input.
     * @param hjsonConfig : hjson config String
     * @return ElideTable pojo
     */
    public Object parseConfigString(String hjsonConfig, String configType) {
        try {
            if (Util.isNullOrEmpty(hjsonConfig)) {
                return null;
            }
            return parse(Util.hjsonToJson(hjsonConfig), configType);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}
