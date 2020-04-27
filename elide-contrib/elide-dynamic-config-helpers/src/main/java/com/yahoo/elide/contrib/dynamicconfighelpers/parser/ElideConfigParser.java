/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
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
 * Parses Hjson configuration (String or from local file path) and generates Dynamic Model POJO
 */
@Data
public class ElideConfigParser {

	private ElideTableConfig elideTableConfig;
	private ElideSecurityConfig elideSecurityConfig;
	private Map<String, Object> variables;
	private DynamicConfigHelpers util = new DynamicConfigHelpers();

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

	private void parse(String localConfigPath) throws Exception {
		//variables
		String varibleJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpers.SCHEMA_TYPE_VARIABLE).get(0);
		populatePojo(varibleJson, DynamicConfigHelpers.SCHEMA_TYPE_VARIABLE);

		//security
		String securityJson = util.getJsonConfig(localConfigPath, DynamicConfigHelpers.SCHEMA_TYPE_SECURITY).get(0);
		populatePojo(securityJson, DynamicConfigHelpers.SCHEMA_TYPE_SECURITY);

		//table
		populateTablesPojo(util.getJsonConfig(localConfigPath, DynamicConfigHelpers.SCHEMA_TYPE_TABLE));
	}

	@SuppressWarnings("unchecked")
	private void populatePojo(String jsonConfig, String configType) throws Exception {
		switch (configType) {
		case DynamicConfigHelpers.SCHEMA_TYPE_VARIABLE:
			this.variables = (Map<String, Object>) parseJsonConfig(jsonConfig,
					DynamicConfigHelpers.SCHEMA_TYPE_VARIABLE);
			break;

		case DynamicConfigHelpers.SCHEMA_TYPE_SECURITY:
			this.elideSecurityConfig = (ElideSecurityConfig) parseJsonConfig(jsonConfig,
					DynamicConfigHelpers.SCHEMA_TYPE_SECURITY);
			break;
		}
	}

	private void populateTablesPojo(List<String> tablesJson) throws Exception {
		Set<Table> tables = new HashSet<>();
		for (String tableJson : tablesJson) {
			ElideTableConfig table = (ElideTableConfig) parseJsonConfig(tableJson,
					DynamicConfigHelpers.SCHEMA_TYPE_TABLE);
			tables.addAll(table.getTables());
		}
		this.elideTableConfig = new ElideTableConfig();
		this.elideTableConfig.setTables(tables);
	}


	private Object parseJsonConfig(String jsonConfig, String inputConfigType) throws Exception {
		JSONObject configType = util.schemaToJsonObject(inputConfigType);

		if (configType != null && util.isConfigValid(configType, jsonConfig)) {
			return util.getModelPojo(inputConfigType, jsonConfig);
		}
		else {
			log.error(DynamicConfigHelpers.INVALID_ERROR_MSG);
			return null;
		}
	}
}
