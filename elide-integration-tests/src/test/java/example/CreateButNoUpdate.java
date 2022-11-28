/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

/**
 * A model intended to be ONLY created and read, but never updated.
 */
@Include
@Entity
@CreatePermission(expression = "Prefab.Role.All")
@ReadPermission(expression = "Prefab.Role.All")
@UpdatePermission(expression = "Prefab.Role.None")
public class CreateButNoUpdate extends BaseId {
    private String textValue;

    private String cannotModify = "unmodified";

    private CreateButNoUpdate toOneRelation;

    @CreatePermission(expression = "Prefab.Role.None")
    public String getCannotModify() {
        return cannotModify;
    }

    public void setCannotModify(String cannotModify) {
        this.cannotModify = cannotModify;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public String getTextValue() {
        return textValue;
    }

    @OneToOne
    public CreateButNoUpdate getToOneRelation() {
        return toOneRelation;
    }

    public void setToOneRelation(CreateButNoUpdate toOneRelation) {
        this.toOneRelation = toOneRelation;
    }
}
