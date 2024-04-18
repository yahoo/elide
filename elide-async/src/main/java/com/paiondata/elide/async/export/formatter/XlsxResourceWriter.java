/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import com.paiondata.elide.core.PersistentResource;
import com.paiondata.elide.core.request.Attribute;
import com.paiondata.elide.core.request.EntityProjection;
import com.paiondata.elide.core.utils.ObjectProperties;
import com.paiondata.elide.core.utils.coerce.CoerceUtil;

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

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
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
public class XlsxResourceWriter extends ResourceWriterSupport {
    protected static final String DEFAULT_HEADER_SEPARATOR = "_";
    protected final boolean writeHeader;
    protected final EntityProjection entityProjection;
    protected final SXSSFWorkbook workbook;

    /**
     * Each individual header is a list to handle nested objects.
     */
    protected final List<List<String>> headers;
    protected final Map<String, Attribute> attributes;

    protected SXSSFSheet sheet;
    protected int recordCount = 0;
    protected int rowCount = 0;

    protected String localDateFormat = "d/m/yyyy";
    protected String localDateTimeFormat = "d/m/yyyy h:mm:ss";
    protected String dateFormat = "d/m/yyyy h:mm:ss";
    protected String headerSeparator = DEFAULT_HEADER_SEPARATOR;

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
        if (!closed) {
            if (recordCount == 0) {
                preFormat();
            }
            workbook.write(this.outputStream);
            workbook.close();
            super.close();
            closed = true;
        }
    }

    public void format(PersistentResource<?> resource) {
        if (resource == null) {
            rowCount++;
            return;
        }
        SXSSFRow row = this.sheet.createRow(rowCount++);
        int column = 0;
        Map<String, Object> values = getAttributes(resource);
        List<Object> result = headers.stream().map(header -> {
            return getValue(header, values);
        }).toList();

        for (Object object : result) {
            SXSSFCell cell = row.createCell(column);
            setValue(cell, process(object));
            column++;
        }
    }

    /**
     * Gets the attributes from a resource.
     *
     * @param resource the resource
     * @return the attributes
     */
    protected Map<String, Object> getAttributes(PersistentResource<?> resource) {
        return Attributes.getAttributes(resource);
    }

    /**
     * Allows derived classes a chance to process the attribute value.
     *
     * @param object the object to process
     * @return the processed object
     */
    protected Object process(Object object) {
        return object;
    }

    protected void setValue(SXSSFCell cell, Object object) {
        if (object == null) {
            // Empty cell
            cell.setBlank();
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
        } else if (object instanceof OffsetDateTime value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(localDateTimeFormat));
            cell.setCellValue(value.toLocalDateTime()); // drops the offset
            cell.setCellStyle(cellStyle);
        } else if (object instanceof ZonedDateTime value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(localDateTimeFormat));
            cell.setCellValue(value.toLocalDateTime()); // drops the zone
            cell.setCellStyle(cellStyle);
        } else if (object instanceof Instant value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormat));
            cell.setCellValue(Date.from(value));
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
        } else if (object instanceof Number value) {
            cell.setCellValue(value.doubleValue());
        } else if (object instanceof Enum value) {
            cell.setCellValue(value.name());
        } else if (object instanceof Calendar value) {
            cell.setCellValue(value);
        } else if (object instanceof RichTextString value) {
            cell.setCellValue(value);
        } else if (object instanceof Collection<?> value) {
            cell.setCellValue(convertCollection(value));
        } else {
            cell.setCellValue(convert(object, String.class));
        }
    }

    /**
     * Converts a collection.
     *
     * @param value the collection
     * @return the value
     */
    protected String convertCollection(Collection<?> value) {
        return String.join(";", value.stream().map(v -> convert(v, String.class)).toList());
    }

    /**
     * Generate the header.
     *
     * @param projection the projection
     */
    protected void generateHeader(EntityProjection projection) {
        if (projection.getAttributes() == null || projection.getAttributes().isEmpty()) {
            return;
        }
        CellStyle cellStyle = getHeaderCellStyle();
        SXSSFRow row = this.sheet.createRow(rowCount++);

        int column = 0;

        for (List<String> header : headers) {
            String headerValue = getHeader(header, attributes);
            SXSSFCell cell = row.createCell(column);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(headerValue);
            column++;
        }
    }

    /**
     * Gets the header value.
     *
     * @param header the header
     * @param attributes the attributes
     * @return the header value
     */
    protected String getHeader(List<String> header, Map<String, Attribute> attributes) {
        StringBuilder headerBuilder = new StringBuilder();
        Attribute attribute = attributes.get(header.get(0));
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0 && !StringUtils.isEmpty(attribute.getAlias())) {
                item = attribute.getAlias();
            }
            if (x != 0) {
               headerBuilder.append(headerSeparator);
            }
            headerBuilder.append(item);
        }
        String arguments = Attributes.getArguments(attribute);
        if (!"".equals(arguments)) {
            headerBuilder.append(arguments);
        }
        String headerValue = headerBuilder.toString();
        return headerValue;
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

    /**
     * Gets the value from the attributes.
     *
     * @param header the header
     * @param values the attributes
     * @return the value
     */
    protected Object getValue(List<String> header, Map<String, Object> values) {
        Object value = null;
        for (int x = 0; x < header.size(); x++) {
            String item = header.get(x);
            if (x == 0) {
                value = values.get(item);
            } else {
                value = ObjectProperties.getProperty(value, item);
            }
            if (value == null) {
                break;
            }
        }
        return value;
     }
}
