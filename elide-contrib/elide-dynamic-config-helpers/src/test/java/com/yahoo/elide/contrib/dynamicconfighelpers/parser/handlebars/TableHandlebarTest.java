/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideTableToPojo;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TableHandlebarTest {

    public static final String VALID_TABLE = "{\n"
            + "  tables: [{\n"
            + "      name: PlayerStats\n"
            + "      table: player_stats\n"
            + "      schema: gamedb\n"
            + "      description:\n"
            + "      '''\n"
            + "      A long description\n"
            + "      '''\n"
            + "      cardinality : large\n"
            + "      readAccess : A user is admin or is a player in the game\n"
            + "    \n"
            + "      joins: [\n"
            + "          {  \n"
            + "             name: playerCountry\n"
            + "             to: country\n"
            + "             type: toOne\n"
            + "             definition: '${to}.id = ${from}.country_id'\n"
            + "          }\n"
            + "      ]\n"
            + "    \n"
            + "      measures : [\n"
            + "         {  \n"
            + "            name : highScore\n"
            + "            type : INTEGER\n"
            + "            definition: 'MAX(score)'\n"
            + "         }\n"
            + "      ]\n"
            + "    \n"
            + "      dimensions : [\n"
            + "         { \n"
            + "           name : countryIsoCode\n"
            + "           type : TEXT\n"
            + "           definition : playerCountry.isoCode\n"
            + "         },\n"
            + "         { \n"
            + "           name : createdOn\n"
            + "           type : TIME \n"
            + "           definition : create_on\n"
            + "           grains: [ \n"
            + "            {\n"
            + "             grain :  DAY\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n"
            + "             '''\n"
            + "            },\n"
            + "            {\n"
            + "             grain :  MONTH\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-01'), 'yyyy-MM-dd')\n"
            + "             '''\n"
            + "            }\n"
            + "           ]\n"
            + "           \n"
            + "         }\n"
            + "      ]\n"
            + "    }]\n"
            + "}";

    public static final String VALID_TABLE_JAVA = "import java.util.Date;\n"
            + "import javax.persistence.Column;\n"
            + "import javax.persistence.Id;\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@Include(rootLevel = true)\n"
            + "@Cardinality(size = CardinalitySize.LARGE)\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@FromTable(name = \"player_stats\")\n"
            + "public class PlayerStats {\n"
            + "\n"
            + "\n"
            + "    private String countryIsoCode;\n"
            + "\n"
            + "\n"
            + "\n"
            + "    \n"
            + "        @DimensionFormula(\"playerCountry.isoCode\")\n"
            + "        public String getCountryIsoCode() {\n"
            + "        return countryIsoCode;\n"
            + "    }\n"
            + "    \n"
            + "    \n"
            + "\n"
            + "\n"
            + "    private Date createdOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grains = {\n"
            + "    \n"
            + "            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\"), \n"
            + "    \n"
            + "            @TimeGrainDefinition(grain = TimeGrain.MONTH, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-01'), 'yyyy-MM-dd')\")\n"
            + "    \n"
            + "    }, timeZone = \"UTC\")\n"
            + "\n"
            + "    public Date getCreatedOn() {\n"
            + "        return createdOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedOn(Date createdOn) {\n"
            + "        this.createdOn = createdOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Country country;\n"
            + "\n"
            + "    @Join(\"${to}.id = ${from}.country_id\")\n"
            + "    public Country getCountry() {\n"
            + "        return country;\n"
            + "    }\n"
            + "\n"
            + "    public void setCountry(Country country) {\n"
            + "        this.country = country;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "}\n";

    @Test
    public void whenHelperSourceIsCreatedThenCanRegister() throws IOException {

        TableHandlebar obj = new TableHandlebar();
        ElideTableToPojo testClass = new ElideTableToPojo();
        ElideTable table = testClass.parseTableConfig(VALID_TABLE);

        assertEquals(VALID_TABLE_JAVA, obj.hydrateTableTemplate(table));
        //System.out.println(obj.hydrateTableTemplate(table));
    }
}
