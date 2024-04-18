/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.core.security.checks.prefab;

import com.paiondata.elide.core.security.ChangeSpec;
import com.paiondata.elide.core.security.RequestScope;
import com.paiondata.elide.core.security.checks.OperationCheck;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Checks to ensure that collections are only modified in a prescribed manner.
 */
public class Collections {

    private static Predicate<? super ChangeSpec> changeSpecIsCollection = c -> c.getModified() instanceof Collection;

    // Suppresses default constructor, ensuring non-instantiability.
    private Collections() {
        throw new UnsupportedOperationException();
    }

    /**
     * Use changeSpec to enforce that values were exclusively added to the collection.
     *
     * @param <T> type collection to be validated
     */
    public static class AppendOnly<T> extends OperationCheck<T> {

        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec
                    .filter(changeSpecIsCollection)
                    .filter(c -> collectionIsSuperset(c::getOriginal, c::getModified))
                    .isPresent();
        }
    }

    /**
     * Use changeSpec to enforce that values were exclusively removed from the collection.
     *
     * @param <T> type parameter
     */
    public static class RemoveOnly<T> extends OperationCheck<T> {

        @Override
        public boolean ok(T record, RequestScope requestScope, Optional<ChangeSpec> changeSpec) {
            return changeSpec
                    .filter(changeSpecIsCollection)
                    .filter(c -> collectionIsSuperset(c::getModified, c::getOriginal))
                    .isPresent();
        }

    }

    private static boolean collectionIsSuperset(Supplier<?> base, Supplier<?> potential) {
        Collection<?> baseCollection = (Collection<?>) base.get();
        Collection<?> potentialSuperset = (Collection<?>) potential.get();
        return potentialSuperset.size() >= baseCollection.size()
                && potentialSuperset.containsAll(baseCollection);
    }
}
