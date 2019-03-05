/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Include(rootLevel = true) // optional here because class has this name
@Entity
public class Preview {
    @Id
    private UUID id;

    @ManyToOne
    private Book book;
}
