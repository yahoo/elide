/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store.models;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.core.security.checks.prefab.Role.NONE_ROLE;

import com.yahoo.elide.annotation.ComputedAttribute;
import com.yahoo.elide.annotation.CreatePermission;
import com.yahoo.elide.annotation.DeletePermission;
import com.yahoo.elide.annotation.Exclude;
import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.ReadPermission;
import com.yahoo.elide.annotation.UpdatePermission;
import com.yahoo.elide.core.exceptions.BadRequestException;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents an HJSON configuration file for dynamic Elide models.
 */
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
    private String id; //Base64 encoded path-version

    @UpdatePermission(expression = NONE_ROLE)
    private String path;

    private String version;

    @Exclude
    private Supplier<String> contentProvider;

    @Exclude
    private String content;

    @ComputedAttribute
    public String getContent() {
        if (content == null) {
            if (contentProvider == null) {
                return null;
            }
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

        this.id = toId(path, version);

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
        return Objects.equals(path, that.path) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, version);
    }

    public static String toId(String path, String version) {
        String id;
        if (version == null || version.isEmpty()) {
            id = path;
        } else {
            id = path + "-" + version;
        }
        return Base64.getEncoder().encodeToString(id.getBytes());
    }

    public static String fromId(String id) {
        String idString;
        try {
            idString = URLDecoder.decode(id, "UTF-8");
            idString = new String(Base64.getDecoder().decode(idString.getBytes()));
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            throw new BadRequestException("Invalid ID: " + id);
        }

        int hyphenIndex = idString.lastIndexOf(".hjson-");

        String path;
        if (hyphenIndex < 0) {
            path = idString;
        } else {
            path = idString.substring(0, idString.lastIndexOf('-'));
        }

        return path;
    }
}
