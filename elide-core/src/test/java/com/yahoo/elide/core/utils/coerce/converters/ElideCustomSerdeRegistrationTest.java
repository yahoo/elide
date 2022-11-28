/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.utils.coerce.converters;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.Elide;
import com.yahoo.elide.ElideSettings;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.core.utils.coerce.CoerceUtil;
import org.junit.jupiter.api.Test;

class Dummy {
}

class DummyTwo extends Dummy {
}

class DummyThree extends Dummy {
}

public class ElideCustomSerdeRegistrationTest {
    @ElideTypeConverter(type = Dummy.class, name = "Dummy", subTypes = { DummyThree.class, DummyTwo.class })
    public static class DummySerde implements Serde<String, Dummy> {

        @Override
        public Dummy deserialize(String val) {
            return null;
        }

        @Override
        public String serialize(Dummy val) {
            return null;
        }
    }

    @Test
    public void testRegisterCustomSerde() {

        //Create a fake Elide.  Don't actually bind any entities.
        HashMapDataStore wrapped = new HashMapDataStore(DefaultClassScanner.getInstance(), String.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        ElideSettings elideSettings = new ElideSettingsBuilder(store)
                .withEntityDictionary(EntityDictionary.builder().build()).build();
        Elide elide = new Elide(elideSettings);
        elide.doScans();
        assertNotNull(CoerceUtil.lookup(Dummy.class));
        assertNotNull(CoerceUtil.lookup(DummyTwo.class));
        assertNotNull(CoerceUtil.lookup(DummyThree.class));
    }
}
