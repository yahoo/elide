/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity(name = "invoice")
@Include
@Data
public class Invoice {
    @Id
    private long id;
    @OneToMany(cascade = { CascadeType.ALL} , mappedBy = "invoice", targetEntity = LineItem.class)
    private Set<LineItem> items;
    private Map<String, BigDecimal> taxes;
    private Date creationDate;
}
