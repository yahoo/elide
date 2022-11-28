/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.OnCreatePostCommit;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.annotation.OnCreatePreSecurity;
import com.yahoo.elide.annotation.OnDeletePostCommit;
import com.yahoo.elide.annotation.OnDeletePreCommit;
import com.yahoo.elide.annotation.OnDeletePreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * Model used to mock different lifecycle test scenarios using legacy annotations.
 */
@Include(name = "legacyTestModel")
public class LegacyTestModel {

    @Id
    private String id;

    @Getter
    @Setter
    private String field;

    @Getter
    @Setter
    private String field2;

    @OnCreatePostCommit("field")
    public void fieldCreatePostCommit() {
    }

    @OnCreatePreCommit("field")
    public void fieldCreatePreCommit() {
    }

    @OnCreatePreSecurity("field")
    public void fieldCreatePreSecurity() {
    }

    @OnUpdatePostCommit("field")
    public void fieldUpdatePostCommit() {
    }

    @OnUpdatePreCommit("field")
    public void fieldUpdatePreCommit() {
    }

    @OnUpdatePreSecurity("field")
    public void fieldUpdatePreSecurity() {
    }

    @OnDeletePostCommit
    public void classDeletePostCommit() {
    }

    @OnDeletePreCommit
    public void classDeletePreCommit() {
    }

    @OnDeletePreSecurity
    public void classDeletePreSecurity() {
    }

    @OnCreatePostCommit
    public void classCreatePostCommit() {
    }

    @OnCreatePreCommit
    public void classCreatePreCommit() {
    }

    @OnCreatePreSecurity
    public void classCreatePreSecurity() {
    }

    @OnUpdatePostCommit
    public void classUpdatePostCommit() {
    }

    @OnUpdatePreCommit
    public void classUpdatePreCommit() {
    }

    @OnUpdatePreSecurity
    public void classUpdatePreSecurity() {
    }

    @OnCreatePreSecurity("*")
    public void classCreatePreCommitAllUpdates() {
    }

    @OnCreatePostCommit("field")
    @OnCreatePreCommit
    @OnCreatePreSecurity("field")
    @OnUpdatePostCommit("field")
    @OnUpdatePreCommit
    @OnUpdatePreSecurity("field")
    public void fieldMultiple() {
    }

    @OnCreatePostCommit
    @OnCreatePreCommit("field")
    @OnCreatePreSecurity
    @OnUpdatePostCommit
    @OnUpdatePreCommit("field")
    @OnUpdatePreSecurity
    @OnDeletePostCommit
    @OnDeletePreCommit
    @OnDeletePreSecurity
    public void classMultiple() {
    }
}
