package com.yahoo.elide.modelconfig.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ConfigDataStoreTest {

    @Test
    public void testLoadObjects() {
        ConfigDataStore store = new ConfigDataStore("src/test/resources/validator/valid");

        ConfigDataStoreTransaction tx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        DataStoreIterable<ConfigFile> loaded = tx.loadObjects(EntityProjection.builder()
                        .type(ClassType.of(ConfigFile.class)).build(), scope);

        List<ConfigFile> configFiles = Lists.newArrayList(loaded.iterator());

        assertEquals(10, configFiles.size());
    }
}
