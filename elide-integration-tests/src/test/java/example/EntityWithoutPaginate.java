/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity that does not have the Paginate annotation that modifies pagination behavior.
 */
@Entity
@Include
public class EntityWithoutPaginate extends BaseId {
    @Getter
    @Setter
    private String name;
}
