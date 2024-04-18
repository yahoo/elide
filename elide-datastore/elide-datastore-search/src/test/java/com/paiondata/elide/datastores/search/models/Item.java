/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.search.models;

import com.paiondata.elide.annotation.Include;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Include
@Indexed
@Data
public class Item {
    @Id
    private long id;

    @FullTextField(name = "name", searchable = Searchable.YES,
                    projectable = Projectable.NO, analyzer = "case_insensitive")
    @KeywordField(name = "sortName", sortable = Sortable.YES, projectable = Projectable.NO, searchable = Searchable.YES)
    private String name;

    @FullTextField(searchable = Searchable.YES, projectable = Projectable.NO, analyzer = "case_insensitive")
    private String description;

    @GenericField(searchable = Searchable.YES, projectable = Projectable.NO, sortable = Sortable.YES)
    private Date modifiedDate;

    private BigDecimal price;
}
