/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.Paginate;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Include
@Paginate(defaultLimit = 5)
public class EntityWithPaginateDefaultLimit extends BaseId {
    @Getter
    @Setter
    private String name;
}
