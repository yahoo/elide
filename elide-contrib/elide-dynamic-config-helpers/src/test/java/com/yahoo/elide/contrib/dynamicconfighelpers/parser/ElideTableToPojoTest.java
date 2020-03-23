/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import org.junit.jupiter.api.Test;

public class ElideTableToPojoTest {

    private ElideConfigParser testClass = new ElideConfigParser();
    private static final String VALID_TABLE = "{\n"
                    + "    tables: [{\n"
                    + "        name: PlayerStats\n"
                    + "        table: player_stats\n"
                    + "        schema: gamedb\n"
                    + "        description:\n"
                    + "        '''\n"
                    + "        A long description\n"
                    + "        '''\n"
                    + "        cardinality : large\n"
                    + "        readAccess : A user is admin or is a player in the game\n"
                    + "        joins: [\n"
                    + "            {\n"
                    + "                name: playerCountry\n"
                    + "                to: country\n"
                    + "                type: toOne\n"
                    + "                definition: '${to}.id = ${from}.country_id'\n"
                    + "            }\n"
                    + "        ]\n"
                    + "        measures : [\n"
                    + "            {\n"
                    + "            name : highScore\n"
                    + "            type : INTEGER\n"
                    + "            definition: 'MAX(score)'\n"
                    + "            }\n"
                    + "        ]\n"
                    + "        dimensions : [\n"
                    + "            {\n"
                    + "                name : countryCode\n"
                    + "                type : TEXT\n"
                    + "                definition : playerCountry.isoCode\n"
                    + "            },\n"
                    + "            {\n"
                    + "                name : createdOn\n"
                    + "                type : TIME\n"
                    + "                definition : create_on\n"
                    + "                grains: [\n"
                    + "                {\n"
                    + "                    grain :  MONTH\n"
                    + "                    sql :  '''\n"
                    + "                    PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-01'), 'yyyy-MM-dd')\n"
                    + "                    '''\n"
                    + "                }\n"
                    + "                ]\n"
                    + "            }\n"
                    + "        ]\n"
                    + "    }]\n"
                    + "}";

    @Test
    public void testValidTable() throws Exception {
        ElideTable table = (ElideTable) testClass.parseConfigString(VALID_TABLE, "table");

        for (Table t : table.getTables()) {
            assertEquals(t.getMeasures().get(0).getName() , t.getMeasures().get(0).getDescription());
            assertEquals("MAX(score)", t.getMeasures().get(0).getDefinition());
            assertEquals(Table.Cardinality.LARGE, t.getCardinality());
        }
    }

    @Test
    public void testInValidTable() throws Exception {
        assertNull(testClass.parseConfigFile("", "test"));
    }

    @Test
    public void testValidTableConfig() {
        String tableSchemaFile = "https://raw.githubusercontent.com/yahoo/elide/elide-5.x-dynamic-config/"
                        + "elide-contrib/elide-dynamic-config-helpers/src/test/resources/table/valid_table.hjson";

        ElideTable table = (ElideTable) testClass.parseConfigFile(tableSchemaFile, "table");

        for (Table t : table.getTables()) {
            assertEquals(t.getMeasures().get(0).getName() , t.getMeasures().get(0).getDescription());
            assertEquals("MAX(score)", t.getMeasures().get(0).getDefinition());
            assertEquals(Table.Cardinality.LARGE, t.getCardinality());
        }
    }
}
