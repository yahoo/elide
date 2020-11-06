/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ISOFormatUtil {

    public static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final SimpleDateFormat ISO_FORMATTER = new SimpleDateFormat(ISO_FORMAT);

    public static java.util.Date formatDateString(String dateString, SimpleDateFormat toFormat) throws ParseException {
        try {
            return new Timestamp(toFormat.parse(dateString).getTime());
        } catch (ParseException e) {
            return toFormat.parse(toFormat.format(ISO_FORMATTER.parse(dateString)));
        }
    }
}
