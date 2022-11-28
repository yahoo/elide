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

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ConfigDataStoreTransaction implements DataStoreTransaction {
    private final FileLoader fileLoader;
    private final Set<Runnable> todo;
    private final Set<ConfigFile> dirty;
    private final Set<String> deleted;
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
        this.deleted = new LinkedHashSet<>();
        this.todo = new LinkedHashSet<>();
        this.validator = validator;
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {
        ConfigFile file = (ConfigFile) entity;

        boolean canWrite;
        if (scope.isNewResource(file)) {
            canWrite = canCreate(file.getPath());
        } else {
            canWrite = canModify(file.getPath());
        }

        if (readOnly || !canWrite) {
            log.error("Attempt to modify a read only configuration");
            throw new UnsupportedOperationException("Configuration is read only.");
        }

        dirty.add(file);
        todo.add(() -> updateFile(file.getPath(), file.getContent()));
    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {
        ConfigFile file = (ConfigFile) entity;
        if (readOnly || !canModify(file.getPath())) {
            log.error("Attempt to modify a read only configuration");
            throw new UnsupportedOperationException("Configuration is read only.");
        }
        dirty.add(file);
        deleted.add(file.getPath());
        todo.add(() -> deleteFile(file.getPath()));
    }

    @Override
    public void flush(RequestScope scope) {
        if (!readOnly) {
            Map<String, ConfigFile> resources;
            try {
                resources = fileLoader.loadResources();
            } catch (IOException e) {
                log.error("Error reading configuration resources: {}", e.getMessage());
                throw new IllegalStateException(e);
            }

            for (ConfigFile file : dirty) {
                resources.put(file.getPath(), file);
            }

            for (String path: deleted) {
                resources.remove(path);
            }

            try {
                validator.validate(resources);
            } catch (Exception e) {
                log.error("Error validating configuration: {}", e.getMessage());
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
        ConfigFile file = (ConfigFile) entity;

        if (readOnly || !canCreate(file.getPath())) {
            log.error("Attempt to modify a read only configuration");
            throw new UnsupportedOperationException("Configuration is read only.");
        }
        dirty.add(file);
        todo.add(() -> {

            //We have to assign the ID here during commit so it gets sent back in the response.
            file.setId(ConfigFile.toId(file.getPath(), file.getVersion()));

            createFile(file.getPath());
            updateFile(file.getPath(), file.getContent());
        });
    }

    @Override
    public <T> T loadObject(EntityProjection entityProjection, Serializable id, RequestScope scope) {
        String path = ConfigFile.fromId(id.toString());

        try {
            return (T) fileLoader.loadResource(path);
        } catch (IOException e) {
            log.error("Error reading configuration resources for {} : {}", id, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            Map<String, ConfigFile> resources = fileLoader.loadResources();

            return new DataStoreIterableBuilder(resources.values()).allInMemory().build();
        } catch (IOException e) {
            log.error("Error reading configuration resources: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        todo.clear();
        dirty.clear();
        deleted.clear();
    }

    @Override
    public void close() throws IOException {
        //NOOP
    }

    private boolean canCreate(String filePath) {
        Path path = Path.of(fileLoader.getRootPath(), filePath);
        File directory = path.toFile().getParentFile();

        while (directory != null && !directory.exists()) {
            directory = directory.getParentFile();
        }

        return (directory != null && Files.isWritable(directory.toPath()));
    }

    private boolean canModify(String filePath) {
        Path path = Path.of(fileLoader.getRootPath(), filePath);
        File file = path.toFile();
        return !file.exists() || Files.isWritable(file.toPath());
    }

    private void deleteFile(String path) {
        Path deletePath = Path.of(fileLoader.getRootPath(), path);
        File file = deletePath.toFile();

        if (! file.exists()) {
            return;
        }

        if (! file.delete()) {
            log.error("Error deleting file: {}", file.getPath());
            throw new IllegalStateException("Unable to delete: " + path);
        }
    }

    private void updateFile(String path, String content) {
        Path updatePath = Path.of(fileLoader.getRootPath(), path);
        File file = updatePath.toFile();

        try {
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error updating file: {} with message: {}", file.getPath(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }
    private void createFile(String path) {
        Path createPath = Path.of(fileLoader.getRootPath(), path);
        File file = createPath.toFile();

        if (file.exists()) {
            log.debug("File already exits: {}", file.getPath());
            return;
        }

        try {
            File parentDirectory = file.getParentFile();
            Files.createDirectories(Path.of(parentDirectory.getPath()));

            boolean created = file.createNewFile();
            if (!created) {
                log.error("Unable to create file: {}", file.getPath());
                throw new IllegalStateException("Unable to create file: " + path);
            }
        } catch (IOException e) {
            log.error("Error creating file: {} with message: {}", file.getPath(), e.getMessage());
            throw new IllegalStateException(e);
        }
    }
}
