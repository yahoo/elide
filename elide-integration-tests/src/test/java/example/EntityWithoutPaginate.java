/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

/**
 * Entity that does not have the Paginate annotation that modifies pagination behavior.
 */
@Entity
@Include(rootLevel = true)
public class EntityWithoutPaginate extends BaseId {
    @Getter
    @Setter
    private String name;
}
