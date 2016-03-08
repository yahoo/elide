/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog;

import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.SharePermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.security.checks.prefab.Role;
import example.blog.security.IsActiveUser;
import example.blog.security.IsOwner;
import example.blog.security.UserAssociatedRecord;
import example.blog.security.UserType;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
@Include(rootLevel = true)
@ReadPermission(any = { IsActiveUser.class })
@UpdatePermission(any = { IsOwner.class })
@DeletePermission(any = { Role.NONE.class })
@SharePermission(any = { Role.ALL.class })
public class Blogger extends BlogObject implements UserAssociatedRecord {
    private String userName;
    private boolean active = true;
    private UserType userType = UserType.Registered;
    private Set<Post> posts = new HashSet<>();

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @UpdatePermission(any = { Role.NONE.class })
    public UserType getUserType() {
        return userType;
    }
    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    @OneToMany(mappedBy = "author")
    public Set<Post> getPosts() {
        return posts;
    }
    public void setPosts(Set<Post> posts) {
        this.posts = posts;
    }

    @ReadPermission(any = { IsOwner.class })
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public Blogger getUser() {
        return this;
    }
}
