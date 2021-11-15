/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.io;

import com.yahoo.elide.modelconfig.DynamicConfigHelpers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Responsible for loading HJSON configuration either from the classpath or from the file system.
 */
public class FileLoader {
    private static final Pattern FILTER_VARIABLE_PATTERN = Pattern.compile(".*?\\{\\{(\\w+)\\}\\}");
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String RESOURCES = "resources";
    private static final int RESOURCES_LENGTH = 9; //"resources".length()

    private static final String HJSON_EXTN = "**/*.hjson";

    private final PathMatchingResourcePatternResolver resolver;

    @Getter
    private final String rootPath;

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
            this.rootPath = pattern;
        } else {
            File config = new File(rootPath);
            if (!config.exists()) {
                throw new IllegalStateException(rootPath + " : config path does not exist");
            }
            this.rootPath = FILEPATH_PATTERN + DynamicConfigHelpers.formatFilePath(config.getAbsolutePath());
        }
    }

    /**
     * Load resources from the filesystem/classpath.
     * @return A map from the path to the resource.
     * @throws IOException If something goes boom.
     */
    public Map<String, Resource> loadResources() throws IOException {
        Map<String, Resource> resourceMap = new HashMap<>();
        int configDirURILength = resolver.getResources(this.rootPath)[0].getURI().toString().length();

        Resource[] hjsonResources = resolver.getResources(this.rootPath + HJSON_EXTN);
        for (Resource resource : hjsonResources) {
            resourceMap.put(resource.getURI().toString().substring(configDirURILength), resource);
        }

        return resourceMap;
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
}
