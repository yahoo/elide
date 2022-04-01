/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Include // optional here because class has this name
@Entity
@Subscription(operations = {}) //Denotes a custom subscription
public class Preview {
    @Id
    private UUID id;

    @ManyToOne
    @SubscriptionField
    private Book book;
}
