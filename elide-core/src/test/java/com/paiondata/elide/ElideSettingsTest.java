/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.paiondata.elide.ElideSettings.ElideSettingsBuilder;
import com.paiondata.elide.Settings.SettingsBuilder;
import com.paiondata.elide.utils.HeaderProcessor;

import org.junit.jupiter.api.Test;

/**
 * Test for ElideSettings.
 */
class ElideSettingsTest {

    @Test
    void shouldBuildWithDefaultSerdes() {
        ElideSettings elideSettings = ElideSettings.builder().build();
        assertEquals(8, elideSettings.getSerdes().size());
    }

    @Test
    void mutate() {
        SettingsBuilder settingsBuilder = mock(SettingsBuilder.class);
        Settings settings = mock(Settings.class);
        when(settingsBuilder.build()).thenReturn(settings);
        when(settings.mutate()).thenReturn(settingsBuilder);
        ElideSettings elideSettings = ElideSettings.builder().settings(settingsBuilder).build();
        ElideSettings mutated = elideSettings.mutate().serdes(serdes -> serdes.clear()).build();
        assertNotEquals(elideSettings.getSerdes().size(), mutated.getSerdes().size());
        assertNotNull(mutated.getSettings(settings.getClass()));
    }

    @Test
    void verbose() {
        ElideSettings elideSettings = ElideSettings.builder().verboseErrors(true).build();
        assertTrue(elideSettings.isVerboseErrors());
    }

    @Test
    void notVerbose() {
        ElideSettings elideSettings = ElideSettings.builder().verboseErrors(false).build();
        assertFalse(elideSettings.isVerboseErrors());
    }

    @Test
    void settings() {
        SettingsBuilder settingsBuilder = mock(SettingsBuilder.class);
        Settings settings = mock(Settings.class);
        when(settingsBuilder.build()).thenReturn(settings);
        ElideSettings elideSettings = ElideSettings.builder().settings(settingsBuilder).build();
        assertSame(settings, elideSettings.getSettings(settings.getClass()));
    }

    @Test
    void settingsBuilder() {
        SettingsBuilder settingsBuilder = mock(SettingsBuilder.class);
        Settings settings = mock(Settings.class);
        when(settingsBuilder.build()).thenReturn(settings);
        ElideSettingsBuilder elideSettingsBuilder = ElideSettings.builder().settings(customizer -> {
            customizer.put(settingsBuilder.getClass(), settingsBuilder);
        });
        assertSame(settingsBuilder, elideSettingsBuilder.getSettings(settingsBuilder.getClass()));
    }

    @Test
    void baseUrl() {
        ElideSettings settings = ElideSettings.builder().baseUrl("baseUrl").build();
        assertEquals("baseUrl", settings.getBaseUrl());
    }

    @Test
    void headerProcessor() {
        HeaderProcessor headerProcessor = mock(HeaderProcessor.class);
        ElideSettings settings = ElideSettings.builder().headerProcessor(headerProcessor).build();
        assertSame(headerProcessor, settings.getHeaderProcessor());
    }
}
