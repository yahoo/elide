/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Setter;

import java.util.Date;

@Entity
@Include
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
