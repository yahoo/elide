/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.io;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.yahoo.elide.modelconfig.DynamicConfigHelpers;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Responsible for loading HJSON configuration either from the classpath or from the file system.
 */
@Slf4j
public class FileLoader {
    private static final Pattern TABLE_FILE = Pattern.compile("models/tables/[^/]+\\.hjson");
    private static final Pattern NAME_SPACE_FILE = Pattern.compile("models/namespaces/[^/]+\\.hjson");
    private static final Pattern DB_FILE = Pattern.compile("db/sql/[^/]+\\.hjson");
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String RESOURCES = "resources";
    private static final int RESOURCES_LENGTH = 9; //"resources".length()

    private static final String HJSON_EXTN = "**/*.hjson";

    private final PathMatchingResourcePatternResolver resolver;

    private static final Function<Resource, String> CONTENT_PROVIDER = (resource) -> {
        try {
            return IOUtils.toString(resource.getInputStream(), UTF_8);
        } catch (IOException e) {
            log.error ("Error converting stream to String: {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    };

    @Getter
    private final String rootPath;
    private final String rootURL;

    @Getter
    private boolean writeable;

    /**
     * Constructor.
     * @param rootPath The file system (or classpath) root directory path for configuration.
     */
    public FileLoader(String rootPath) {
        this.resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());

        String pattern = CLASSPATH_PATTERN + DynamicConfigHelpers.formatFilePath(formatClassPath(rootPath));

        boolean classPathExists = false;
        try {
            classPathExists = (resolver.getResources(pattern).length != 0);
        } catch (IOException e) {
            //NOOP
        }

        if (classPathExists) {
            this.rootURL = pattern;
            writeable = false;
        } else {
            File config = new File(rootPath);
            if (!config.exists()) {
                log.error ("Config path does not exist: {}", config);
                throw new IllegalStateException(rootPath + " : config path does not exist");
            }

            writeable = Files.isWritable(config.toPath());
            this.rootURL = FILEPATH_PATTERN + DynamicConfigHelpers.formatFilePath(config.getAbsolutePath());
        }

        this.rootPath = rootPath;
    }

    /**
     * Load resources from the filesystem/classpath.
     * @return A map from the path to the resource.
     * @throws IOException If something goes boom.
     */
    public Map<String, ConfigFile> loadResources() throws IOException {
        Map<String, ConfigFile> resourceMap = new LinkedHashMap<>();
        int configDirURILength = resolver.getResources(this.rootURL)[0].getURI().toString().length();

        Resource[] hjsonResources = resolver.getResources(this.rootURL + HJSON_EXTN);
        for (Resource resource : hjsonResources) {
            if (! resource.exists()) {
                log.error("Missing resource during HJSON configuration load: {}", resource.getURI());
                continue;
            }
            String path = resource.getURI().toString().substring(configDirURILength);
            resourceMap.put(path, ConfigFile.builder()
                    .type(toType(path))
                    .contentProvider(() -> CONTENT_PROVIDER.apply(resource))
                    .path(path)
                    .version(NO_VERSION)
                    .build());
        }

        return resourceMap;
    }

    /**
     * Load a single resource from the filesystem/classpath.
     * @return The file content.
     * @throws IOException If something goes boom.
     */
    public ConfigFile loadResource(String relativePath) throws IOException {
        Resource[] hjsonResources = resolver.getResources(this.rootURL + relativePath);
        if (hjsonResources.length == 0 || ! hjsonResources[0].exists()) {
            return null;
        }

        return ConfigFile.builder()
                .type(toType(relativePath))
                .contentProvider(() -> CONTENT_PROVIDER.apply(hjsonResources[0]))
                .path(relativePath)
                .version(NO_VERSION)
                .build();
    }

    /**
     * Remove src/.../resources/ from class path for configs directory.
     * @param filePath class path for configs directory.
     * @return formatted class path for configs directory.
     */
    static String formatClassPath(String filePath) {
        if (filePath.indexOf(RESOURCES + "/") > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES + "/") + RESOURCES_LENGTH + 1);
        } else if (filePath.indexOf(RESOURCES) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES) + RESOURCES_LENGTH);
        }
        return filePath;
    }

    public static ConfigFile.ConfigFileType toType(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);
        if (lowerCasePath.endsWith("db/variables.hjson")) {
            return ConfigFile.ConfigFileType.VARIABLE;
        } else if (lowerCasePath.endsWith("models/variables.hjson")) {
            return ConfigFile.ConfigFileType.VARIABLE;
        } else if (lowerCasePath.equals("models/security.hjson")) {
            return ConfigFile.ConfigFileType.SECURITY;
        } else if (DB_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.DATABASE;
        } else if (TABLE_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.TABLE;
        } else if (NAME_SPACE_FILE.matcher(lowerCasePath).matches()) {
            return ConfigFile.ConfigFileType.NAMESPACE;
        } else {
            return ConfigFile.ConfigFileType.UNKNOWN;
        }
    }
}
