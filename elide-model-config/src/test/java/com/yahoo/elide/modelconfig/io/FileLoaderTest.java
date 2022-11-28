/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.io;

import static com.yahoo.elide.modelconfig.io.FileLoader.formatClassPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class FileLoaderTest {

    @Test
    public void testFormatClassPath() {
        assertEquals("anydir", formatClassPath("src/test/resources/anydir"));
        assertEquals("anydir/configs", formatClassPath("src/test/resources/anydir/configs"));
        assertEquals("src/test/resourc", formatClassPath("src/test/resourc"));
        assertEquals("", formatClassPath("src/test/resources/"));
        assertEquals("", formatClassPath("src/test/resources"));
        assertEquals("anydir/configs", formatClassPath("src/test/resourcesanydir/configs"));
    }
}
