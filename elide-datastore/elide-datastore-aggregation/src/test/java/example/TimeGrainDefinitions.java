/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

public class TimeGrainDefinitions {
    public static final String DATE_FORMAT = "PARSEDATETIME(FORMATDATETIME({{$$column.expr}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')";
    public static final String MONTH_FORMAT = "PARSEDATETIME(FORMATDATETIME({{$$column.expr}}, 'yyyy-MM-01'), 'yyyy-MM-dd')";
    public static final String QUARTER_FORMAT =
            "PARSEDATETIME(CONCAT(FORMATDATETIME({{$$column.expr}}, 'yyyy-'), LPAD(3 * QUARTER({{$$column.expr}}) - 2, 2, '0'), '-01'), 'yyyy-MM-dd')";
}
