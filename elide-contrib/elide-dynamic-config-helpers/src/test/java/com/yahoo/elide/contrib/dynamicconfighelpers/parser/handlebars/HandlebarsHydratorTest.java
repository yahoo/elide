/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurity;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTable;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideSecurityToPojo;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideTableToPojo;
import com.yahoo.elide.contrib.dynamicconfighelpers.parser.ElideVariableToPojo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HandlebarsHydratorTest {

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

    public static final String VALID_TABLE_WITH_VARIABLES = "{\n"
            + "  tables: [{\n"
            + "      name: {{ name }}\n"
            + "      table: {{ table }}\n"
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

    public static final String VALID_TABLE_JAVA_NAME = "PlayerStats";

    public static final String VALID_TABLE_JAVA = "package com.yahoo.elide.contrib.dynamicconfig.model;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.Include;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.JoinTo;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "import lombok.Data;\n"
            + "\n"
            + "import java.util.Date;\n"
            + "import javax.persistence.Column;\n"
            + "import javax.persistence.Id;\n"
            + "import javax.persistence.Entity;\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@Include(rootLevel = true)\n"
            + "@Cardinality(size = CardinalitySize.LARGE)\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@Entity\n"
            + "@Data\n"
            + "@FromTable(name = \"player_stats\")\n"
            + "public class PlayerStats {\n"
            + "\n"
            + "    @Id\n"
            + "    private String name;\n"
            + "\n"
            + "\n"
            + "    private String countryIsoCode;\n"
            + "\n"
            + "    private Date createdOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Country playerCountry;\n"
            + " \n"
            + "\n"
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
            + "    @Join(\"${to}.id = ${from}.country_id\")\n"
            + "    public Country getPlayerCountry() {\n"
            + "        return playerCountry;\n"
            + "    }\n"
            + "\n"
            + "    public void setPlayerCountry(Country playerCountry) {\n"
            + "        this.playerCountry = playerCountry;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "}\n";

    public static final String VALID_VARIABLE = "{\n"
            + "    name: PlayerStats\n"
            + "    table: player_stats\n"
            + "}";

    private static final String VALID_SECURITY = "{\n"
            + "    roles : [\n"
            + "        admin, \n"
            + "        guest, \n"
            + "        member\n"
            + "        ]\n"
            + "    rules: [\n"
            + "        {\n"
            + "            filter: id==${principal.id}\n"
            + "            name: Principal is Admin\n"
            + "        },\n"
            + "        {\n"
            + "            filter: id==${principal.id}\n"
            + "            name: Principal is member\n"
            + "        },\n"
            + "    ]\n"
            + "}";

    public static final String VALID_SECURITY_JAVA = "package com.yahoo.elide.contrib.dynamicconfig.model;\n"
            + "\n"
            + "import java.security.Principal;\n"
            + "import java.util.Optional;\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "/**\n"
            + " * Operation Checks on the Async Query and Result objects.\n"
            + " */\n"
            + "public class AsyncQueryOperationChecks {\n"
            + "\n"
            + "\n"
            + "    @SecurityCheck(AsyncQueryOwnerPrincipalIsAdmin.PRINCIPAL_IS_ADMIN)\n"
            + "    public static class AsyncQueryOwnerPrincipalIsAdmin extends RoleMemberCheck {\n"
            + "        public static final String PRINCIPAL_IS_ADMIN = \"Principal is Admin\";\n"
            + "        public AsyncQueryOwnerPrincipalIsAdmin() {\n"
            + "                        super(\"admin\");\n"
            + "                }\n"
            + "    }\n"
            + "\n"
            + "    @SecurityCheck(AsyncQueryOwnerPrincipalIsMember.PRINCIPAL_IS_MEMBER)\n"
            + "    public static class AsyncQueryOwnerPrincipalIsMember extends RoleMemberCheck {\n"
            + "        public static final String PRINCIPAL_IS_MEMBER = \"Principal is member\";\n"
            + "        public AsyncQueryOwnerPrincipalIsMember() {\n"
            + "                        super(\"member\");\n"
            + "                }\n"
            + "    }\n"
            + "\n"
            + "}\n";

    @Test
    public void testConfigHydration() throws IOException {

        ElideVariableToPojo testClass = new ElideVariableToPojo();
        Map<String, Object> map = testClass.parseVariableConfig(VALID_VARIABLE);
        HandlebarsHydrator obj = new HandlebarsHydrator();

        assertEquals(VALID_TABLE, obj.hydrateConfigTemplate(VALID_TABLE_WITH_VARIABLES, map));
    }

    @Test
    public void testTableHydration() throws IOException {

        HandlebarsHydrator obj = new HandlebarsHydrator();
        ElideTableToPojo testClass = new ElideTableToPojo();
        ElideTable table = testClass.parseTableConfig(VALID_TABLE);

        List<String> tableObjects = obj.hydrateTableTemplate(table);

        assertEquals(VALID_TABLE_JAVA, tableObjects.get(0));
    }

    @Test
    public void testSecurityHydration() throws IOException {

        HandlebarsHydrator obj = new HandlebarsHydrator();
        ElideSecurityToPojo testClass = new ElideSecurityToPojo();
        ElideSecurity security = testClass.parseSecurityConfig(VALID_SECURITY);

        assertEquals(VALID_SECURITY_JAVA, obj.hydrateSecurityTemplate(security));
    }

    @Test
    public void getTableClassNames() throws IOException {

        HandlebarsHydrator obj = new HandlebarsHydrator();
        ElideTableToPojo testClass = new ElideTableToPojo();
        ElideTable table = testClass.parseTableConfig(VALID_TABLE);

        List<String> tableObjects = obj.getTableClassNames(table);

        assertEquals(VALID_TABLE_JAVA_NAME, tableObjects.get(0));
    }
}
