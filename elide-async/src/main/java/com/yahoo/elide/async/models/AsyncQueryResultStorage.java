/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.async.models;

import com.yahoo.elide.annotation.Exclude;
import lombok.Data;

import java.sql.Blob;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@Exclude //We don't want this to be exposed through the standard Elide APIs.
@Entity
@Data
public class AsyncQueryResultStorage {
    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id; //Matches UUID in query.

    private Blob result; // To allow expansion to XLSX?
}
