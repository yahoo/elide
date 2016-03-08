/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example.blog.security;


import example.blog.BlogObject;

import javax.persistence.OneToOne;

public interface VersionedRecord<T extends BlogObject> {
    @OneToOne
    public default T getNextRevision() {
        return null;
    }

    @OneToOne(mappedBy = "previousRevision")
    public default T getPreviousRevision() {
        return null;
    }
}
