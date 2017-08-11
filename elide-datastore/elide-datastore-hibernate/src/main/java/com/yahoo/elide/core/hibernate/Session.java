/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.hibernate;

import java.util.Collection;

/**
 * Interface that represents a Hibernate session but has no dependencies on a specific version of Hibernate.
 */
public interface Session {
    public Query createQuery(String queryText);
    public Query createFilter(Collection collection, String queryText);
}
