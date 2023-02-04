/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;

import jakarta.persistence.Id;
import lombok.Data;

@Include
@Data
@Subscription(operations = Subscription.Operation.CREATE)
public class Author {
    @Id
    private long id;

    @SubscriptionField
    private String name;
}
