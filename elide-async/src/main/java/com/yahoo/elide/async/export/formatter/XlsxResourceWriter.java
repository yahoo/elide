/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link ResourceWriter} that writes in XLSX format.
 */
@Slf4j
public class XlsxResourceWriter extends ResourceWriterSupport {
    private static final String HEADER_SEPARATOR = "_";
    private final boolean writeHeader;
    private final EntityProjection entityProjection;
    private final SXSSFWorkbook workbook;
    private final List<List<String>> headers;
    private final Map<String, Attribute> attributes;
    private final ObjectMapper objectMapper;

    private SXSSFSheet sheet;
    private int recordCount = 0;
    private int rowCount = 0;

    private String localDateFormat = "d/m/yyyy";
    private String localDateTimeFormat = "d/m/yyyy h:mm:ss";
    private String dateFormat = "d/m/yyyy h:mm:ss";

    public XlsxResourceWriter(OutputStream outputStream, ObjectMapper objectMapper, boolean writeHeader,
            EntityProjection entityProjection) {
        this(outputStream, objectMapper, writeHeader, entityProjection, SXSSFWorkbook.DEFAULT_WINDOW_SIZE);
    }

    public XlsxResourceWriter(OutputStream outputStream, ObjectMapper objectMapper, boolean writeHeader,
            EntityProjection entityProjection, int rowAccessWindowSize) {
        super(outputStream);
        this.writeHeader = writeHeader;
        this.entityProjection = entityProjection;
        // Rows exceeding the row access window size are flushed to disk
        this.workbook = new SXSSFWorkbook(rowAccessWindowSize);
        this.sheet = this.workbook.createSheet();
        this.objectMapper = objectMapper;
        this.headers = entityProjection != null ? Attributes.getHeaders(objectMapper, entityProjection.getAttributes())
                : Collections.emptyList();
        this.attributes = entityProjection != null && entityProjection.getAttributes() != null ? entityProjection
                .getAttributes().stream().collect(Collectors.toMap(Attribute::getName, Function.identity()))
                : Collections.emptyMap();

    }

    @Override
    public void write(PersistentResource<?> resource) throws IOException {
        if (recordCount == 0) {
            preFormat();
        }
        recordCount++;
        format(resource);
    }

    @Override
    public void close() throws IOException {
        if (recordCount == 0) {
            preFormat();
        }
        workbook.write(this.outputStream);
        workbook.close();
        super.close();
    }

    public void format(PersistentResource<?> resource) throws IOException {
        if (resource == null) {
            rowCount++;
            return;
        }
        SXSSFRow row = this.sheet.createRow(rowCount++);
        int column = 0;
        try {
            @SuppressWarnings("rawtypes")
            Map values = objectMapper.convertValue(Attributes.getAttributes(resource), Map.class);
            List<Object> result = headers.stream().map(header -> {
                return getValue(header, values);
            }).toList();

            for (Object object : result) {
                List<String> header = headers.get(column);
                if (header.size() == 1) {
                    String key = header.get(0);
                    object = resource.getAttribute(this.attributes.get(key));
                }
                SXSSFCell cell = row.createCell(column);
                setValue(cell, object);
                column++;
            }
        } catch (Exception e) {
            log.error("Exception while converting to XLSX: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    protected void setValue(SXSSFCell cell, Object object) {
        if (object == null) {
           // Empty cell
        } else if (object instanceof String value) {
            cell.setCellValue(value);
        } else if (object instanceof Boolean value) {
            cell.setCellValue(value);
        } else if (object instanceof LocalDate value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(localDateFormat));
            cell.setCellValue(value);
            cell.setCellStyle(cellStyle);
        } else if (object instanceof LocalDateTime value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(localDateTimeFormat));
            cell.setCellValue(value);
            cell.setCellStyle(cellStyle);
        } else if (object instanceof Date value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormat));
            cell.setCellValue(value);
            cell.setCellStyle(cellStyle);
        } else if (object instanceof Double value) {
            cell.setCellValue(value);
        } else if (object instanceof Float value) {
            cell.setCellValue(value);
        } else if (object instanceof Enum value) {
            cell.setCellValue(value.name());
        } else if (object instanceof Calendar value) {
            cell.setCellValue(value);
        } else if (object instanceof RichTextString value) {
            cell.setCellValue(value);
        } else if (object instanceof Collection<?> value) {
            cell.setCellValue(convertCollection(value));
        } else {
            throw new IllegalArgumentException("Unexpected attribute value.");
        }
    }

    protected String convertCollection(Collection<?> value) {
        return String.join(";", value.stream().map(v -> convert(v, String.class)).toList());
    }

    /**
     * Generate the header.
     *
     * @param projection the projection
     */
    private void generateHeader(EntityProjection projection) {
        if (projection.getAttributes() == null || projection.getAttributes().isEmpty()) {
            return;
        }
        CellStyle cellStyle = getHeaderCellStyle();
        SXSSFRow row = this.sheet.createRow(rowCount++);

        int column = 0;

        for (List<String> header : headers) {
            StringBuilder headerBuilder = new StringBuilder();
            Attribute attribute = attributes.get(header.get(0));
            for (int x = 0; x < header.size(); x++) {
                String item = header.get(x);
                if (x == 0 && !StringUtils.isEmpty(attribute.getAlias())) {
                    item = attribute.getAlias();
                }
                if (x != 0) {
                   headerBuilder.append(HEADER_SEPARATOR);
                }
                headerBuilder.append(item);
            }
            String arguments = Attributes.getArguments(attribute);
            if (!"".equals(arguments)) {
                headerBuilder.append(arguments);
            }
            String headerValue = headerBuilder.toString();
            SXSSFCell cell = row.createCell(column);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(headerValue);
            column++;
        }
    }

    protected <T> T convert(Object value, Class<T> clazz) {
        return CoerceUtil.coerce(value, clazz);
    }

    public void preFormat() throws IOException {
        if (this.entityProjection == null || !writeHeader) {
            return;
        }
        generateHeader(this.entityProjection);
    }

    protected CellStyle getHeaderCellStyle() {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        cellStyle.setFont(font);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }

    public String getLocalDateFormat() {
        return localDateFormat;
    }

    public void setLocalDateFormat(String localDateFormat) {
        this.localDateFormat = localDateFormat;
    }

    public String getLocalDateTimeFormat() {
        return localDateTimeFormat;
    }

    public void setLocalDateTimeFormat(String localDateTimeFormat) {
        this.localDateTimeFormat = localDateTimeFormat;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @SuppressWarnings("rawtypes")
    private Object getValue(List<String> header, Map values) {
        Object value = null;
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0) {
                value = values.get(item);
            } else {
                if (value instanceof Map m) {
                    value = m.get(item);
                }
            }
        }
        return value;
     }
}
