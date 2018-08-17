/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.functions.LifeCycleHook;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * RX Java Observer which invokes a lifecycle hook function.
 */
public class LifecycleHookInvoker implements Observer<CRUDEvent> {

    private EntityDictionary dictionary;
    private Class<? extends Annotation> annotation;

    public LifecycleHookInvoker(EntityDictionary dictionary, Class<? extends Annotation> annotation) {
        this.dictionary = dictionary;
        this.annotation = annotation;
    }

    @Override
    public void onSubscribe(Disposable disposable) { }

    @Override
    public void onNext(CRUDEvent event) {

        Collection<LifeCycleHook> hooks = dictionary.getTriggers(
                event.getResource().getResourceClass(),
                this.annotation,
                event.getFieldName());

        hooks.forEach((hook) -> {
            hook.execute(
                    event.getResource().getObject(),
                    event.getResource().getRequestScope(),
                    event.getChanges());
        });
    }

    @Override
    public void onError(Throwable throwable) { }

    @Override
    public void onComplete() { }
}
