/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.DELETE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRECOMMIT;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PREFLUSH;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.PRESECURITY;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.security.ChangeSpec;

import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Tests the invocation &amp; sequencing of DataStoreTransaction method invocations and life cycle events.
 * Model used to mock different lifecycle test scenarios.  This model uses fields instead of properties.
 */
@Include(name = "testModel")
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = CREATE, phase = PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = DELETE, phase = PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHookEverything.class, operation = CREATE,
        phase = PRECOMMIT, oncePerRequest = false)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreFlushHook.class, operation = UPDATE, phase = PREFLUSH)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
@LifeCycleHookBinding(hook = FieldTestModel.ClassPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)

@EqualsAndHashCode
public class FieldTestModel {

    @Id
    private String id;

    @Getter
    @Setter
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = CREATE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = DELETE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreFlushHook.class, operation = UPDATE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.AttributePostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    private String field;

    @Getter
    @Setter
    @OneToMany
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = CREATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = CREATE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = CREATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = DELETE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = DELETE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = DELETE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = DELETE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreSecurityHook.class, operation = UPDATE, phase = PRESECURITY)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreFlushHook.class, operation = UPDATE, phase = PREFLUSH)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPreCommitHook.class, operation = UPDATE, phase = PRECOMMIT)
    @LifeCycleHookBinding(hook = FieldTestModel.RelationPostCommitHook.class, operation = UPDATE, phase = POSTCOMMIT)
    private Set<FieldTestModel> models = new HashSet<>();

    static class ClassPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, PRESECURITY);
        }
    }

    static class ClassPreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, PREFLUSH);
        }
    }

    static class ClassPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, PRECOMMIT);
        }
    }

    static class ClassPreCommitHookEverything implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classAllFieldsCallback(operation, PRECOMMIT);
        }
    }

    static class ClassPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.classCallback(operation, POSTCOMMIT);
        }
    }

    static class AttributePreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, PRESECURITY, changes.orElse(null));
        }
    }

    static class AttributePreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, PREFLUSH, changes.orElse(null));
        }
    }

    static class AttributePreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, PRECOMMIT, changes.orElse(null));
        }
    }

    static class AttributePostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.attributeCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

    static class RelationPreSecurityHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, PRESECURITY, changes.orElse(null));
        }
    }

    static class RelationPreFlushHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, PREFLUSH, changes.orElse(null));
        }
    }

    static class RelationPreCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, PRECOMMIT, changes.orElse(null));
        }
    }

    static class RelationPostCommitHook implements LifeCycleHook<FieldTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            FieldTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

    public void classCallback(LifeCycleHookBinding.Operation operation,
                              LifeCycleHookBinding.TransactionPhase phase) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void attributeCallback(LifeCycleHookBinding.Operation operation,
                                  LifeCycleHookBinding.TransactionPhase phase,
                                  ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void relationCallback(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }

    public void classAllFieldsCallback(LifeCycleHookBinding.Operation operation,
                                       LifeCycleHookBinding.TransactionPhase phase) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}
