/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpersUtil;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import org.json.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
/**
 * Parses Hjson configuration (String or from local file path) and generates Dynamic Model POJO
 */
@Data
public class ElideConfigParser {

    private ElideTableConfig elideTableConfig;
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> variables;
    private DynamicConfigHelpersUtil util = new DynamicConfigHelpersUtil();

    /**
     * Parse File path containing hjson config; http or local.
     * @param localFilePath : File Path to hjson config
     * @throws NullPointerException
     */
    public void parseConfigPath(String localFilePath) {
        try {
            if (util.isNullOrEmpty(localFilePath)) {
                throw new NullPointerException("Config path is null");
            }
            parse(localFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * Parse hjson config string.
     * @param hjsonConfig : hjson config String
     * @throws NullPointerException
     */
    public void parseConfigString(String hjsonConfig, String configType) {
        try {
            if (util.isNullOrEmpty(hjsonConfig)) {
                throw new NullPointerException("config is null");
            }
            populatePojo(util.hjsonToJson(hjsonConfig), configType);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    private void parse(String localConfigPath) throws Exception {
        //variables
        String varibleJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE).get(0);
        populatePojo(varibleJson, DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE);

        //security
        String securityJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY).get(0);
        populatePojo(util.resolveVariables(securityJson, this.variables),
                DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY);

        //table
        Set<Table> tables = new HashSet<>();
        for (String tableJson : util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE)) {
            ElideTableConfig table = (ElideTableConfig) parseJsonConfig(
                    util.resolveVariables(tableJson, this.variables),
                    DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE);
            tables.addAll(table.getTables());
        }
        this.elideTableConfig = new ElideTableConfig();
        this.elideTableConfig.setTables(tables);
    }

    @SuppressWarnings("unchecked")
    private void populatePojo(String jsonConfig, String configType) throws Exception {
        switch (configType) {
        case DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE:
            this.variables = (Map<String, Object>) parseJsonConfig(jsonConfig,
                    DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE);
            break;

        case DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY:
            this.elideSecurityConfig = (ElideSecurityConfig) parseJsonConfig(jsonConfig,
                    DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY);
            break;

        case DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE:
            this.elideTableConfig = (ElideTableConfig) parseJsonConfig(jsonConfig,
                    DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE);
            break;
        }
    }

    private Object parseJsonConfig(String jsonConfig, String inputConfigType) throws Exception {
        JSONObject configType = util.schemaToJsonObject(inputConfigType);

        if (configType != null && util.isConfigValid(configType, jsonConfig)) {
            return util.getModelPojo(inputConfigType, jsonConfig);
        }
        else {
            log.error(DynamicConfigHelpersUtil.INVALID_ERROR_MSG);
            return null;
        }
    }
}
