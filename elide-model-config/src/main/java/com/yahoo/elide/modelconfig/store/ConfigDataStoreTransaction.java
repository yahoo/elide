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
import com.yahoo.elide.core.request.Attribute;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.modelconfig.io.FileLoader;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Elide DataStoreTransaction which loads/persists HJSON configuration files as Elide models.
 */
public class ConfigDataStoreTransaction implements DataStoreTransaction {

    private final FileLoader fileLoader;
    private static final Pattern TABLE_FILE = Pattern.compile("models/tables/[^/]+\\.hjson");
    private static final Pattern NAME_SPACE_FILE = Pattern.compile("models/namespaces/[^/]+\\.hjson");
    private static final Pattern DB_FILE = Pattern.compile("db/sql/[^/]+\\.hjson");

    public ConfigDataStoreTransaction(FileLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    @Override
    public <T> void save(T entity, RequestScope scope) {

    }

    @Override
    public <T> void delete(T entity, RequestScope scope) {

    }

    @Override
    public void flush(RequestScope scope) {

    }

    @Override
    public void commit(RequestScope scope) {

    }

    @Override
    public void preCommit(RequestScope scope) {
        DataStoreTransaction.super.preCommit(scope);
    }

    @Override
    public <T> void createObject(T entity, RequestScope scope) {

    }

    @Override
    public <T> T createNewObject(Type<T> entityClass, RequestScope scope) {
        return DataStoreTransaction.super.createNewObject(entityClass, scope);
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
    public <T, R> DataStoreIterable<R> getToManyRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relationship,
            RequestScope scope
    ) {
        return DataStoreTransaction.super.getToManyRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public <T, R> R getToOneRelation(
            DataStoreTransaction relationTx,
            T entity,
            Relationship relationship,
            RequestScope scope
    ) {
        return DataStoreTransaction.super.getToOneRelation(relationTx, entity, relationship, scope);
    }

    @Override
    public <T, R> void updateToManyRelation(
            DataStoreTransaction relationTx,
            T entity,
            String relationName,
            Set<R> newRelationships,
            Set<R> deletedRelationships,
            RequestScope scope
    ) {
        DataStoreTransaction.super.updateToManyRelation(relationTx, entity, relationName,
                newRelationships, deletedRelationships, scope);
    }

    @Override
    public <T, R> void updateToOneRelation(
            DataStoreTransaction relationTx,
            T entity,
            String relationName,
            R relationshipValue,
            RequestScope scope
    ) {
        DataStoreTransaction.super.updateToOneRelation(relationTx, entity, relationName, relationshipValue, scope);
    }

    @Override
    public <T, R> R getAttribute(T entity, Attribute attribute, RequestScope scope) {
        return DataStoreTransaction.super.getAttribute(entity, attribute, scope);
    }

    @Override
    public <T> void setAttribute(T entity, Attribute attribute, RequestScope scope) {
        DataStoreTransaction.super.setAttribute(entity, attribute, scope);
    }

    @Override
    public void cancel(RequestScope scope) {

    }

    @Override
    public <T> T getProperty(String propertyName) {
        return DataStoreTransaction.super.getProperty(propertyName);
    }

    @Override
    public void close() throws IOException {

    }

    private ConfigFile.ConfigFileType toType(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);
        if (lowerCasePath.endsWith("variables.hjson")) {
            return ConfigFile.ConfigFileType.VARIABLE;
        } else if (lowerCasePath.endsWith("security.hjson")) {
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
}
