/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.modelconfig;

/**
 * Dynamic Config enum.
 */
public enum Config {

    TABLE("table",
          "models/tables/",
          "/elideTableSchema.json"),

    SECURITY("security",
             "models/security.hjson",
             "/elideSecuritySchema.json"),

    MODELVARIABLE("variable",
                  "models/variables.hjson",
                  "/elideVariableSchema.json"),

    DBVARIABLE("variable",
               "db/variables.hjson",
               "/elideVariableSchema.json"),

    SQLDBConfig("sqldbconfig",
                "db/sql/",
                "/elideDBConfigSchema.json"),

    NAMESPACEConfig("namespaceconfig",
            "models/namespaces/",
            "/elideNamespaceConfigSchema.json");

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
