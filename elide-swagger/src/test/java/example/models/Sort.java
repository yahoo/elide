/*
 * Copyright 2025, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models;


import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Include(friendlyName = "Sort")
public class Sort {
    public static enum Enumeration {
        UP,
        DOWN
    }

    public Enumeration enumeration;
    public Date date;
    public LocalDateTime localDateTime;
    public BigDecimal bigDecimal;
}
