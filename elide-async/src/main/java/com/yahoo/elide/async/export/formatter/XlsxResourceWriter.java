/*
 * Copyright 2023, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.export.formatter;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;

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
import java.util.Date;

/**
 * {@link ResourceWriter} that writes in XLSX format.
 */
@Slf4j
public class XlsxResourceWriter extends ResourceWriterSupport {
    private boolean writeHeader = true;
    private EntityProjection entityProjection;
    private int recordCount = 0;
    private int rowCount = 0;
    private final SXSSFWorkbook workbook;
    private SXSSFSheet sheet;

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
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy"));
            cell.setCellValue(value);
            cell.setCellStyle(cellStyle);
        } else if (object instanceof LocalDateTime value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy h:mm:ss"));
            cell.setCellValue(value);
            cell.setCellStyle(cellStyle);
        } else if (object instanceof Date value) {
            CreationHelper createHelper = this.workbook.getCreationHelper();
            CellStyle cellStyle = this.workbook.createCellStyle();
            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yyyy h:mm:ss"));
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
        } else {
            throw new IllegalArgumentException("Unexpected attribute value.");
        }
    }

    /**
     * Generate the header.
     *
     * @param projection the projection
     */
    private void generateHeader(EntityProjection projection) {
        if (projection.getAttributes() == null) {
            return;
        }
        CellStyle cellStyle = getHeaderCellStyle();
        SXSSFRow row = this.sheet.createRow(rowCount++);

        int column = 0;
        for (Attribute attribute : this.entityProjection.getAttributes()) {
            String header = attribute.getName();
            SXSSFCell cell = row.createCell(column);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(header);
            column++;
        }
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
}
