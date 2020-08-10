/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import java.io.File;
/**
 * Dynamic Config enum.
 */
public enum Config {

    TABLE("table",
          "models" + File.separator + "tables" + File.separator,
          File.separator + "elideTableSchema.json"),

    SECURITY("security",
             "models" + File.separator + "security.hjson",
             File.separator + "elideSecuritySchema.json"),

    MODELVARIABLE("variable",
                  "models" + File.separator + "variables.hjson",
                  File.separator + "elideVariableSchema.json"),

    DBVARIABLE("variable",
               "db" + File.separator + "variables.hjson",
               File.separator + "elideVariableSchema.json"),

    SQLDBConfig("sqldbconfig",
                "db" + File.separator + "sql" + File.separator,
                File.separator + "elideDBConfigSchema.json"),

    NONSQLDBConfig("nonsqldbconfig",
                   "db" + File.separator + "nonsql" + File.separator,
                   File.separator + "elideDBConfigSchema.json");

    private final String configType;
    private final String configPath;
    private final String configSchema;

    private Config(String configType, String configPath, String configSchema) {
        this.configPath = configPath;
        this.configType = configType;
        this.configSchema = configSchema;
    }

    public String getConfigType() {
        return configType;
    }

    public String getConfigPath() {
        return configPath;
    }

    public String getConfigSchema() {
        return configSchema;
    }
}
