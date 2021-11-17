/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import static com.yahoo.elide.modelconfig.store.ConfigDataStore.VALIDATE_ONLY_HEADER;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.modelconfig.io.FileLoader;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.yahoo.elide.modelconfig.validator.Validator;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Elide DataStoreTransaction which loads/persists HJSON configuration files as Elide models.
 */
public class ConfigDataStoreTransaction implements DataStoreTransaction {


    private final FileLoader fileLoader;
    private final Set<Runnable> todo;
    private final Set<ConfigFile> dirty;
    private final Validator validator;
    private final boolean readOnly;

    public ConfigDataStoreTransaction(
            FileLoader fileLoader,
            boolean readOnly,
            Validator validator
    ) {
        this.fileLoader = fileLoader;
        this.readOnly = readOnly || !fileLoader.isWriteable();
        this.dirty = new LinkedHashSet<>();
        this.todo = new LinkedHashSet<>();
        this.validator = validator;
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {
        if (readOnly) {
            throw new UnsupportedOperationException("Configuration is read only.");
        }

        ConfigFile file = (ConfigFile) entity;
        dirty.add(file);
        todo.add(() -> updateFile(file.getPath(), file.getContent()));
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        if (readOnly) {
            throw new UnsupportedOperationException("Configuration is read only.");
        }
        ConfigFile file = (ConfigFile) entity;
        dirty.add(file);
        todo.add(() -> deleteFile(file.getPath()));
    }

    @Override
    public void flush(RequestScope scope) {
        if (!readOnly) {
            Map<String, ConfigFile> resources;
            try {
                resources = fileLoader.loadResources();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            for (ConfigFile file : dirty) {
                resources.put(file.getPath(), file);
            }

            try {
                validator.validate(resources);
            } catch (Exception e) {
                throw new BadRequestException(e.getMessage());
            }
        }
    }

    @Override
    public void commit(RequestScope scope) {
        boolean validateOnly = scope.getRequestHeaderByName(VALIDATE_ONLY_HEADER) != null;

        if (! validateOnly) {
            for (Runnable runnable : todo) {
                runnable.run();
            }
        }
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        if (readOnly) {
            throw new UnsupportedOperationException("Configuration is read only.");
        }
        ConfigFile file = (ConfigFile) entity;
        dirty.add(file);
        todo.add(() -> {
            createFile(file.getPath());
            updateFile(file.getPath(), file.getContent());
        });
    }

    @Override
    public <T> T loadObject(EntityProjection entityProjection, Serializable id, RequestScope scope) {
        String idString = id.toString();
        int hyphenIndex = idString.lastIndexOf('-');

        String path;
        if (hyphenIndex < 0) {
            path = idString;
        } else {
            path = idString.substring(idString.lastIndexOf('-'));
        }
        try {
            return (T) fileLoader.loadResource(path);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            Map<String, ConfigFile> resources = fileLoader.loadResources();

            return new DataStoreIterableBuilder(resources.values()).allInMemory().build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        todo.clear();
        dirty.clear();
    }

    @Override
    public void close() throws IOException {
        //NOOP
    }

    private void deleteFile(String path) {
        Path deletePath = Path.of(fileLoader.getRootPath(), path);
        File file = deletePath.toFile();

        if (! file.exists()) {
            return;
        }

        if (! file.delete()) {
            throw new IllegalStateException("Unable to delete: " + path);
        }
    }

    private void updateFile(String path, String content) {
        Path updatePath = Path.of(fileLoader.getRootPath(), path);
        File file = updatePath.toFile();

        try {
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    private void createFile(String path) {
        Path createPath = Path.of(fileLoader.getRootPath(), path);
        File file = createPath.toFile();

        if (file.exists()) {
            throw new IllegalStateException("File already exists: " + path);
        }

        try {
            File parentDirectory = file.getParentFile();
            Files.createDirectories(Path.of(parentDirectory.getPath()));

            boolean created = file.createNewFile();
            if (!created) {
                throw new IllegalStateException("Unable to create file: " + path);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
