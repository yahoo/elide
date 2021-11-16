package com.yahoo.elide.modelconfig.store;

import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.DATABASE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.NAMESPACE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.SECURITY;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.TABLE;
import static com.yahoo.elide.modelconfig.store.models.ConfigFile.ConfigFileType.VARIABLE;
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
        assertEquals(ConfigFile.builder()
                .version("")
                .type(ConfigFile.ConfigFileType.DATABASE)
                .content("{\n" +
                        "  dbconfigs:\n" +
                        "  [\n" +
                        "    {\n" +
                        "      name: MyDB2Connection\n" +
                        "      url: jdbc:db2:localhost:50000/testdb\n" +
                        "      driver: COM.ibm.db2.jdbc.net.DB2Driver\n" +
                        "      user: guestdb2\n" +
                        "      dialect: com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.PrestoDBDialect\n" +
                        "      propertyMap:\n" +
                        "      {\n" +
                        "        hibernate.show_sql: true\n" +
                        "        hibernate.default_batch_fetch_size: 100.1\n" +
                        "        hibernate.hbm2ddl.auto: create\n" +
                        "      }\n" +
                        "    }\n" +
                        "    {\n" +
                        "      name: MySQLConnection\n" +
                        "      url: jdbc:mysql://localhost/testdb?serverTimezone=UTC\n" +
                        "      driver: com.mysql.jdbc.Driver\n" +
                        "      user: guestmysql\n" +
                        "      dialect: com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.impl.HiveDialect\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n")
                .path("db/sql/multiple_db_no_variables.hjson")
                .build(), configFiles.get(0));

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
}
