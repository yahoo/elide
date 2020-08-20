/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import com.yahoo.elide.annotation.Include;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Tests model for Issue 1461 (Missing GeneratedValue annotation)
 */
@Include(rootLevel = true, type = "incidents")
@Entity
public class Incident {
    @Id
    // This annotation is missing
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
