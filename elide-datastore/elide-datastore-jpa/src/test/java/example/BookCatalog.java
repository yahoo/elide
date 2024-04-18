/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
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
