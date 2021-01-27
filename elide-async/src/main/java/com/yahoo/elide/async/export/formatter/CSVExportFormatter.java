/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.core.PersistentResource;

import com.yahoo.elide.core.request.EntityProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;

import io.reactivex.Observable;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON output format implementation.
 */
@Slf4j
public class CSVExportFormatter implements TableExportFormatter {
    private static final String COMMA = ",";
    private static final String DOUBLE_QUOTES = "\"";

    public static boolean skipCSVHeader = false;
    private ObjectMapper mapper = new ObjectMapper();

    public CSVExportFormatter(boolean skipCSVHeader) {
        this.skipCSVHeader = skipCSVHeader;
    }

    @Override
    public String format(PersistentResource resource, Integer recordNumber) {
        if (resource == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();

        List<Object[]> json2Csv;

        try {
            String jsonStr = JSONExportFormatter.resourceToJSON(mapper, resource);

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

    /**
     * Generate CSV Header when Observable is Empty.
     * @param projection EntityProjection object.
     * @return returns Header string which is in CSV format.
     */
    protected Observable<String> generateCSVHeader(EntityProjection projection) {
        Observable<String> header = Observable.empty();

        String headerString = projection.getAttributes().stream()
        .map(attr -> {
            StringBuilder column = new StringBuilder();
            String alias = attr.getAlias();
            column.append(alias != null && !alias.isEmpty() ? alias : attr.getName());
            return column;
        })
        .map(quotable -> {
            // Quotes at the beginning
            quotable.insert(0, DOUBLE_QUOTES);
            // Quotes at the end
            quotable.append(DOUBLE_QUOTES);
            return quotable;
        })
        .collect(Collectors.joining(COMMA));

        header = Observable.just(headerString);
        return header;
    }

    @Override
    public String preFormat(EntityProjection projection, TableExport query) {
        if (!skipCSVHeader) {
            StringBuilder str = new StringBuilder();
            str.append(generateCSVHeader(projection));
            str.append(System.getProperty("line.separator"));
            return str.toString();
        };
        return null;
    }

    @Override
    public String postFormat(EntityProjection projection, TableExport query) {
        return null;
    }
}
