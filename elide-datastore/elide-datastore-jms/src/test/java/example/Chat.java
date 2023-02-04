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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Include(name = Chat.CHAT)

//This is a custom subscription
@Subscription(operations = {})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chat {

    public static final String CHAT = "chat";

    @Id
    long id;

    @SubscriptionField
    String message;
}
