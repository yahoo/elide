/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.graphql;

import com.yahoo.elide.annotation.LifeCycleHookBinding;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.lifecycle.CRUDEvent;
import com.yahoo.elide.core.lifecycle.LifecycleHookInvoker;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * GraphQL Listener for CRUD events.  Populates events into a Queue.  Dispatches them after operation completes.
 */
@Slf4j
public class LifeCycleHookObserver implements Observer<CRUDEvent> {
    private final ConcurrentLinkedQueue<CRUDEvent> events;
    private final EntityDictionary dictionary;
    private volatile Throwable error = null;
    private volatile Disposable disposable;

    public LifeCycleHookObserver(EntityDictionary dictionary) {
        this.dictionary = dictionary;
        this.events = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        this.disposable = disposable;
    }

    @Override
    public void onNext(CRUDEvent event) {
        events.add(event);
    }

    @Override
    public void onError(Throwable e) {
        error = e;
    }

    @Override
    public void onComplete() {
        //noop
    }

    public Iterable<CRUDEvent> getEvents() {
        return events;
    }

    public void processQueuedEvents() {
        Observable.fromIterable(events)
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(
                        dictionary,
                        LifeCycleHookBinding.Operation.CREATE,
                        LifeCycleHookBinding.TransactionPhase.PRESECURITY, false)
                ).throwOnError();

        Observable.fromIterable(events)
                .filter(CRUDEvent::isCreateEvent)
                .subscribeWith(new LifecycleHookInvoker(
                        dictionary,
                        LifeCycleHookBinding.Operation.CREATE,
                        LifeCycleHookBinding.TransactionPhase.PREFLUSH, false)
                ).throwOnError();

        Observable.fromIterable(events)
                .filter(CRUDEvent::isDeleteEvent)
                .subscribeWith(new LifecycleHookInvoker(
                        dictionary,
                        LifeCycleHookBinding.Operation.DELETE,
                        LifeCycleHookBinding.TransactionPhase.PREFLUSH, false)
                ).throwOnError();

        Observable.fromIterable(events)
                .filter(CRUDEvent::isUpdateEvent)
                .subscribeWith(new LifecycleHookInvoker(
                        dictionary,
                        LifeCycleHookBinding.Operation.UPDATE,
                        LifeCycleHookBinding.TransactionPhase.PREFLUSH, false)
                ).throwOnError();

        if (this.disposable != null) {
            disposable.dispose();
        }

        if (error != null) {
            if (error instanceof RuntimeException) {
                throw ((RuntimeException) error);
            }
            throw new IllegalStateException(error);
        }
    }
}
