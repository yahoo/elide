/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.core.lifecycle;

import com.paiondata.elide.annotation.LifeCycleHookBinding;
import com.paiondata.elide.core.dictionary.EntityDictionary;

import java.util.ArrayList;

/**
 * RX Java Observer which invokes a lifecycle hook function.
 */
public class LifecycleHookInvoker {

    private EntityDictionary dictionary;
    private LifeCycleHookBinding.Operation op;
    private LifeCycleHookBinding.TransactionPhase phase;

    public LifecycleHookInvoker(EntityDictionary dictionary,
                                LifeCycleHookBinding.Operation op,
                                LifeCycleHookBinding.TransactionPhase phase) {
        this.dictionary = dictionary;
        this.op = op;
        this.phase = phase;
    }

    public void onNext(CRUDEvent event) {
        ArrayList<LifeCycleHook> hooks = new ArrayList<>();

        //Collect all the hooks that are keyed on a specific field.
        hooks.addAll(dictionary.getTriggers(event.getResource().getResourceType(), op, phase, event.getFieldName()));

        //Collect all the hooks that are keyed on any field.
        if (!event.getFieldName().isEmpty()) {
            hooks.addAll(dictionary.getTriggers(event.getResource().getResourceType(), op, phase));
        }

        //Invoke all the hooks
        hooks.forEach(hook ->
            hook.execute(this.op, this.phase, event)
        );
    }
}
