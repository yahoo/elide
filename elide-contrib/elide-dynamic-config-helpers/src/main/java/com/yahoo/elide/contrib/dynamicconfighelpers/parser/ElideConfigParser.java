/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpersUtil;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import org.json.JSONObject;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
/**
 * Parses Hjson configuration and returns Dynamic Model POJO
 */
@Data
public class ElideConfigParser {

    private ElideTable elideTable = new ElideTable();
    private ElideSecurity elideSecurity;
    private Map<String, Object> variables;
    private DynamicConfigHelpersUtil util = new DynamicConfigHelpersUtil();

    /**
     * Parse File path containing hjson config; http or local.
     * @param localConfigPath : File Path to hjson config
     * @throws NullPointerException
     */
    public void parseConfigPath(String localConfigPath) {
        try {
            if (util.isNullOrEmpty(localConfigPath)) {
                throw new NullPointerException("config path is null");
            }
            parse(localConfigPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * Parse hjson config string as input.
     * @param hjsonConfig : hjson config String
     * @return ElideTable pojo
     */
    public Object parseConfigString(String hjsonConfig, String configType) {
        try {
            if (util.isNullOrEmpty(hjsonConfig)) {
                return null;
            }
            return parse(util.hjsonToJson(hjsonConfig), configType);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return null;
        }
    }

    private Object parse(String jsonConfig, String inputConfigType) throws Exception {
        JSONObject configType = util.schemaToJsonObject(inputConfigType);

        if (configType != null && util.validateDataWithSchema(configType, jsonConfig)) {
            return util.getModelPojo(inputConfigType, jsonConfig);
        }
        else {
            log.error(DynamicConfigHelpersUtil.INVALID_ERROR_MSG);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void parse(String localConfigPath) throws Exception {
        //variables
        String varibleJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE).get(0);
        this.variables = (Map<String, Object>) parse(varibleJson, DynamicConfigHelpersUtil.SCHEMA_TYPE_VARIABLE);

        //security
        String securityJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY).get(0);
        this.elideSecurity = (ElideSecurity) parse(securityJson, DynamicConfigHelpersUtil.SCHEMA_TYPE_SECURITY);

        //table
        Set<Table> tables = new HashSet<>();
        List<String> tablesJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE);
        for (String tableJson : tablesJson) {
            ElideTable table = (ElideTable) parse(tableJson, DynamicConfigHelpersUtil.SCHEMA_TYPE_TABLE);
            tables.addAll(table.getTables());
        }
        this.elideTable.setTables(tables);
    }
}
