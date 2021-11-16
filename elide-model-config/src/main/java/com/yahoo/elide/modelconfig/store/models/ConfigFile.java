/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store.models;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.annotation.Include;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Include(name = "config")
@Data
@NoArgsConstructor
public class ConfigFile {
    public enum ConfigFileType {
        NAMESPACE,
        TABLE,
        VARIABLE,
        DATABASE,
        SECURITY,
        UNKNOWN;
    }

    @Builder
    public ConfigFile(String path, String version, String content, ConfigFileType type) {
        if (version == null || version.isEmpty()) {
            this.id = path;
        } else {
            this.id = path + "-" + version;
        }
        this.path = path;
        if (version == null) {
            this.version = NO_VERSION;
        } else {
            this.version = version;
        }
        this.content = content;
        this.type = type;
    }

    @Id
    @GeneratedValue
    private String id; //path-version

    private String path;

    private String version;

    private String content;

    private ConfigFileType type;
}
