/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.modelconfig;

import com.paiondata.elide.modelconfig.model.DBConfig;
import com.paiondata.elide.modelconfig.model.NamespaceConfig;
import com.paiondata.elide.modelconfig.model.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * Returns the model, security role, and database connection configurations derived from HJSON
 * files or other dynamic configuration systems.
 */
public interface DynamicConfiguration {

    /**
     * Returns the set of dynamically configured tables.
     * @return a set of tables.
     */
    default Set<Table> getTables() {
        return new HashSet<>();
    }

    /**
     * Return the set of role names leveraged by dynamic models.
     * @return a set of role names.
     */
    default Set<String> getRoles() {
        return new HashSet<>();
    }

    /**
     * Returns a set of database connection configurations.
     * @return a set of database configurations.
     */
    default Set<DBConfig> getDatabaseConfigurations() {
        return new HashSet<>();
    }

    /**
     * Returns a set of Namespace configurations.
     * @return a set of Namespace configurations.
     */
    default Set<NamespaceConfig> getNamespaceConfigurations() {
        return new HashSet<>();
    }
}
