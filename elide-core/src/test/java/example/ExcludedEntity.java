/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Exclude;
import lombok.AllArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Exclude
@AllArgsConstructor
public class ExcludedEntity {
    @Id public long id;
    public String name;
}
