/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.test.graphql.elements;

/**
 * {@link Selection} represents the same concept as {@link graphql.language.Selection} but specializes in serialization,
 * in contrast to {@link graphql.language.Selection GraphQL Field Selection}, which is designed for deserialization.
 */
public abstract class Selection extends Definition {

    private static final long serialVersionUID = 2555015523902719386L;
}
