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
import com.yahoo.elide.annotation.OnReadPostCommit;
import com.yahoo.elide.annotation.OnReadPreCommit;
import com.yahoo.elide.annotation.OnReadPreSecurity;
import com.yahoo.elide.annotation.OnUpdatePostCommit;
import com.yahoo.elide.annotation.OnUpdatePreCommit;
import com.yahoo.elide.annotation.OnUpdatePreSecurity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Id;

/**
 * Model used to mock different lifecycle test scenarios using legacy annotations.
 */
@Include(type = "legacyTestModel")
public class LegacyTestModel {

    @Id
    private String id;

    @Getter
    @Setter
    private String field;

    @Getter
    @Setter
    private String field2;

    @OnCreatePostCommit(value = "field")
    public void fieldCreatePostCommit() {
    }

    @OnCreatePreCommit(value = "field")
    public void fieldCreatePreCommit() {
    }

    @OnCreatePreSecurity(value = "field")
    public void fieldCreatePreSecurity() {
    }

    @OnUpdatePostCommit(value = "field")
    public void fieldUpdatePostCommit() {
    }

    @OnUpdatePreCommit(value = "field")
    public void fieldUpdatePreCommit() {
    }

    @OnUpdatePreSecurity(value = "field")
    public void fieldUpdatePreSecurity() {
    }

    @OnReadPostCommit(value = "field")
    public void fieldReadPostCommit() {
    }

    @OnReadPreCommit(value = "field")
    public void fieldReadPreCommit() {
    }

    @OnReadPreSecurity(value = "field")
    public void fieldReadPreSecurity() {
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

    @OnReadPostCommit
    public void classReadPostCommit() {
    }

    @OnReadPreCommit
    public void classReadPreCommit() {
    }

    @OnReadPreSecurity
    public void classReadPreSecurity() {
    }

    @OnCreatePreSecurity("*")
    public void classCreatePreCommitAllUpdates() {
    }
}
