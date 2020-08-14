/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.jsonapi.models;

import com.yahoo.elide.jsonapi.serialization.SingletonSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.reactivex.Observable;
import io.reactivex.Observer;

/**
 * Single object treated as an Observable.
 *
 * @param <T>  the type of element to treat as an Observable.
 */
@JsonSerialize(using = SingletonSerializer.class)
public class SingleElementObservable<T> extends Observable<T> {

    private final T value;

    public SingleElementObservable(T v) {
        value = v;
    }
    public T getValue() {
        return value;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        if (value != null) {
            observer.onNext(value);
        }
        observer.onComplete();
    }
}
