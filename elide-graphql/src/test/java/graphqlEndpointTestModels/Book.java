/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package graphqlEndpointTestModels;

import com.yahoo.elide.annotation.Audit;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.graphql.GraphQLEndpointTest;
import com.yahoo.elide.security.RequestScope;

import graphqlEndpointTestModels.security.CommitChecks;
import graphqlEndpointTestModels.security.UserChecks;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

@Include(rootLevel = true)
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

    @OnUpdatePreSecurity(value = "title")
    public void titlePreSecurity(RequestScope scope) {
        GraphQLEndpointTest.User user = (GraphQLEndpointTest.User) scope.getUser().getOpaqueUser();
        user.appendLog("On Title Update Pre Security\n");
    }

    @OnUpdatePreCommit(value = "title")
    public void titlePreCommit(RequestScope scope) {
        GraphQLEndpointTest.User user = (GraphQLEndpointTest.User) scope.getUser().getOpaqueUser();
        user.appendLog("On Title Update Pre Commit\n");
    }

    @OnUpdatePostCommit(value = "title")
    public void titlePostCommit(RequestScope scope) {
        GraphQLEndpointTest.User user = (GraphQLEndpointTest.User) scope.getUser().getOpaqueUser();
        user.appendLog("On Title Update Post Commit\n");
    }
}
