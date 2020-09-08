/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.EntityDictionary;
import com.yahoo.elide.datastores.aggregation.QueryEngine;
import com.yahoo.elide.datastores.aggregation.query.Query;
import com.yahoo.elide.datastores.aggregation.queryengines.AbstractEntityHydrator;

import java.sql.ResultSet;

/**
 * {@link SQLEntityHydrator} hydrates the entity loaded by
 * {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}.
 */
public class SQLEntityHydrator extends AbstractEntityHydrator {

    /**
     * Constructor.
     *
     * @param results The loaded objects from {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)}
     * @param query  The query passed to {@link QueryEngine#executeQuery(Query, QueryEngine.Transaction)} to load the
     *               objects
     * @param entityDictionary  An object that sets entity instance values and provides entity metadata info
     */
    public SQLEntityHydrator(
            ResultSet results,
            Query query,
            EntityDictionary entityDictionary
    ) {
        super(results, query, entityDictionary);
    }
}
