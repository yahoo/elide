/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.jpa.v3;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.OffsetDateTime;

@Include(name = "group")
@Entity
@Data
@Subscription
@Table(name = "ArtifactGroup")
public class ArtifactGroupV3 {
    @Id
    private String name = "";

    @SubscriptionField
    @Column(name = "commonName")
    private String title = "";

    @SubscriptionField
    @Column(name = "createdOn", nullable = true)
    private OffsetDateTime createdOn;

    @SubscriptionField
    private boolean deprecated = false;
}
