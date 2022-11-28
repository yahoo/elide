/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import jakarta.persistence.Entity;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Include(rootLevel = false)
public class CoerceBean {
    public String string;
    public List<Boolean> list;
    public Map<String, Long> map;
    public Set<Double> set;
}
