/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
/**
 * Parses Hjson configuration from local file path and initializes Dynamic Model POJOs
 */
@Data
public class ElideConfigParser {

    private ElideTableConfig elideTableConfig;
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> variables;

    /**
     * Initialize Dynamic config objects.
     * @param localFilePath : Path to dynamic model config dir.
     * @throws IllegalArgumentException
     */
    public ElideConfigParser(String localFilePath) {

        if (DynamicConfigHelpers.isNullOrEmpty(localFilePath)) {
            throw new IllegalArgumentException("Config path is null");
        }
        try {
            String basePath = DynamicConfigHelpers.formatFilePath(localFilePath);

            this.variables = DynamicConfigHelpers.getVaribalesPojo(basePath);
            this.elideTableConfig = DynamicConfigHelpers.getElideTablePojo(basePath);
            this.elideSecurityConfig = DynamicConfigHelpers.getElideSecurityPojo(basePath);

        } catch (JsonProcessingException e) {
            log.error("Error while parsing dynamic config at location " + localFilePath);
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
