/*
 * Copyright 2018, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.jpa.transaction.checker.classes;

/**
 * All EclipseLink IndirectCollection subclasses.
 * Needed to exclude EclipseLink dependency.
 */
public class EclipseLinkPersistentCollections {
    public static final String[] CLASSES = {
            "org.eclipse.persistence.indirection.IndirectList",
            "org.eclipse.persistence.indirection.IndirectMap",
            "org.eclipse.persistence.indirection.IndirectSet"
    };
}
