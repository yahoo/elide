/*
 * Copyright 2017, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.example.beans;

import com.yahoo.elide.annotation.ComputedRelationship;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.example.hbase.beans.RedisActions;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Hibernate-managed user.
 */
@Include(rootLevel = true)
@Entity
@CreatePermission(expression = "allow all")
@ReadPermission(expression = "allow all")
@UpdatePermission(expression = "allow all")
@DeletePermission(expression = "allow all")
public class HibernateUser {
    private Long id;
    private String firstName;
    private Set<RedisActions> redisActions;
    private Integer specialActionId;
    private RedisActions specialAction;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @OneToMany
    @Transient
    @ComputedRelationship
    public Set<RedisActions> getRedisActions() {
        return redisActions;
    }

    public void setRedisActions(Set<RedisActions> redisActions) {
        this.redisActions = redisActions;
    }

    @Exclude // Hide this from elide: this is the internal pointer for finding this value in hbase
    public Integer getSpecialActionId() {
        return specialActionId;
    }

    public void setSpecialActionId(Integer specialActionId) {
        this.specialActionId = specialActionId;
    }

    @OneToOne
    @Transient
    @ComputedRelationship
    public RedisActions getSpecialAction() {
        return specialAction;
    }

    public void setSpecialAction(RedisActions specialAction) {
        this.specialAction = specialAction;
    }
}
