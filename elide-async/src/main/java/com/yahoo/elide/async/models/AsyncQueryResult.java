/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.async.models;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

/**
 * Model for Async Query Result
 */
@Entity
@Include(type="queryResult")
@ReadPermission(expression = "Principal is Owner")
@UpdatePermission(expression = "Prefab.Role.None")
@CreatePermission(expression = "Prefab.Role.None")
public class AsyncQueryResult implements PrincipalOwned {
    @Id
    private UUID id; //Matches UUID in query.

    private Integer contentLength;

    private String responseBody; //success or errors

    private Integer status; // HTTP Status

    private Date createdOn;

    private Date updatedOn;

    @OneToOne
    private AsyncQuery query;

    @Exclude
    public String getPrincipalName() {
        return query.getPrincipalName();
    }

    @PrePersist
    public void prePersist() {
        createdOn = updatedOn = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = new Date();
    }

    public void setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setQuery(AsyncQuery query) {
        this.query = query;
    }

    public void setId(UUID id) {
        this.id = id;
    }

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}

	public UUID getId() {
		return id;
	}

	public Integer getContentLength() {
		return contentLength;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public Integer getStatus() {
		return status;
	}

	public AsyncQuery getQuery() {
		return query;
	}
}