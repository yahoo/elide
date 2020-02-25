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
    UUID id; //Matches UUID in query.

    Integer contentLength;

    String responseBody; //success or errors

    Integer status; // HTTP Status

    Date createdOn;
    Date updatedOn;

    @OneToOne
    AsyncQuery query;

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
}