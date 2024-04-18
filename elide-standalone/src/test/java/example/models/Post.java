/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;
import example.checks.AdminCheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.util.Date;

@Entity
@Include
@Data
@Subscription
public class Post {
    @Id
    private long id;

    @Column(nullable = false)
    @SubscriptionField
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    @SubscriptionField
    private Date date;

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    private boolean abusiveContent;
}
