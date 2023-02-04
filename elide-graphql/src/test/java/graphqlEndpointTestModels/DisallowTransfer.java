/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.NonTransferable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Include(rootLevel = false)
@Entity
@NonTransferable
public class DisallowTransfer {
    Long id;

    public void setId(Long id) {
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }
}
