package com.yahoo.elide.async.models;

import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.RequestScope;

import lombok.extern.slf4j.Slf4j;

/**
 * Model for Async Query
 */
@Entity
@Include(type = "query", rootLevel = true)
@ReadPermission(expression = "Principal is Owner")
@UpdatePermission(expression = "Prefab.Role.None")
@Slf4j
public class AsyncQuery implements PrincipalOwned {
    @Id
    UUID id; //Can be generated or provided.

    //Extracted from the Principal object
    String principalName;

    String query;  //JSON-API PATH or GraphQL payload.

    QueryType queryType; //GRAPHQL, JSONAPI

    @UpdatePermission(expression = "Principal is Owner AND value is Cancelled")
    QueryStatus status;

    @OneToOne
    AsyncQueryResult result;

    Date createdOn;
    Date updatedOn;

    @Override
	public String getPrincipalName() {
		return principalName;
	}

    @PrePersist
    public void prePersist() {
        createdOn = updatedOn = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = new Date();
    }

    public void setResult(AsyncQueryResult result) {
        this.result = result;
    }

    public Date getCreatedOn() {
        return this.createdOn;
    }

    public QueryStatus getQueryStatus() {
        return status;
    }

    public void setQueryStatus(QueryStatus status) {
        this.status = status;
    }

}
