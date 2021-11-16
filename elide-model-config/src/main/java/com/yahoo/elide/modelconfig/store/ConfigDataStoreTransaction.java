/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.datastore.DataStoreIterableBuilder;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Elide DataStoreTransaction which loads/persists HJSON configuration files as Elide models.
 */
public class ConfigDataStoreTransaction implements DataStoreTransaction {

    private static final Pattern TABLE_FILE = Pattern.compile("models/tables/[^/]+\\.hjson");
    private static final Pattern NAME_SPACE_FILE = Pattern.compile("models/namespaces/[^/]+\\.hjson");
    private static final Pattern DB_FILE = Pattern.compile("db/sql/[^/]+\\.hjson");

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
        todo.add(() -> upsertFile(file.getPath(), file.getContent()));
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
            Map<String, String> resources;
            try {
                resources = fileLoader.loadResources();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            for (ConfigFile file : dirty) {
                resources.put(file.getPath(), file.getContent());
            }

            validator.validate(resources);
        }
    }

    @Override
    public void commit(RequestScope scope) {
        for (Runnable runnable : todo) {
            runnable.run();
        }
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {
        if (readOnly) {
            throw new UnsupportedOperationException("Configuration is read only.");
        }
        ConfigFile file = (ConfigFile) entity;
        dirty.add(file);
        todo.add(() -> upsertFile(file.getPath(), file.getContent()));
    }

    @Override
    public <T> T loadObject(EntityProjection entityProjection, Serializable id, RequestScope scope) {
        return null;
    }

    @Override
    public <T> DataStoreIterable<T> loadObjects(EntityProjection entityProjection, RequestScope scope) {
        try {
            Map<String, String> resources = fileLoader.loadResources();

            List<T> configFiles = new ArrayList<>();
            resources.forEach((path, content) -> {
                configFiles.add((T) ConfigFile.builder()
                        .content(content)
                        .path(path)
                        .version(NO_VERSION)
                        .type(toType(path))
                        .build());
            });

            return new DataStoreIterableBuilder<T>(configFiles)
                    .allInMemory()
                    .build();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void cancel(RequestScope scope) {
        //NOOP
    }

    @Override
    public void close() throws IOException {
        //NOOP
    }

    private ConfigFile.ConfigFileType toType(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);
        if (lowerCasePath.endsWith("db/variables.hjson")) {
            return ConfigFile.ConfigFileType.VARIABLE;
        } else if (lowerCasePath.endsWith("models/variables.hjson")) {
            return ConfigFile.ConfigFileType.VARIABLE;
        } else if (lowerCasePath.equals("models/security.hjson")) {
            return ConfigFile.ConfigFileType.SECURITY;
        } else if (DB_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.DATABASE;
        } else if (TABLE_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.TABLE;
        } else if (NAME_SPACE_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.NAMESPACE;
        } else {
            return ConfigFile.ConfigFileType.UNKNOWN;
        }
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

    private void upsertFile(String path, String content) {
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
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
