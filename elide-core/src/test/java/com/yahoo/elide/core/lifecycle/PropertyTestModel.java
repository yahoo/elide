/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.lifecycle;

import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.CREATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.Operation.UPDATE;
import static com.yahoo.elide.annotation.LifeCycleHookBinding.TransactionPhase.POSTCOMMIT;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.security.ChangeSpec;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Model used to mock different lifecycle test scenarios.  This model uses properties instead of fields.
 */
@Include
public class PropertyTestModel {
    private String id;

    private Set<PropertyTestModel> models = new HashSet<>();

    static class RelationPostCommitHook implements LifeCycleHook<PropertyTestModel> {
        @Override
        public void execute(LifeCycleHookBinding.Operation operation,
                            LifeCycleHookBinding.TransactionPhase phase,
                            PropertyTestModel elideEntity,
                            com.yahoo.elide.core.security.RequestScope requestScope,
                            Optional<ChangeSpec> changes) {
            elideEntity.relationCallback(operation, POSTCOMMIT, changes.orElse(null));
        }
    }

    @Id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ManyToMany
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = CREATE, phase = POSTCOMMIT)
    @LifeCycleHookBinding(hook = PropertyTestModel.RelationPostCommitHook.class,
            operation = UPDATE, phase = POSTCOMMIT)
    public Set<PropertyTestModel> getModels() {
        return models;
    }

    public void setModels(Set<PropertyTestModel> models) {
        this.models = models;
    }

    public void relationCallback(LifeCycleHookBinding.Operation operation,
                                 LifeCycleHookBinding.TransactionPhase phase,
                                 ChangeSpec changes) {
        //NOOP - this will be mocked to verify hook invocation.
    }
}
