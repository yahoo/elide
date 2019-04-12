/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import lombok.Setter;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Include(rootLevel = true)
public class Generate {
    @Setter
    private long id;

    @Setter
    private Date created;

    @Id
    public long getId() {
        return id;
    }

    @Generated(GenerationTime.INSERT)
    @Column(updatable = false, insertable = false, columnDefinition = "timestamp default current_timestamp")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getCreated() {
        return created;
    }
}
