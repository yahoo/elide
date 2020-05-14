/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.contrib.dynamicconfig.model;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;
import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;


import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;


/**
 * A root level entity for testing AggregationDataStore.
 */
@Include(rootLevel = true , type = "playerStats")
@Cardinality(size = CardinalitySize.LARGE)
@EqualsAndHashCode
@ToString
@Data
@Entity
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.None")
@DeletePermission(expression = "Prefab.Role.None")
public class PlayerStats {

    @Id
    private String name;

    @ReadPermission(expression = "Prefab.Role.All")
    private String countryId;

    @ReadPermission(expression = "Prefab.Role.All")
    private Date createdOn;

    @ReadPermission(expression = "Prefab.Role.All")
    private Long highScore;

}
