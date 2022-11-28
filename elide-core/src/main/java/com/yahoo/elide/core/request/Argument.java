/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.core.request;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents an argument passed to an attribute.
 */
@Value
@Builder
public class Argument implements Serializable {
    private static final long serialVersionUID = 2913180218704512683L;

    // square brackets having non-empty argument name and  encoded agument value separated by ':'
    // eg: [grain:month] , [foo:bar][blah:Encoded+Value]
    public static final Pattern ARGUMENTS_PATTERN = Pattern.compile("\\[(\\w+):([^\\]]+)\\]");

    @NonNull
    String name;

    Object value;

    /**
     * Returns the argument type.
     * @return the argument type.
     */
    public Class<?> getType() {
        return value.getClass();
    }

    /**
     * Parses input string and returns a set of {@link Argument}.
     *
     * @param argsString String to parse for arguments.
     * @return A Set of {@link Argument}.
     * @throws UnsupportedEncodingException
     */
    public static Set<Argument> getArgumentsFromString(String argsString) throws UnsupportedEncodingException {
        Set<Argument> arguments = new HashSet<>();

        if (!isEmpty(argsString)) {

            Matcher matcher = ARGUMENTS_PATTERN.matcher(argsString);
            while (matcher.find()) {
                arguments.add(Argument.builder()
                                .name(matcher.group(1))
                                .value(URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8.name()))
                                .build());
            }
        }

        return arguments;
    }

    /**
     * Converts Set of {@link Argument} into Map.
     * @param arguments Set of {@link Argument}.
     * @return a Map of {@link Argument}.
     */
    public static Map<String, Argument> getArgumentMapFromArgumentSet(Set<Argument> arguments) {
        return arguments.stream()
                        .collect(Collectors.toMap(Argument::getName, Function.identity()));
    }

    /**
     * Parses input string and returns a Map of {@link Argument}.
     *
     * @param argsString String to parse for arguments.
     * @return a Map of {@link Argument}.
     * @throws UnsupportedEncodingException
     */
    public static Map<String, Argument> getArgumentMapFromString(String argsString)
                    throws UnsupportedEncodingException {
        return getArgumentMapFromArgumentSet(getArgumentsFromString(argsString));
    }
}
