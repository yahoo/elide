/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store.models;

import com.yahoo.elide.annotation.Include;
import nonapi.io.github.classgraph.json.Id;

@Include(name = "config")
public class ConfigFile {
    public enum ConfigFileType {
        NAMESPACE,
        TABLE,
        VARIABLE,
        DATABASE;
    }

    @Id
    private String id; //path-version

    private String path;

    private String version;

    private String content;

    private ConfigFileType type;
}
