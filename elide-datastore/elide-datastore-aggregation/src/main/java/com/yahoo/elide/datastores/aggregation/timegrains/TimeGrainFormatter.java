/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.timegrains;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/** Interface for ISO timegrain Support. */
public interface TimeGrainFormatter {

    String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    SimpleDateFormat ISO_FORMATTER = new SimpleDateFormat(ISO_FORMAT);

    static Timestamp formatDateString(SimpleDateFormat formatter, String val) throws ParseException {
        try {
            return new Timestamp(formatter.parse((String) val).getTime());
        } catch (ParseException pe) {
            return new Timestamp(ISO_FORMATTER.parse((String) val).getTime());
        }
    }
}
