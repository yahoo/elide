/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.utils.DefaultClassScanner;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@TestInstance(Lifecycle.PER_CLASS)
public class HandlebarsHydratorTest {

    private static final String CONFIG_PATH = "src/test/resources/validator/valid";

    private static final String VALID_TABLE_WITH_VARIABLES = "{\n"
            + "  tables: [{\n"
            + "      name: <% name %>\n"
            + "      namespace: PlayerNamespace\n"
            + "      friendlyName: Player Statistics\n"
            + "      table: <% table %>\n"
            + "      schema: gamedb\n"
            + "      description:\n"
            + "      // newlines are replaced by single space in handlebar if no helper function is applied\n"
            + "      '''\n"
            + "      A long description\n"
            + "      with newline\n"
            + "       and additional space at start of this line.\n"
            + "      '''\n"
            + "      category: Table Category\n"
            + "      cardinality : lARge\n"
            + "      hidden : false\n"
            + "      readAccess : (user AND member) OR (admin AND NOT gu.est user)\n"
            + "      filterTemplate : countryIsoCode=={{code}}\n"
            + "      tags: ['GAME', 'PLAYER',  '''\n"
            + "      A tag\n"
            + "      with newline\n"
            + "      ''']\n"
            + "      hints: ['NoAggregateBeforeJoin']\n"
            + "      arguments: [\n"
            + "          {\n"
            + "             name: scoreFormat\n"
            + "             type: TEXT\n"
            + "             default: 999999D00\n"
            + "          }\n"
            + "          {\n"
            + "             name: countryCode\n"
            + "             type: TEXT\n"
            + "             tableSource: {\n"
            + "                 table: Country\n"
            + "                 column: isoCode\n"
            + "             }\n"
            + "             default: US\n"
            + "          }\n"
            + "      ]\n"
            + "      joins: [\n"
            + "          {\n"
            + "             name: playerCountry\n"
            + "             to: Country\n"
            + "             type: Cross\n"
            + "          },\n"
            + "          {\n"
            + "             name: playerTeam\n"
            + "             to: Team\n"
            + "             kind: Tomany\n"
            + "             type: left\n"
            + "             definition: '{{playerTeam.id}} = {{ team_id}}'\n"
            + "          }\n"
            + "      ]\n"
            + "\n"
            + "      measures : [\n"
            + "          {\n"
            + "          name : highScore\n"
            + "          friendlyName : High Score\n"
            + "          type : INteGER\n"
            + "          description : very awesome score\n"
            + "          definition: 'MAX({{score}})'\n"
            + "          tags: ['PUBLIC']\n"
            + "          },\n"
            + "          {\n"
            + "          name : newHighScore\n"
            + "          type : INteGER\n"
            + "          description : very awesome score\n"
            + "          definition: 'MAX({{score}})'\n"
            + "          tags: ['PUBLIC']\n"
            + "          }\n"
            + "      ]\n"
            + "      dimensions : [\n"
            + "         {\n"
            + "           name : countryIsoCode\n"
            + "           friendlyName : Country ISO Code\n"
            + "           type : TEXT\n"
            + "           category : country detail\n"
            + "           definition : '{{playerCountry.isoCode}}'\n"
            + "           values : ['US', 'HK']\n"
            + "           tags: ['PRIVATE']\n"
            + "           cardinality: Small\n"
            + "         },\n"
            + "         {\n"
            + "           name : teamRegion\n"
            + "           type : TEXT\n"
            + "           definition : '{{playerTeam.region}}'\n"
            + "           tableSource: {\n"
            + "              table: PlayerStatsChild\n"
            + "              namespace: PlayerNamespace\n"
            + "              column: teamRegion\n"
            + "           }\n"
            + "         },\n"
            + "         {\n"
            + "           name : createdOn\n"
            + "           friendlyName : Created On\n"
            + "           type : TIME\n"
            + "           definition : '{{create_on}}'\n"
            + "           filterTemplate : 'createdOn=={{createdOn}}'\n"
            + "           grains:\n"
            + "            [{\n"
            + "             type : DaY\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME({{$$column.expr}}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n"
            + "             '''\n"
            + "            }]\n"
            + "         },\n"
            + "         {\n"
            + "           name : updatedOn\n"
            + "           type : TIme\n"
            + "           definition : '{{updated_on}}'\n"
            + "         }\n"
            + "      ]\n"
            + "  }]\n"
            + "}\n";

    private DynamicConfigValidator testClass;
    private HandlebarsHydrator hydrator;

    @BeforeAll
    public void setup() throws IOException {
        hydrator = new HandlebarsHydrator();
        testClass = new DynamicConfigValidator(DefaultClassScanner.getInstance(), CONFIG_PATH);
        testClass.readConfigs();
    }

    @Test
    public void testConfigHydration() throws IOException {
        File file = new File(CONFIG_PATH);
        String hjsonPath = file.getAbsolutePath() + "/models/tables/player_stats.hjson";
        String content = new String(Files.readAllBytes(Paths.get(hjsonPath)));

        assertEquals(content, hydrator.hydrateConfigTemplate(
                VALID_TABLE_WITH_VARIABLES, testClass.getModelVariables()));
    }
}
