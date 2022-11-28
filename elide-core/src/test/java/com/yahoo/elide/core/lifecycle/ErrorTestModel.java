/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.security.ChangeSpec;

import jakarta.persistence.Id;

import java.util.Optional;

/**
 * Tests life cycle hooks which raise errors.
 */
@Include(name = "errorTestModel")
@LifeCycleHookBinding(hook = ErrorTestModel.ErrorHook.class, operation = CREATE, phase = PRECOMMIT)
public class ErrorTestModel {

    @Id
    private String id;

    private String field;

    static class ErrorHook implements LifeCycleHook<ErrorTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            ErrorTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            throw new BadRequestException("Invalid");
        }
    }
}
