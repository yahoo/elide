/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SharePermission;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@Include(rootLevel = true, type = "noShareBid")
@SharePermission(expression = "deny all")
public class NoShareBiDirectional {
    private Long id;
    private NoShareBiDirectional other;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @OneToOne(fetch = FetchType.LAZY)
    public NoShareBiDirectional getOther() {
        return other;
    }

    public void setOther(NoShareBiDirectional other) {
        this.other = other;
    }
}
