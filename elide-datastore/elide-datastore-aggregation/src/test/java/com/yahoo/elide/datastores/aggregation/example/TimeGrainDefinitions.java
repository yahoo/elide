/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.example;

public class TimeGrainDefinitions {
    public static final String DATE_FORMAT = "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
    public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME({{}}, 'yyyy-MM'), 'yyyy-MM')";
    public static final String QUARTER_FORMAT =
            "PARSEDATETIME(CONCAT(FORMATDATETIME({{}}, 'yyyy-'), 3 * QUARTER({{}}) - 2), 'yyyy-MM')";
}
