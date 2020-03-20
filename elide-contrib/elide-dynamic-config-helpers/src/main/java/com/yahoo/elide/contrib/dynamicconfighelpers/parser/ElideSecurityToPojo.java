/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.ElideHjsonUtil;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * The ElideSecurityToPojo class allows conversion of Security config.
 * Converts Hjson format to a Elide Security POJO.
 */
@Slf4j
public class ElideSecurityToPojo {

    /**
     * Parse the security config to POJO.
     * @param jsonConfig hjson string security config
     * @return a ElideSecurity Object
     * @throws Exception
     */
    private ElideSecurity parseSecurity(String jsonConfig) throws Exception {

        if (ElideHjsonUtil.validateDataWithSchema(ElideHjsonUtil.SCHEMA_TYPE_SECURITY, jsonConfig)) {
            return new ObjectMapper().readValue(jsonConfig, ElideSecurity.class);
        }
        else {
            log.error(ElideHjsonUtil.INVALID_ERROR_MSG);
            return null;
        }
    }
    /**
     * Parse File path containing hjson config; http or local.
     * @param configFilePath : File Path to hjson config
     * @return ElideSecurity pojo
     */
    public ElideSecurity parseSecurityConfigFile(String configFilePath) {
        try {
            if (ElideHjsonUtil.isNullOrEmpty(configFilePath)) {
                return null;
            }
            return parseSecurity(ElideHjsonUtil.hjsonToJson(ElideHjsonUtil.readConfigFile(configFilePath)));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
    /**
     * Parse hjson config string as input.
     * @param hjsonConfig : hjson config String
     * @return ElideSecurity pojo
     */
    public ElideSecurity parseSecurityConfig(String hjsonConfig) {
        try {
            if (ElideHjsonUtil.isNullOrEmpty(hjsonConfig)) {
                return null;
            }
            return parseSecurity(ElideHjsonUtil.hjsonToJson(hjsonConfig));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}
