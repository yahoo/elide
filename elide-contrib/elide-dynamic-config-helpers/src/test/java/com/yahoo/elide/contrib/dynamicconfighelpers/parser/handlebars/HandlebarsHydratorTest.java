/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@TestInstance(Lifecycle.PER_CLASS)
public class HandlebarsHydratorTest {

    private static final String CONFIG_PATH = "src/test/resources/validator/valid";

    private static final String VALID_TABLE_WITH_VARIABLES = "{\n"
            + "  tables: [{\n"
            + "      name: <% name %>\n"
            + "      table: <% table %>\n"
            + "      schema: gamedb\n"
            + "      description:\n"
            + "      '''\n"
            + "      A long description\n"
            + "      '''\n"
            + "      category: Table Category\n"
            + "      cardinality : large\n"
            + "      hidden : false\n"
            + "      readAccess : A user is admin or is a player in the game\n"
            + "      filterTemplate : countryIsoCode=={{code}}\n"
            + "      tags: ['GAME', 'PLAYER']\n"
            + "      joins: [\n"
            + "          {\n"
            + "             name: playerCountry\n"
            + "             to: country\n"
            + "             type: toOne\n"
            + "             definition: '${to}.id = ${from}.country_id'\n"
            + "          },\n"
            + "          {\n"
            + "             name: playerTeam\n"
            + "             to: team\n"
            + "             type: toMany\n"
            + "             definition: '${to}.id = ${from}.team_id'\n"
            + "          }\n"
            + "      ]\n"
            + "\n"
            + "      measures : [\n"
            + "          {\n"
            + "          name : highScore\n"
            + "          type : INTEGER\n"
            + "          description : very awesome score\n"
            + "          definition: 'MAX(score)'\n"
            + "          tags: ['PUBLIC']\n"
            + "          }\n"
            + "      ]\n"
            + "      dimensions : [\n"
            + "         {\n"
            + "           name : countryIsoCode\n"
            + "           type : TEXT\n"
            + "           category : country detail\n"
            + "           definition : '{{playerCountry.isoCode}}'\n"
            + "           values : ['US', 'HK']\n"
            + "           tags: ['PRIVATE']\n"
            + "         },\n"
            + "         {\n"
            + "           name : createdOn\n"
            + "           type : TIME\n"
            + "           definition : create_on\n"
            + "           grain:\n"
            + "            {\n"
            + "             type :  SIMPLEDATE\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n"
            + "             '''\n"
            + "            }\n"
            + "         }\n"
            + "      ]\n"
            + "  }]\n"
            + "}\n";

    private static final String VALID_TABLE_JAVA_NAME = "PlayerStats";

    private static final String VALID_TABLE_CLASS = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.DeletePermission;\n"
            + "import com.yahoo.elide.annotation.Include;\n"
            + "import com.yahoo.elide.annotation.Exclude;\n"
            + "import com.yahoo.elide.annotation.ReadPermission;\n"
            + "import com.yahoo.elide.annotation.UpdatePermission;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Cardinality;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "\n"
            + "import lombok.Data;\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "\n"
            + "import java.math.BigDecimal;\n"
            + "import java.util.Date;\n"
            + "import java.util.Set;\n"
            + "import javax.persistence.Column;\n"
            + "import javax.persistence.Id;\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@Cardinality(size = CardinalitySize.LARGE)\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@Data\n"
            + "@FromTable(name = \"gamedb.player_stats\", dbConnectionName = \"\")\n"
            + "\n"
            + "@ReadPermission(expression = \"A user is admin or is a player in the game\")\n"
            + "@TableMeta(\n"
            + "    description = \"A long description\",\n"
            + "    category=\"Table Category\",\n"
            + "    tags={\"GAME\",\"PLAYER\"},\n"
            + "    filterTemplate=\"countryIsoCode=={{code}}\"\n"
            + ")\n"
            + "@Include(type = \"playerStats\")\n"
            + "public class PlayerStats {\n"
            + "\n"
            + "    private String id;\n"
            + "\n"
            + "    @Id\n"
            + "    public String getId() {\n"
            + "        return id;\n"
            + "    }\n"
            + "\n"
            + "    public void setId(String id){\n"
            + "        this.id = id;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private String countryIsoCode;\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"countryIsoCode\",\n"
            + "        category=\"country detail\",\n"
            + "        values={\"US\",\"HK\"},\n"
            + "        tags={\"PRIVATE\"},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "\n"
            + "    @DimensionFormula(\"{{playerCountry.isoCode}}\")\n"
            + "\n"
            + "    public String getCountryIsoCode(){\n"
            + "        return countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "    public void setCountryIsoCode(String countryIsoCode){\n"
            + "        this.countryIsoCode = countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate createdOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Temporal(grain = @TimeGrainDefinition(grain = TimeGrain.SIMPLEDATE, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\"), timeZone = \"UTC\")\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"createdOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "\n"
            + "    @DimensionFormula(\"create_on\")\n"
            + "\n"
            + "    public com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate getCreatedOn(){\n"
            + "        return createdOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedOn(com.yahoo.elide.datastores.aggregation.timegrains.SimpleDate createdOn){\n"
            + "        this.createdOn = createdOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Country playerCountry;\n"
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
            + "\n"
            + "    private Set<Team> playerTeam;\n"
            + "\n"
            + "    @Join(\"${to}.id = ${from}.team_id\")\n"
            + "    public Set<Team> getPlayerTeam() {\n"
            + "        return playerTeam;\n"
            + "    }\n"
            + "\n"
            + "    public void setPlayerTeam(Set<Team> playerTeam) {\n"
            + "        this.playerTeam = playerTeam;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Long highScore;\n"
            + "\n"
            + "    @MetricFormula(\"MAX(score)\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        description = \"very awesome score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "\n"
            + "    public Long getHighScore(){\n"
            + "        return highScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setHighScore(Long highScore){\n"
            + "        this.highScore = highScore;\n"
            + "    }\n"
            + "}\n";

    private static final String VALID_SECURITY_ADMIN_JAVA_NAME = "DynamicConfigOperationChecksPrincipalIsAdmin";
    private static final String VALID_SECURITY_GUEST_JAVA_NAME = "DynamicConfigOperationChecksPrincipalIsGuest";

    private static final String VALID_SECURITY_ADMIN_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksPrincipalIsAdmin.PRINCIPAL_IS_ADMIN)\n"
            + "public class DynamicConfigOperationChecksPrincipalIsAdmin extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String PRINCIPAL_IS_ADMIN = \"Principal is admin\";\n"
            + "    public DynamicConfigOperationChecksPrincipalIsAdmin() {\n"
            + "        super(\"admin\");\n"
            + "    }\n"
            + "}\n";

    private static final String VALID_SECURITY_GUEST_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksPrincipalIsGuest.PRINCIPAL_IS_GUEST)\n"
            + "public class DynamicConfigOperationChecksPrincipalIsGuest extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String PRINCIPAL_IS_GUEST = \"Principal is guest\";\n"
            + "    public DynamicConfigOperationChecksPrincipalIsGuest() {\n"
            + "        super(\"guest\");\n"
            + "    }\n"
            + "}\n";

    private DynamicConfigValidator testClass;
    private HandlebarsHydrator hydrator;

    @BeforeAll
    public void setup() throws IOException {
        hydrator = new HandlebarsHydrator();
        testClass = new DynamicConfigValidator(CONFIG_PATH);
        testClass.readAndValidateConfigs();
    }

    @Test
    public void testConfigHydration() throws IOException {
        File file = new File(CONFIG_PATH);
        String hjsonPath = file.getAbsolutePath() + "/models/tables/player_stats.hjson";
        String content = new String (Files.readAllBytes(Paths.get(hjsonPath)));

        assertEquals(content, hydrator.hydrateConfigTemplate(
                VALID_TABLE_WITH_VARIABLES, testClass.getModelVariables()));
    }

    @Test
    public void testTableHydration() throws IOException {

        Map<String, String> tableClasses = hydrator.hydrateTableTemplate(testClass.getElideTableConfig());

        assertTrue(tableClasses.keySet().contains(VALID_TABLE_JAVA_NAME));
        assertEquals(VALID_TABLE_CLASS.replaceAll("\\s+", ""),
                tableClasses.get(VALID_TABLE_JAVA_NAME).replaceAll("\\s+", ""));
    }

    @Test
    public void testSecurityHydration() throws IOException {
        Map<String, String> securityClasses = hydrator.hydrateSecurityTemplate(testClass.getElideSecurityConfig());

        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_GUEST_JAVA_NAME));
        assertEquals(VALID_SECURITY_ADMIN_JAVA, securityClasses.get(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(VALID_SECURITY_GUEST_JAVA, securityClasses.get(VALID_SECURITY_GUEST_JAVA_NAME));
    }
}
