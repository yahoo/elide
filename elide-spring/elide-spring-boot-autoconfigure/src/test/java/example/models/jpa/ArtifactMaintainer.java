/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.models.jpa;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Include(name = "maintainer")
@Entity
@Data
@Subscription
public class ArtifactMaintainer {
    @Id
    private String name = "";

    @SubscriptionField
    private String commonName = "";

    private String description = "";

    @ManyToMany
    private List<ArtifactProduct> products = new ArrayList<>();
}
