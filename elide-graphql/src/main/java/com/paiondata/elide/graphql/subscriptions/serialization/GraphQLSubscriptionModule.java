/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.subscriptions.serialization;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * GraphQL Subscription Module.
 */
public class GraphQLSubscriptionModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public GraphQLSubscriptionModule() {
        super("GraphQLSubscriptionModule", Version.unknownVersion());
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new SubscriptionBeanSerializerModifier());
    }
}
