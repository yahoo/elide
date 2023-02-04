/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.versioned;

import com.yahoo.elide.annotation.Include;
import example.models.BaseId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Include(name = "book")
@Entity
@Data
@Table(name = "book")
public class BookV2 extends BaseId {

    @Column(name = "title")
    private String name;

    private long publishDate = 0;
}
