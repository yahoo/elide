/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.DATABASE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.NAMESPACE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.SECURITY;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.TABLE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.VARIABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.modelconfig.validator.Validator;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public class ConfigDataStoreTest {

    @Test
    public void testLoadObjects() {
        String configRoot = "src/test/resources/validator/valid";
        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigDataStoreTransaction tx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        DataStoreIterable<ConfigFile> loaded = tx.loadObjects(EntityProjection.builder()
                        .type(ClassType.of(ConfigFile.class)).build(), scope);

        List<ConfigFile> configFiles = Lists.newArrayList(loaded.iterator());

        Supplier<String> contentProvider = () ->
                "{\n"
                        + "  dbconfigs:\n"
                        + "  [\n"
                        + "    {\n"
                        + "      name: MyDB2Connection\n"
                        + "      url: jdbc:db2:localhost:50000/testdb\n"
                        + "      driver: COM.ibm.db2.jdbc.net.DB2Driver\n"
                        + "      user: guestdb2\n"
                        + "      dialect: com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.PrestoDBDialect\n"
                        + "      propertyMap:\n"
                        + "      {\n"
                        + "        hibernate.show_sql: true\n"
                        + "        hibernate.default_batch_fetch_size: 100.1\n"
                        + "        hibernate.hbm2ddl.auto: create\n"
                        + "      }\n"
                        + "    }\n"
                        + "    {\n"
                        + "      name: MySQLConnection\n"
                        + "      url: jdbc:mysql://localhost/testdb?serverTimezone=UTC\n"
                        + "      driver: com.mysql.jdbc.Driver\n"
                        + "      user: guestmysql\n"
                        + "      dialect: com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.HiveDialect\n"
                        + "    }\n"
                        + "  ]\n"
                        + "}\n";

        assertEquals(10, configFiles.size());
        assertTrue(compare(ConfigFile.builder()
                .version("")
                .type(ConfigFile.ConfigFileType.DATABASE)
                .contentProvider(contentProvider)
                .path("db/sql/multiple_db_no_variables.hjson")
                .build(), configFiles.get(0)));

        assertEquals("models/namespaces/player.hjson", configFiles.get(1).getPath());
        assertEquals(NAMESPACE, configFiles.get(1).getType());

        assertEquals("db/variables.hjson", configFiles.get(2).getPath());
        assertEquals(VARIABLE, configFiles.get(2).getType());

        assertEquals("models/tables/referred_model.hjson", configFiles.get(3).getPath());
        assertEquals(TABLE, configFiles.get(3).getType());

        assertEquals("db/sql/single_db.hjson", configFiles.get(4).getPath());
        assertEquals(DATABASE, configFiles.get(4).getType());

        assertEquals("models/tables/player_stats_extends.hjson", configFiles.get(5).getPath());
        assertEquals(TABLE, configFiles.get(5).getType());

        assertEquals("db/sql/multiple_db.hjson", configFiles.get(6).getPath());
        assertEquals(DATABASE, configFiles.get(6).getType());

        assertEquals("models/variables.hjson", configFiles.get(7).getPath());
        assertEquals(VARIABLE, configFiles.get(7).getType());

        assertEquals("models/tables/player_stats.hjson", configFiles.get(8).getPath());
        assertEquals(TABLE, configFiles.get(8).getType());

        assertEquals("models/security.hjson", configFiles.get(9).getPath());
        assertEquals(SECURITY, configFiles.get(9).getType());
    }

    @Test
    public void testCreate(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile newFile = createFile(configRoot, store);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                "models/tables/test.hjson", scope);

        assertTrue(compare(newFile, loaded));
    }

    @Test
    public void testUpdate(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        createFile(configRoot, store);
        ConfigFile updateFile = updateFile(configRoot, store);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                "models/tables/test.hjson", scope);

        assertTrue(compare(updateFile, loaded));
    }

    @Test
    public void testDelete(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile newFile = createFile(configRoot, store);

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);
        tx.delete(newFile, scope);

        tx.flush(scope);
        tx.commit(scope);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                "models/tables/test.hjson", scope);

        assertNull(loaded);
    }

    protected ConfigFile createFile(String configRoot, ConfigDataStore store) {
        Supplier<String> contentProvider = () -> "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        ConfigFile newFile = ConfigFile.builder()
                .type(TABLE)
                .contentProvider(contentProvider)
                .path("models/tables/test.hjson")
                .build();

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);

        tx.createObject(newFile, scope);
        tx.save(newFile, scope);
        tx.flush(scope);
        tx.commit(scope);

        return newFile;
    }

    protected ConfigFile updateFile(String configRoot, ConfigDataStore store) {
        Supplier<String> contentProvider = () -> "{            \n"
                + "  tables: [{     \n"
                + "      name: Test2\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INTEGER\n"
                + "          definition: 'MAX({{$measure}})'\n"
                + "         }\n"
                + "      ]      \n"
                + "      dimensions : [\n"
                + "         {\n"
                + "           name : dimension\n"
                + "           type : TEXT\n"
                + "           definition : '{{$dimension}}'\n"
                + "         }\n"
                + "      ]\n"
                + "  }]\n"
                + "}";

        ConfigFile updatedFile = ConfigFile.builder()
                .type(TABLE)
                .contentProvider(contentProvider)
                .path("models/tables/test.hjson")
                .build();

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);

        tx.save(updatedFile, scope);
        tx.flush(scope);
        tx.commit(scope);

        return updatedFile;
    }

    protected boolean compare(ConfigFile a, ConfigFile b) {
        return a.equals(b) && a.getContent().equals(b.getContent()) && a.getType().equals(b.getType());
    }
}
