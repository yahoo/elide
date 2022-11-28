/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import example.hook.BookCatalogOnCreateHook;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.Set;

@Include
@Entity
@Data
@LifeCycleHookBinding(
        operation = LifeCycleHookBinding.Operation.CREATE,
        phase = PREFLUSH,
        hook = BookCatalogOnCreateHook.class
)
public class BookCatalog {

    @Id
    private long id;

    @OneToMany
    private Set<Book> books;
}
