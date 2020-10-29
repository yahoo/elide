/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.TableExport;

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
 * It supports Async Module to store results with async query.
 */
@Singleton
@Slf4j
@Getter
public class FileResultStorageEngine implements ResultStorageEngine {
    @Setter private String basePath;

    public FileResultStorageEngine() {
    }

    /**
     * Constructor.
     * @param basePath basePath for storing the files. Can be absolute or relative.
     */
    public FileResultStorageEngine(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public TableExport storeResults(TableExport tableExport, Observable<String> result) {
        log.debug("store AsyncResults for Download");

        try (BufferedWriter writer = getWriter(tableExport.getId())) {
            result
                .map(record -> record.concat(System.getProperty("line.separator")))
                .subscribe(
                        recordCharArray -> {
                            writer.write(recordCharArray);
                            writer.flush();
                        },
                        throwable -> {
                            throw new IllegalStateException(throwable);
                        },
                        () -> {
                            writer.flush();
                        }
                );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return tableExport;
    }

    @Override
    public Observable<String> getResultsByID(String tableExportID) {
        log.debug("getAsyncResultsByID");

        return Observable.using(
                () -> getReader(tableExportID),
                reader -> {
                    return Observable.fromIterable(() -> {
                        return new Iterator<String>() {
                            private String record = null;

                            @Override
                            public boolean hasNext() {
                                try {
                                    record = reader.readLine();
                                    return record != null;
                                } catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            }

                            @Override
                            public String next() {
                                if (record != null) {
                                    return record;
                                }
                                throw new IllegalStateException("null line found.");
                            }
                        };
                    });
                }, BufferedReader::close);
    }

    private BufferedReader getReader(String tableExportID) {
        try {
            return Files.newBufferedReader(Paths.get(basePath + File.separator + tableExportID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException("Unable to retrieve results.");
        }
    }

    private BufferedWriter getWriter(String tableExportID) {
        try {
            return Files.newBufferedWriter(Paths.get(basePath + File.separator + tableExportID));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException("Unable to store results.");
        }
    }
}
