/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.spring.hooks;

import com.yahoo.elide.annotation.Hook;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.spring.models.ArtifactGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Hook(lifeCycle = OnCreatePreCommit.class)
@Slf4j
public class CreationLoggerHook implements LifeCycleHook<ArtifactGroup> {
    @Override
    public void execute(ArtifactGroup group, RequestScope requestScope, Optional<ChangeSpec> changes) {
        log.debug("New Group: " + group);
    }
}
