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
import java.util.Date;
import java.util.List;

/**
 * {@link ResourceWriter} that writes in XLSX format.
 */
@Slf4j
public class XlsxResourceWriter extends ResourceWriterSupport {
    private final boolean writeHeader;
    private final EntityProjection entityProjection;
    private final SXSSFWorkbook workbook;

    private SXSSFSheet sheet;
    private int recordCount = 0;
    private int rowCount = 0;

    private String localDateFormat = "d/m/yyyy";
    private String localDateTimeFormat = "d/m/yyyy h:mm:ss";
    private String dateFormat = "d/m/yyyy h:mm:ss";

    public XlsxResourceWriter(OutputStream outputStream, boolean writeHeader, EntityProjection entityProjection) {
        this(outputStream, writeHeader, entityProjection, SXSSFWorkbook.DEFAULT_WINDOW_SIZE);
    }

    public XlsxResourceWriter(OutputStream outputStream, boolean writeHeader,
            EntityProjection entityProjection, int rowAccessWindowSize) {
        super(outputStream);
        this.writeHeader = writeHeader;
        this.entityProjection = entityProjection;
        // Rows exceeding the row access window size are flushed to disk
        this.workbook = new SXSSFWorkbook(rowAccessWindowSize);
        this.sheet = this.workbook.createSheet();
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
            for (Attribute attribute : this.entityProjection.getAttributes()) {
                Object object = getValue(resource, attribute);
                SXSSFCell cell = row.createCell(column);
                setValue(cell, object);
                column++;
            }
        } catch (Exception e) {
            log.error("Exception while converting to XLSX: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    protected Object getValue(PersistentResource<?> resource, Attribute attribute) {
        return resource.getAttribute(attribute);
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
        for (Attribute attribute : this.entityProjection.getAttributes()) {
            String header = getHeader(attribute);
            SXSSFCell cell = row.createCell(column);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(header);
            column++;
        }
    }

    protected String getHeader(Attribute attribute) {
        String header = attribute.getName();
        List<String> arguments = attribute.getArguments().stream()
                .map(argument -> argument.getName() + "=" + convert(argument.getValue(), String.class)).toList();
        if (!arguments.isEmpty()) {
            header = header + "(" + String.join(" ", arguments) + ")";
        }
        return header;
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
}
