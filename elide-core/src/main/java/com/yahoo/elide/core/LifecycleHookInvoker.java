/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core;

import com.yahoo.elide.functions.LifeCycleHook;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Optional;

/**
 * RX Java Observer which invokes a lifecycle hook function.
 */
@Slf4j
public class LifecycleHookInvoker implements Observer<CRUDEvent> {

    private EntityDictionary dictionary;
    private Class<? extends Annotation> annotation;
    private Optional<RuntimeException> exception;
    private boolean throwsExceptions;

    public LifecycleHookInvoker(EntityDictionary dictionary,
                                Class<? extends Annotation> annotation,
                                boolean throwExceptions) {
        this.dictionary = dictionary;
        this.annotation = annotation;
        this.exception = Optional.empty();
        this.throwsExceptions = throwExceptions;
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        //NOOP
    }

    @Override
    public void onNext(CRUDEvent event) {
        ArrayList<LifeCycleHook> hooks = new ArrayList<>();

        //Collect all the hooks that are keyed on a specific field.
        hooks.addAll(dictionary.getTriggers(
                event.getResource().getResourceClass(),
                this.annotation,
                event.getFieldName()));

        //Collect all the hooks that are keyed on any field.
        if (!event.getFieldName().isEmpty()) {
            hooks.addAll(dictionary.getTriggers(event.getResource().getResourceClass(), this.annotation));
        }

        try {
            //Invoke all the hooks
            hooks.forEach((hook) -> {
                    hook.execute(
                            event.getResource().getObject(),
                            event.getResource().getRequestScope(),
                            event.getChanges());

            });
        } catch (RuntimeException e) {
            exception = Optional.of(e);
            if (throwsExceptions) {
                throw e;
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        //NOOP
    }

    @Override
    public void onComplete() {
        //NOOP
    }

    public void throwOnError() {
        exception.ifPresent(e -> { throw e; });
    }
}
