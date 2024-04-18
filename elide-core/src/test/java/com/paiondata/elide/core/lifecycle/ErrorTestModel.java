/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import static com.paiondata.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.paiondata.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;

import com.paiondata.elide.annotation.Include;
import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.exceptions.BadRequestException;
import com.paiondata.elide.core.security.ChangeSpec;

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

    public static class ErrorHook implements LifeCycleHook<ErrorTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            ErrorTestModel elideEntity,
                            com.paiondata.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            throw new BadRequestException("Invalid");
        }
    }
}
