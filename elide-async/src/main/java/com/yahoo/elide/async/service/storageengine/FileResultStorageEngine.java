/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.FileExtensionType;
import com.yahoo.elide.async.models.TableExport;
import com.yahoo.elide.async.models.TableExportResult;

import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.inject.Singleton;

/**
 * Default implementation of ResultStorageEngine that stores results on local filesystem.
 * It supports Async Module to store results with Table Export query.
 */
@Singleton
@Slf4j
@Getter
public class FileResultStorageEngine implements ResultStorageEngine {
    @Setter private String basePath;
    @Setter private boolean enableExtension;

    /**
     * Constructor.
     * @param basePath basePath for storing the files. Can be absolute or relative.
     * @param enableExtension Enable file extensions.
     */
    public FileResultStorageEngine(String basePath, boolean enableExtension) {
        this.basePath = basePath;
        this.enableExtension = enableExtension;
    }

    @Override
    public TableExportResult storeResults(TableExport tableExport, Observable<String> result) {
        log.debug("store TableExportResults for Download");
        String extension = this.isExtensionEnabled()
                ? tableExport.getResultType().getFileExtensionType().getExtension()
                : FileExtensionType.NONE.getExtension();

       TableExportResult exportResult = new TableExportResult();
        try (BufferedWriter writer = getWriter(tableExport.getId(), extension)) {
            result
                .map(record -> record.concat(System.lineSeparator()))
                .subscribe(
                        recordCharArray -> {
                            writer.write(recordCharArray);
                            writer.flush();
                        },
                        throwable -> {
                            StringBuilder message = new StringBuilder();
                            message.append(throwable.getClass().getCanonicalName()).append(" : ");
                            message.append(throwable.getMessage());
                            exportResult.setMessage(message.toString());

                            throw new IllegalStateException(STORE_ERROR, throwable);
                        },
                        writer::flush
                );
        } catch (IOException e) {
            throw new IllegalStateException(STORE_ERROR, e);
        }

        return exportResult;
    }

    @Override
    public Observable<String> getResultsByID(String tableExportID) {
        log.debug("getTableExportResultsByID");

        return Observable.using(
                () -> getReader(tableExportID),
                reader -> Observable.fromIterable(() -> new Iterator<String>() {
                    private String record = null;

                    @Override
                    public boolean hasNext() {
                        try {
                            record = reader.readLine();
                            return record != null;
                        } catch (IOException e) {
                            throw new IllegalStateException(RETRIEVE_ERROR, e);
                        }
                    }

                    @Override
                    public String next() {
                        if (record != null) {
                            return record;
                        }
                        throw new IllegalStateException("null line found.");
                    }
                }),
                BufferedReader::close);
    }

    private BufferedReader getReader(String tableExportID) {
        try {
            return Files.newBufferedReader(Paths.get(basePath + File.separator + tableExportID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException(RETRIEVE_ERROR, e);
        }
    }

    private BufferedWriter getWriter(String tableExportID, String extension) {
        try {
            return Files.newBufferedWriter(Paths.get(basePath + File.separator + tableExportID + extension));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException(STORE_ERROR, e);
        }
    }

    @Override
    public boolean isExtensionEnabled() {
        return this.enableExtension;
    }
}
