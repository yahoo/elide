/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.Paginate;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Include
@Paginate(maxPageSize = 10, defaultPageSize = 10)
public class EntityWithPaginateMaxLimit extends BaseId {
    @Getter
    @Setter
    private String name;
}
