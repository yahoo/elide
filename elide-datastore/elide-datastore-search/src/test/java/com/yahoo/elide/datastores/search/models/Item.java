/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search.models;

import com.yahoo.elide.annotation.Include;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include
@Indexed
public class Item {
    @Id
    private long id;

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private String name;

    @Field(index = Index.YES, analyze = Analyze.YES, store = Store.YES)
    private String description;
}
