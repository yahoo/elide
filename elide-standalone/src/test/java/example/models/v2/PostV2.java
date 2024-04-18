/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package example.models.v2;

import com.paiondata.elide.annotation.CreatePermission;
import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.UpdatePermission;
import com.paiondata.elide.graphql.subscriptions.annotations.Subscription;
import com.paiondata.elide.graphql.subscriptions.annotations.SubscriptionField;

import example.checks.AdminCheck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.util.Date;

@Entity
@Include(name = "post")
@Data
@Table(name = "Post")
@Subscription
public class PostV2 {
    @Id
    private long id;

    @Column(nullable = false, name = "content")
    @SubscriptionField
    private String text;

    @Temporal(TemporalType.TIMESTAMP)
    @SubscriptionField
    private Date date;

    @CreatePermission(expression = AdminCheck.USER_IS_ADMIN)
    @UpdatePermission(expression = AdminCheck.USER_IS_ADMIN)
    private boolean abusiveContent;
}
