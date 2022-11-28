/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.hook;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.datastores.jpa.transaction.AbstractJpaTransaction;
import example.BookCatalog;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.HashSet;
import java.util.Optional;

public class BookCatalogOnCreateHook implements LifeCycleHook<BookCatalog> {
    @Override
    public void execute(
            LifeCycleHookBinding.Operation operation,
            LifeCycleHookBinding.TransactionPhase phase,
            BookCatalog catalog,
            RequestScope requestScope,
            Optional<ChangeSpec> changes) {

        EntityManager entityManager =
                requestScope.getTransaction().getProperty(AbstractJpaTransaction.ENTITY_MANAGER_PROPERTY);

        /* Load all the books */
        Query query = entityManager.createQuery("SELECT book FROM Book book");
        catalog.setBooks(new HashSet<>(query.getResultList()));
    }
}
