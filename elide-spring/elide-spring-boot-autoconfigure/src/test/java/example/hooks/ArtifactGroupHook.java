/*
 * Copyright 2024, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.hooks;

import com.yahoo.elide.annotation.LifeCycleHookBinding.Operation;
import com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase;
import com.yahoo.elide.core.lifecycle.LifeCycleHook;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;

import example.models.jpa.ArtifactGroup;
import example.models.services.HookService;

import java.util.Optional;

/**
 * Test hook.
 */
public class ArtifactGroupHook implements LifeCycleHook<ArtifactGroup> {
    private final HookService hookService;

    /**
     * This requires a dependency injected service to test the injector.
     *
     * @param hookService the hook service
     */
    public ArtifactGroupHook(HookService hookService) {
        this.hookService = hookService;
    }

    @Override
    public void execute(Operation operation, TransactionPhase phase, ArtifactGroup elideEntity,
            RequestScope requestScope, Optional<ChangeSpec> changes) {
    }

    public HookService getService() {
        return this.hookService;
    }
}
