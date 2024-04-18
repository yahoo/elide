/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.async.export.formatter;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class XlsxTestUtils {
    public static List<Object[]> read(byte[] data) {
        List<Object[]> result = new ArrayList<>();
        read(data, wb -> {
            XSSFSheet sheet = wb.getSheetAt(0);
            for (int x = sheet.getFirstRowNum(); x <= sheet.getLastRowNum(); x++) {
                result.add(readRow(sheet, x));
            }
        });
        return result;
    }

    public static void read(byte[] data, Consumer<XSSFWorkbook> consumer) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            consumer.accept(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Object[] readRow(XSSFSheet sheet, int rowNumber) {
        XSSFRow row = sheet.getRow(rowNumber);
        if (row == null) {
            return null;
        }
        short start = row.getFirstCellNum();
        short end = row.getLastCellNum();
        Object[] result = new Object[end];
        for (short colIx = start; colIx < end; colIx++) {
            XSSFCell cell = row.getCell(colIx);
            if (cell == null) {
                continue;
            } else if (CellType.NUMERIC.equals(cell.getCellType())) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    result[colIx] = cell.getDateCellValue();
                } else {
                    result[colIx] = cell.getNumericCellValue();
                }
            } else {
                result[colIx] = cell.getStringCellValue();
            }
        }
        return result;
    }
}
