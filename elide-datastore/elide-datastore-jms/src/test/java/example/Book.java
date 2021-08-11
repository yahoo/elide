/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.Include;
import lombok.Data;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Include
@Data
public class Book {
    @Id
    private long id;

    private String title;

    @ManyToMany
    private Set<Author> authors;
}
