/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.datastore.DataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.coerce.converters.InstantSerde;
import com.yahoo.elide.core.utils.coerce.converters.OffsetDateTimeSerde;
import com.yahoo.elide.core.utils.coerce.converters.Serde;
import com.yahoo.elide.core.utils.coerce.converters.TimeZoneSerde;
import com.yahoo.elide.core.utils.coerce.converters.URLSerde;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.TimeZone;


class ElideSettingsBuilderTest {

    private ElideSettingsBuilder testInstance;

    @Mock
    private DataStore dataStore;

    @BeforeEach
    public void setUp() {
        testInstance = new ElideSettingsBuilder(dataStore).withEntityDictionary(EntityDictionary.builder().build());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void shouldBuildSettingsWithDefaultSerdes() {
        Map<Class, Serde> serdes = testInstance.build().getSerdes();

        assertEquals(InstantSerde.class, serdes.get(Instant.class).getClass());
        assertEquals(OffsetDateTimeSerde.class, serdes.get(OffsetDateTime.class).getClass());
        assertEquals(TimeZoneSerde.class, serdes.get(TimeZone.class).getClass());
        assertEquals(URLSerde.class, serdes.get(URL.class).getClass());
    }
}
