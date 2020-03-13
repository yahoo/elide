/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.parser;

import com.yahoo.elide.model.ElideTable;
import com.yahoo.elide.util.ElideDynamicConfigConstants;
import com.yahoo.elide.util.ElideHjsonUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElideTableToPojo {

    private ElideTable parseTables(String jsonConfig) throws Exception {

        if (ElideHjsonUtil.validateDataWithSchema(ElideDynamicConfigConstants.SCHEMA_TYPE_TABLE, jsonConfig)) {
            return new ObjectMapper().readValue(jsonConfig, ElideTable.class);
        }
        else {
            log.error(ElideDynamicConfigConstants.INVALID_ERROR_MSG);
            return null;
        }
    }

    /**
     * Pass File path containing hjson config; http or local.
     * @param configFilePath : File Path to hjson config
     * @return ElideTable pojo
     */
    public ElideTable parseTableConfigFile(String configFilePath) {

        try {
            if (ElideHjsonUtil.isNull(configFilePath)) {
                return null;
            }
            return parseTables(ElideHjsonUtil.hjsonToJson(ElideHjsonUtil.readConfigFile(configFilePath)));
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
    public ElideTable parseTableConfig(String hjsonConfig) {
        try {
            if (ElideHjsonUtil.isNull(hjsonConfig)) {
                return null;
            }
            return parseTables(ElideHjsonUtil.hjsonToJson(hjsonConfig));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }
}
