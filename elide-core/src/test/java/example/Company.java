/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Include
@Data
public class Company {

    @Id
    private String id;
    private String description;

    private Address address;
}
