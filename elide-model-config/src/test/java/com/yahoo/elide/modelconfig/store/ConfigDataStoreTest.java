/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.modelconfig.store;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.modelconfig.store.ConfigDataStore.VALIDATE_ONLY_HEADER;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.DATABASE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.NAMESPACE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.SECURITY;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.TABLE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.VARIABLE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.toId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreIterable;
import com.yahoo.elide.core.exceptions.BadRequestException;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.modelconfig.store.models.ConfigFile;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import com.yahoo.elide.modelconfig.validator.Validator;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Slf4j
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
                .build(), configFiles.get(1)));

        assertEquals("db/sql/multiple_db.hjson", configFiles.get(0).getPath());
        assertEquals(DATABASE, configFiles.get(0).getType());

        assertEquals("db/sql/single_db.hjson", configFiles.get(2).getPath());
        assertEquals(DATABASE, configFiles.get(2).getType());

        assertEquals("db/variables.hjson", configFiles.get(3).getPath());
        assertEquals(VARIABLE, configFiles.get(3).getType());

        assertEquals("models/namespaces/player.hjson", configFiles.get(4).getPath());
        assertEquals(NAMESPACE, configFiles.get(4).getType());

        assertEquals("models/security.hjson", configFiles.get(5).getPath());
        assertEquals(SECURITY, configFiles.get(5).getType());

        assertEquals("models/tables/player_stats.hjson", configFiles.get(6).getPath());
        assertEquals(TABLE, configFiles.get(6).getType());

        assertEquals("models/tables/player_stats_extends.hjson", configFiles.get(7).getPath());
        assertEquals(TABLE, configFiles.get(7).getType());

        assertEquals("models/tables/referred_model.hjson", configFiles.get(8).getPath());
        assertEquals(TABLE, configFiles.get(8).getType());

        assertEquals("models/variables.hjson", configFiles.get(9).getPath());
        assertEquals(VARIABLE, configFiles.get(9).getType());
    }

    @Test
    public void testCreate(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile newFile = createFile("test", store, false);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                toId("models/tables/test.hjson", NO_VERSION), scope);

        assertTrue(compare(newFile, loaded));
    }

    @Test
    public void testCreateReadOnly() {

        //This path is read only (Classpath)...
        String configRoot = "src/test/resources/validator/valid";

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        assertThrows(UnsupportedOperationException.class,
                () -> createFile("test", store, false));
    }

    @Test
    public void testCreateInvalid(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        assertThrows(BadRequestException.class,
                () -> createInvalidFile(configRoot, store));
    }

    @Test
    public void testCreateValidateOnly(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                toId("models/tables/test.hjson", NO_VERSION), scope);

        assertNull(loaded);
    }

    @Test
    public void testUpdate(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        createFile("test", store, false);
        ConfigFile updateFile = updateFile(configRoot, store);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        RequestScope scope = mock(RequestScope.class);

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                toId("models/tables/test.hjson", NO_VERSION), scope);

        assertTrue(compare(updateFile, loaded));
    }

    @Test
    public void testUpdateWithPermissionError(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile createdFile = createFile("test", store, false);

        String createdFilePath = Path.of(configPath.toFile().getPath(), createdFile.getPath()).toFile().getPath();

        File file = new File(createdFilePath);
        boolean blockFailed = blockWrites(file);

        if (blockFailed) {
            //We can't actually test because setting permissions isn't working.
            return;
        }

        assertThrows(UnsupportedOperationException.class, () -> updateFile(configRoot, store));
    }

    @Test
    public void testDeleteWithPermissionError(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile createdFile = createFile("test", store, false);

        String createdFilePath = Path.of(configPath.toFile().getPath(), createdFile.getPath()).toFile().getPath();

        File file = new File(createdFilePath);
        boolean blockFailed = blockWrites(file);

        if (blockFailed) {
            //We can't actually test because setting permissions isn't working.
            return;
        }

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);

        assertThrows(UnsupportedOperationException.class, () -> tx.delete(createdFile, scope));
    }

    @Test
    public void testCreateWithPermissionError(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();
        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        File file = configPath.toFile();
        boolean blockFailed = blockWrites(file);

        if (blockFailed) {
            //We can't actually test because setting permissions isn't working.
            return;
        }

        assertThrows(UnsupportedOperationException.class, () -> createFile("test", store, false));
    }

    @Test
    public void testDelete(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);

        ConfigFile newFile = createFile("test", store, false);

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);
        tx.delete(newFile, scope);

        tx.flush(scope);
        tx.commit(scope);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();

        ConfigFile loaded = readTx.loadObject(EntityProjection.builder().type(ClassType.of(ConfigFile.class)).build(),
                toId("models/tables/test.hjson", NO_VERSION), scope);

        assertNull(loaded);
    }

    @Test
    public void testMultipleFileOperations(@TempDir Path configPath) {
        String configRoot = configPath.toFile().getPath();

        Validator validator = new DynamicConfigValidator(DefaultClassScanner.getInstance(), configRoot);
        ConfigDataStore store = new ConfigDataStore(configRoot, validator);
        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);

        String [] tables = {"table1", "table2", "table3"};

        for (String tableName : tables) {
            Supplier<String> contentProvider = () -> String.format("{            \n"
                    + "  tables: [{     \n"
                    + "      name: %s\n"
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
                    + "}", tableName);

            ConfigFile newFile = ConfigFile.builder()
                    .type(TABLE)
                    .contentProvider(contentProvider)
                    .path(String.format("models/tables/%s.hjson", tableName))
                    .build();


            tx.createObject(newFile, scope);
        }

        ConfigFile invalid = ConfigFile.builder().path("/tmp").contentProvider((() -> "Invalid")).build();
        tx.createObject(invalid, scope);

        tx.delete(invalid, scope);

        tx.flush(scope);
        tx.commit(scope);

        ConfigDataStoreTransaction readTx = store.beginReadTransaction();
        DataStoreIterable<ConfigFile> loaded = readTx.loadObjects(EntityProjection.builder()
                .type(ClassType.of(ConfigFile.class)).build(), scope);

        List<ConfigFile> configFiles = Lists.newArrayList(loaded.iterator());

        assertEquals(3, configFiles.size());

        assertEquals("models/tables/table1.hjson", configFiles.get(0).getPath());
        assertEquals(TABLE, configFiles.get(0).getType());

        assertEquals("models/tables/table2.hjson", configFiles.get(1).getPath());
        assertEquals(TABLE, configFiles.get(1).getType());

        assertEquals("models/tables/table3.hjson", configFiles.get(2).getPath());
        assertEquals(TABLE, configFiles.get(2).getType());
    }

    protected ConfigFile createFile(String tableName, ConfigDataStore store, boolean validateOnly) {
        Supplier<String> contentProvider = () -> String.format("{            \n"
                + "  tables: [{     \n"
                + "      name: %s\n"
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
                + "}", tableName);

        ConfigFile newFile = ConfigFile.builder()
                .type(TABLE)
                .contentProvider(contentProvider)
                .path(String.format("models/tables/%s.hjson", tableName))
                .build();

        ConfigDataStoreTransaction tx = store.beginTransaction();
        RequestScope scope = mock(RequestScope.class);

        if (validateOnly) {
            when(scope.getRequestHeaderByName(eq(VALIDATE_ONLY_HEADER))).thenReturn("true");
        }

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

    protected ConfigFile createInvalidFile(String configRoot, ConfigDataStore store) {
        Supplier<String> contentProvider = () -> "{            \n"
                + "  tables: [{     \n"
                + "      name: Test\n"
                + "      table: test\n"
                + "      schema: test\n"
                + "      measures : [\n"
                + "         {\n"
                + "          name : measure\n"
                + "          type : INVALID_TYPE\n"
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

    protected boolean compare(ConfigFile a, ConfigFile b) {
        return a.equals(b) && a.getContent().equals(b.getContent()) && a.getType().equals(b.getType());
    }

    protected boolean blockWrites(File file) {
        Set<PosixFilePermission> perms = new HashSet<>();

        try {
            //Windows doesn't like this.
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (Exception e) {
            file.setWritable(false, false);
        }

        return Files.isWritable(file.toPath());
    }
}
