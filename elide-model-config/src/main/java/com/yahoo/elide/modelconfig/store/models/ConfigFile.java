/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store.models;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.function.Supplier;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Include(name = "config")
@Data
@NoArgsConstructor
@ReadPermission(expression = ConfigChecks.CAN_READ_CONFIG)
@UpdatePermission(expression = ConfigChecks.CAN_UPDATE_CONFIG)
@DeletePermission(expression = ConfigChecks.CAN_DELETE_CONFIG)
@CreatePermission(expression = ConfigChecks.CAN_CREATE_CONFIG)
public class ConfigFile {
    public enum ConfigFileType {
        NAMESPACE,
        TABLE,
        VARIABLE,
        DATABASE,
        SECURITY,
        UNKNOWN;
    }

    @Id
    @GeneratedValue
    private String id; //path-version

    private String path;

    private String version;

    @Exclude
    private Supplier<String> contentProvider;

    @Exclude
    private String content;

    @ComputedAttribute
    public String getContent() {
        if (content == null) {
            content = contentProvider.get();
        }

        return content;
    }

    private ConfigFileType type;

    @Builder
    public ConfigFile(
            String path,
            String version,
            ConfigFileType type,
            Supplier<String> contentProvider) {
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
        this.contentProvider = contentProvider;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigFile that = (ConfigFile) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
