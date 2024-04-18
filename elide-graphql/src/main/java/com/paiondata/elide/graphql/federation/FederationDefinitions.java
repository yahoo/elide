/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.graphql.federation;

import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.Directive;
import graphql.language.StringValue;
import graphql.language.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Federation Definitions.
 */
public class FederationDefinitions {
    public static String namespace = "federation__";

    public static String shareableName = "shareable";

    public static String linkName = "link";

    public static String urlArgumentName = "url";

    public static String importArgumentName = "import";

    /**
     * Returns a link directive.
     *
     * @param url the url
     * @param imports the imports
     * @return the link directive
     */
    public static Directive link(String url, String... imports) {
        return Directive.newDirective().name(linkName).argument(urlArgument(url))
                .argument(importArgument(Arrays.stream(imports).toList())).build();
    }

    /**
     * Returns a url argument.
     *
     * @param url the url
     * @return the url argument
     */
    public static Argument urlArgument(String url) {
        return Argument.newArgument().name(urlArgumentName).value(StringValue.of(url)).build();
    }

    /**
     * Returns a import argument.
     *
     * @param imports the imports
     * @return the imports argument
     */
    public static Argument importArgument(List<String> imports) {
        List<Value> values = new ArrayList<>(imports.size());
        imports.stream().map(StringValue::of).forEach(values::add);
        return Argument.newArgument().name(importArgumentName).value(ArrayValue.newArrayValue().values(values).build())
                .build();
    }
}
