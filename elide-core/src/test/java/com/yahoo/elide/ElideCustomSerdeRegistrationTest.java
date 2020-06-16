/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yahoo.elide.core.datastore.inmemory.HashMapDataStore;
import com.yahoo.elide.core.datastore.inmemory.InMemoryDataStore;
import com.yahoo.elide.utils.coerce.CoerceUtil;
import com.yahoo.elide.utils.coerce.converters.ElideTypeConverter;
import com.yahoo.elide.utils.coerce.converters.Serde;

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
        HashMapDataStore wrapped = new HashMapDataStore(Dummy.class.getPackage());
        InMemoryDataStore store = new InMemoryDataStore(wrapped);
        ElideSettings elideSettings = new ElideSettingsBuilder(store).build();
        new Elide(elideSettings);
        assertNotNull(CoerceUtil.lookup(Dummy.class));
        assertNotNull(CoerceUtil.lookup(DummyTwo.class));
        assertNotNull(CoerceUtil.lookup(DummyThree.class));
    }
}
