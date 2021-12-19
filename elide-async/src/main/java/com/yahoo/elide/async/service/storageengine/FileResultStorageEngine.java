/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.service.storageengine;

import com.yahoo.elide.async.models.TableExport;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import io.reactivex.Observable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
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

        try (BufferedWriter writer = getWriter(tableExport.getId(), tableExport.getResultType().toString())) {
            result
                .map(record -> record.concat(System.lineSeparator()))
                .subscribe(
                        recordCharArray -> {
                            writer.write(recordCharArray);
                            writer.flush();
                        },
                        throwable -> {
                            throw new IllegalStateException(STORE_ERROR, throwable);
                        },
                        writer::flush
                );
        } catch (IOException e) {
            throw new IllegalStateException(STORE_ERROR, e);
        }

        return tableExport;
    }

    @Override
    public Observable<String> getResultsByID(String asyncQueryID) {
        log.debug("getAsyncResultsByID");

        return Observable.using(
                () -> getReader(asyncQueryID),
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

    @Override
    public String getFileExtension(String asyncQueryID) {
        try {
            FileSystem fileSystem = FileSystems.getDefault();
            PathMatcher pathMatcher = fileSystem.getPathMatcher("glob:" + basePath + File.separator + asyncQueryID
                    + "*");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(basePath + File.separator))) {
                for (Path entry : stream) {
                    if (!Files.isDirectory(entry) && pathMatcher.matches(entry)) {
                        String fileExtension = FilenameUtils.getExtension(entry.toString());
                        return StringUtils.isEmpty(fileExtension) ? StringUtils.EMPTY : "." + fileExtension;
                    }
                }
            }
            return null;
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException(RETRIEVE_EXTENSION_ERROR, e);
        }
    }

    private BufferedReader getReader(String asyncQueryID) {
        try {
            String fileExtension = getFileExtension(asyncQueryID);
            return Files.newBufferedReader(Paths.get(basePath + File.separator + asyncQueryID + fileExtension));
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException(RETRIEVE_ERROR, e);
        }
    }

    private BufferedWriter getWriter(String asyncQueryID, String extension) {
        try {
            Path path = Paths.get(basePath + File.separator + asyncQueryID + "."
                    + extension.toLowerCase(Locale.ENGLISH));
            return Files.newBufferedWriter(path);
        } catch (IOException e) {
            log.debug(e.getMessage());
            throw new IllegalStateException(STORE_ERROR, e);
        }
    }
}
