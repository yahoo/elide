/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import graphqlEndpointTestModels.security.CommitChecks;
import graphqlEndpointTestModels.security.UserChecks;
import hooks.BookUpdatePostCommitHook;
import hooks.BookUpdatePreCommitHook;
import hooks.BookUpdatePreSecurityHook;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Transient;
import lombok.Builder;

import java.util.HashSet;
import java.util.Set;

/**
 * Model for book.
 * <p>
 * <b>CAUTION: DO NOT DECORATE IT WITH {@link Builder}, which hides its no-args constructor. This will result in
 * runtime error at places such as {@code entityClass.newInstance();}</b>
 */
@Include
@Entity
@CreatePermission(expression = UserChecks.IS_USER_1)
@ReadPermission(expression = UserChecks.IS_USER_1 + " OR " + UserChecks.IS_USER_2 + " OR NOT "
        + CommitChecks.IS_NOT_USER_3)
@UpdatePermission(expression = CommitChecks.IS_NOT_USER_3 + " AND NOT " + UserChecks.IS_USER_2)
@DeletePermission(expression = UserChecks.IS_USER_1)
@Audit(action = Audit.Action.CREATE,
       operation = 10,
       logStatement = "{0}",
       logExpressions = {"${book.title}"})
public class Book {
    long id;
    String title;
    Set<Author> authors = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @LifeCycleHookBinding(hook = BookUpdatePreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = BookUpdatePreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = BookUpdatePostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ManyToMany
    public Set<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(Set<Author> authors) {
        this.authors = authors;
    }

    @Transient
    @ComputedAttribute
    @ReadPermission(expression = UserChecks.IS_USER_1)
    public String getUser1SecretField() {
        return "this is a secret for user 1 only1";
    }

    public void setUser1SecretField() {
        // Do nothing
    }
}
