/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.engine.schema;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.datastores.aggregation.dimension.Dimension;
import com.yahoo.elide.datastores.aggregation.dimension.TimeDimension;
import com.yahoo.elide.datastores.aggregation.time.TimeGrain;

import java.util.TimeZone;

public class SQLTimeDimension extends SQLDimension implements TimeDimension {

    public SQLTimeDimension(Dimension dimension, String columnAlias, String tableAlias) {
        super(dimension, columnAlias, tableAlias);
    }

    public SQLTimeDimension(Dimension dimension, String columnAlias, String tableAlias, Path joinPath) {
        super(dimension, columnAlias, tableAlias, joinPath);
    }

    @Override
    public TimeZone getTimeZone() {
        //TODO
        return null;

    }

    @Override
    public TimeGrain getTimeGrain() {
        //TODO
        return null;
    }
}
