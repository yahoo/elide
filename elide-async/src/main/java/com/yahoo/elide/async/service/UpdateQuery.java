package com.yahoo.elide.async.service;

import com.yahoo.elide.async.models.AsyncQuery;

@FunctionalInterface
public interface UpdateQuery {
    public void update(AsyncQuery query);
}
