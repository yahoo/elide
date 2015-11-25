/*
 * Copyright 2015, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package example;

import com.yahoo.elide.core.PersistentResource;
import com.yahoo.elide.security.Check;

/**
 * Useful for testing collection filter permissions.
 */
public class NegativeChildIdCheck implements Check<Child> {
    @Override
    public boolean ok(PersistentResource<Child> record) {
        Child child = record.getObject();
        return child.getId() >= 0;
    }
}
