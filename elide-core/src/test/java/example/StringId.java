/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;


@Include(rootLevel = true, type = "stringForId")
@Entity
public class StringId {

    private String surrogateKey;

    @Id
    public String getSurrogateKey() {
        return surrogateKey;
    }

    public void setSurrogateKey(String surrogateKey) {
        this.surrogateKey = surrogateKey;
    }
}
