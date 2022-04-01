/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig;

import com.yahoo.elide.modelconfig.model.DBConfig;

/**
 * Interface for providing password extractor implementation.
 */
public interface DBPasswordExtractor {
    /**
     * Extract password for connecting to DB.
     * @param config DB Config POJO.
     * @return String DB Connection Password.
     */
    String getDBPassword(DBConfig config);
}
