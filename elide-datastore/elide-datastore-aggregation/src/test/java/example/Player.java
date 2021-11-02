/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.datastores.aggregation.annotation.FriendlyName;
import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A root level entity for testing AggregationDataStore.
 */
@Entity
@Include
@Table(name = "players")
@TableMeta(
        isHidden = true
)
@Data
public class Player {

    @Id
    private long id;

    @FriendlyName
    private String name;
}
