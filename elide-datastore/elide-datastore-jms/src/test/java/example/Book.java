/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example;

import static example.checks.InternalBookCheck.HIDDEN_BOOK;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.graphql.subscriptions.annotations.Subscription;
import com.yahoo.elide.graphql.subscriptions.annotations.SubscriptionField;
import lombok.Data;

import java.util.Set;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Include
@Data
@Subscription
@ReadPermission(expression = HIDDEN_BOOK)
public class Book {
    @Id
    private long id;

    @SubscriptionField
    private String title;

    @SubscriptionField
    @ManyToMany
    private Set<Author> authors;

    @SubscriptionField
    @ManyToOne
    private Publisher publisher;

    //Nobody can read this.
    @ReadPermission(expression = "NONE")
    @SubscriptionField
    private String nope;

    private String notSerialized;
}
