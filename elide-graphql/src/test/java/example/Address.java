/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Include(rootLevel = false)
@Entity
public class Address {
    @Getter @Setter
    private String street1;

    @Getter @Setter
    private String street2;

}
