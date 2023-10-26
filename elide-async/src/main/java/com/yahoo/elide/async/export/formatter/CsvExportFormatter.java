/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.Elide;
import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.opendevl.JFlat;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JSON output format implementation.
 */
@Slf4j
public class CsvExportFormatter implements TableExportFormatter {
    private static final String COMMA = ",";
    private static final String LINE_SEPARATOR = "\r\n"; // CRLF rfc4180

    private boolean writeHeader = true;
    private ObjectMapper mapper;

    public CsvExportFormatter(Elide elide, boolean writeHeader) {
        this.writeHeader = writeHeader;
        this.mapper = elide.getObjectMapper();
    }

    @Override
    public void format(TableExportFormatterContext context, PersistentResource resource, OutputStream outputStream)
            throws IOException {
        if (resource == null) {
            return;
        }

        StringBuilder str = new StringBuilder();

        List<Object[]> json2Csv;

        try {
            String jsonStr = JsonExportFormatter.resourceToJSON(mapper, resource);

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
                //The arrays.toString returns o/p with [ and ] at the beginning and end. So need to exclude them.
                objString = objString.substring(1, objString.length() - 1);
                str.append(objString);
                str.append(LINE_SEPARATOR);
            }
        } catch (Exception e) {
            log.error("Exception while converting to CSV: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
        outputStream.write(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate CSV Header when Observable is Empty.
     * @param projection EntityProjection object.
     * @return returns Header string which is in CSV format.
     */
    private String generateCSVHeader(EntityProjection projection) {
        if (projection.getAttributes() == null) {
            return "";
        }

        String header = projection.getAttributes().stream()
                .map(this::toHeader)
                .collect(Collectors.joining(COMMA));
        if (!"".equals(header)) {
            return header + LINE_SEPARATOR;
        }
        return header;
    }

    @Override
    public void preFormat(TableExportFormatterContext context, OutputStream outputStream) throws IOException {
        if (context.getEntityProjection() == null || !writeHeader) {
            return;
        }
        outputStream.write(generateCSVHeader(context.getEntityProjection()).getBytes(StandardCharsets.UTF_8));
    }

    private String toHeader(Attribute attribute) {
        if (attribute.getArguments() == null || attribute.getArguments().size() == 0) {
            return quote(attribute.getName());
        }

        StringBuilder header = new StringBuilder();
        header.append(attribute.getName());
        header.append("(");

        header.append(attribute.getArguments().stream()
                .map((arg) -> arg.getName() + "=" + arg.getValue())
                .collect(Collectors.joining(" ")));

        header.append(")");

        return quote(header.toString());
    }

    private String quote(String toQuote) {
        return "\"" + toQuote + "\"";
    }
}
