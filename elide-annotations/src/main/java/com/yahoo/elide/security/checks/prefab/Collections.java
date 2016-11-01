/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.security.checks.prefab;

import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.security.checks.CommitCheck;

import java.util.Collection;
import java.util.Optional;

/**
 * Checks to ensure that collections are only modified in a prescribed manner.
 */
public class Collections {

    // Suppresses default constructor, ensuring non-instantiability.
    private Collections() {
    }

    /**
     * Use changeSpec to enforce that values were exclusively added to the collection.
     *
     * @param <T> type collection to be validated
     */
    public static class AppendOnly<T> extends CommitCheck<T> {

        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (!changeSpecIsCollection(changeSpec)) {
                return false;
            }

            Collection originalCollection = (Collection) changeSpec.get().getOriginal();
            Collection modifiedCollection = (Collection) changeSpec.get().getModified();

            return collectionIsSuperset(originalCollection, modifiedCollection);
        }
    }

    /**
     * Use changeSpec to enforce that values were exclusively removed from the collection.
     *
     * @param <T> type parameter
     */
    public static class RemoveOnly<T> extends CommitCheck<T> {

        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            if (!changeSpecIsCollection(changeSpec)) {
                return false;
            }

            Collection originalCollection = (Collection) changeSpec.get().getOriginal();
            Collection modifiedCollection = (Collection) changeSpec.get().getModified();

            return collectionIsSuperset(modifiedCollection, originalCollection);
        }

    }

    private static boolean collectionIsSuperset(Collection baseCollection, Collection potentialSuperset) {
        return (potentialSuperset.size() >= baseCollection.size())
                && (potentialSuperset.containsAll(baseCollection));
    }

    private static boolean changeSpecIsCollection(Optional<ChangeSpec> changeSpec) {
        return changeSpec.isPresent() && changeSpec.get().getModified() instanceof Collection;
    }
}
