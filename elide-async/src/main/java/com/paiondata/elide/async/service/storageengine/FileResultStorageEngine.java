/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.async.service.storageengine;

import com.paiondata.elide.async.models.TableExportResult;

import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * Validates that the path to read/write is as expected to prevent path
     * traversal.
     *
     * @param path the path to read/write
     * @throws IOException if the path is not expected
     */
    protected void validatePath(Path path) throws IOException {
        Path parent = Paths.get(basePath);
        if (!path.getParent().equals(parent)) {
            throw new FileNotFoundException();
        }
    }

    private InputStream newInputStream(String tableExportID) {
        try {
            Path path = Paths.get(basePath, tableExportID);
            validatePath(path);
            return Files.newInputStream(path);
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new UncheckedIOException(RETRIEVE_ERROR, e);
        }
    }

    private OutputStream newOutputStream(String tableExportID) {
        try {
            Path path = Paths.get(basePath, tableExportID);
            validatePath(path);
            return Files.newOutputStream(path);
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new UncheckedIOException(STORE_ERROR, e);
        }
    }
}
