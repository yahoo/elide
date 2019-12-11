/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.standalone.hooks;

import com.yahoo.elide.annotation.Hook;
import com.yahoo.elide.annotation.OnCreatePreCommit;
import com.yahoo.elide.functions.LifeCycleHook;
import com.yahoo.elide.security.ChangeSpec;
import com.yahoo.elide.security.RequestScope;
import com.yahoo.elide.standalone.models.Post;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Hook(lifeCycle = OnCreatePreCommit.class)
@Slf4j
public class CreationLogHook implements LifeCycleHook<Post> {
    @Override
    public void execute(Post post, RequestScope requestScope, Optional<ChangeSpec> changes) {
        log.debug("New POST: " + post);
    }
}
