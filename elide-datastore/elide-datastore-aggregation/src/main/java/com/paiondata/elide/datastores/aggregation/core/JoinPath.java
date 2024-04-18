/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.datastores.aggregation.core;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.exceptions.InvalidValueException;
import com.paiondata.elide.core.request.Argument;
import com.paiondata.elide.core.type.ClassType;
import com.paiondata.elide.core.type.Type;
import com.paiondata.elide.datastores.aggregation.annotation.Join;
import com.paiondata.elide.datastores.aggregation.metadata.MetaDataStore;
import com.paiondata.elide.datastores.aggregation.queryengines.sql.metadata.SQLTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JoinPath extends {@link Path} to allow navigation through {@link Join} annotation.
 */
public class JoinPath extends Path {
    private MetaDataStore store;

    public JoinPath(List<PathElement> pathElements, MetaDataStore store) {
        super(pathElements);
        this.store = store;
    }

    public JoinPath(Type<?> entityClass, MetaDataStore store, String dotSeparatedPath) {
        this.store = store;
        pathElements = resolvePathElements(entityClass, store.getMetadataDictionary(), dotSeparatedPath);
    }

    @Override
    protected boolean needNavigation(Type<?> entityClass, String fieldName, EntityDictionary dictionary) {
        return dictionary.isRelation(entityClass, fieldName)
                || SQLTable.isTableJoin(store, entityClass, fieldName);
    }

    /**
     * Extend this path with a extension dot separated path.
     *
     * @param extensionPath extension path append to this join path
     * @return expended join path e.g. <code>[A.B]/[B.C] + C.D = [A.B]/[B.C]/[C.D]</code>
     */
    public JoinPath extend(String extensionPath) {
        return extendJoinPath(this, new JoinPath(lastElement().get().getType(), store, extensionPath), store);
    }

    /**
     * Append an extension path to an original path, the last element of original path should be the same as the
     * first element of extension path.
     *
     * @param path original path, e.g. <code>[A.B]/[B.C]</code>
     * @param extension extension path, e.g. <code>[B.C]/[C.D]</code>
     * @param <P> path extension
     * @return extended path <code>[A.B]/[B.C]/[C.D]</code>
     */
    private static <P extends Path> JoinPath extendJoinPath(Path path, P extension, MetaDataStore store) {
        List<Path.PathElement> toExtend = new ArrayList<>(path.getPathElements());
        toExtend.remove(toExtend.size() - 1);
        toExtend.addAll(extension.getPathElements());
        return new JoinPath(toExtend, store);
    }

    @Override
    protected PathElement resolvePathAttribute(Type<?> entityClass,
                                               String fieldName,
                                               String alias,
                                               Set<Argument> arguments,
                                               EntityDictionary dictionary) {
        Type<?> attributeClass = ClassType.OBJECT_TYPE;
        if (dictionary.isAttribute(entityClass, fieldName)
                        || fieldName.equals(dictionary.getIdFieldName(entityClass))) {
            attributeClass = dictionary.getType(entityClass, fieldName);
            return new PathElement(entityClass, attributeClass, fieldName, alias, arguments);
        }
        // Physical Column Reference starts with $
        if (fieldName.indexOf('$') == 0) {
            return new PathElement(entityClass, attributeClass, fieldName, alias, arguments);
        }

        String entityAlias = dictionary.getJsonAliasFor(entityClass);
        throw new InvalidValueException(entityAlias + " does not contain the field " + fieldName);
    }
}
