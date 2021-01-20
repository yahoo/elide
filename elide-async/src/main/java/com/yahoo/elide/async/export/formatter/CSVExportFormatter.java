/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * JSON output format implementation.
 */
@Slf4j
public class CSVExportFormatter implements TableExportFormatter {
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public String format(PersistentResource resource, Integer recordNumber) {
        if (resource == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();

        List<Object[]> json2Csv;

        try {
            String jsonStr = resourceToJSON(resource);

            JFlat flat = new JFlat(jsonStr);

            json2Csv = flat.json2Sheet().headerSeparator("_").getJsonAsSheet();

            int index = 0;

            for (Object[] obj : json2Csv) {
                // convertToCSV is called once for each PersistentResource in the observable.
                // json2Csv will always have 2 entries.
                // 0th index is the header so we need to skip the header.
                if (index++ == 0) {
                    continue;
                }

                String objString = Arrays.toString(obj);
                if (objString != null) {
                    //The arrays.toString returns o/p with [ and ] at the beginning and end. So need to exclude them.
                    objString = objString.substring(1, objString.length() - 1);
                }
                str.append(objString);
            }
        } catch (Exception e) {
            log.error("Exception while converting to CSV: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }

    private String resourceToJSON(PersistentResource resource) {
        StringBuilder str = new StringBuilder();

        try {
            str.append(mapper.writeValueAsString(resource.getObject()));
        } catch (JsonProcessingException e) {
            log.error("Exception when converting to JSON {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        return str.toString();
    }
}
