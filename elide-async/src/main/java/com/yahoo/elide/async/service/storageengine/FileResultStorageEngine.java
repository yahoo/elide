/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.TableExportResult;

import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Default implementation of ResultStorageEngine that stores results on local filesystem.
 * It supports Async Module to store results with Table Export query.
 */
@Singleton
@Slf4j
@Getter
public class FileResultStorageEngine implements ResultStorageEngine {
    @Setter private String basePath;

    /**
     * Constructor.
     * @param basePath basePath for storing the files. Can be absolute or relative.
     * @param resultTypeFileExtensionMapper Enable file extensions.
     */
    public FileResultStorageEngine(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public TableExportResult storeResults(String tableExportID, Consumer<OutputStream> result) {
        log.debug("store TableExportResults for Download");
       TableExportResult exportResult = new TableExportResult();
       try (OutputStream writer = newOutputStream(tableExportID)) {
           result.accept(writer);
       } catch (IOException e) {
           throw new UncheckedIOException(STORE_ERROR, e);
       }
       return exportResult;
    }

    @Override
    public Consumer<OutputStream> getResultsByID(String tableExportID) {
        log.debug("getTableExportResultsByID");
        return outputStream -> {
            try {
                newInputStream(tableExportID).transferTo(outputStream);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private InputStream newInputStream(String tableExportID) {
        try {
            return Files.newInputStream(Paths.get(basePath + File.separator + tableExportID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new UncheckedIOException(RETRIEVE_ERROR, e);
        }
    }

    private OutputStream newOutputStream(String tableExportID) {
        try {
            return Files.newOutputStream(Paths.get(basePath + File.separator + tableExportID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new UncheckedIOException(STORE_ERROR, e);
        }
    }
}
