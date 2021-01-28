/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.modelconfig.StaticModelsDetails;
import com.yahoo.elide.modelconfig.validator.DynamicConfigValidator;
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
            + "           tableSource: PlayerStatsChild.teamRegion\n"
            + "         },\n"
            + "         {\n"
            + "           name : createdOn\n"
            + "           friendlyName : Created On\n"
            + "           type : TIME\n"
            + "           definition : '{{create_on}}'\n"
            + "           grains:\n"
            + "            [{\n"
            + "             type : DaY\n"
            + "             sql :  '''\n"
            + "             PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\n"
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

    private static final String VALID_TABLE_JAVA_NAME = "PlayerStats";

    private static final String VALID_TABLE_CLASS = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.models;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.Include;\n"
            + "import com.yahoo.elide.annotation.Exclude;\n"
            + "import com.yahoo.elide.annotation.ReadPermission;\n"
            + "import com.yahoo.elide.annotation.UpdatePermission;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.CardinalitySize;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.ColumnMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.JoinType;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TableMeta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "import com.yahoo.elide.datastores.aggregation.timegrains.Time;\n"
            + "import com.yahoo.elide.core.type.ParameterizedModel;\n"
            + "\n"
            + "import lombok.Data;\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "\n"
            + "import java.math.BigDecimal;\n"
            + "import java.util.Set;\n"
            + "import javax.persistence.Id;\n"
            + "\n"
            + "\n"
            + "\n"
            + "/**\n"
            + " * A root level entity for testing AggregationDataStore.\n"
            + " */\n"
            + "@EqualsAndHashCode\n"
            + "@ToString\n"
            + "@Data\n"
            + "@FromTable(name = \"gamedb.player_stats\", dbConnectionName = \"\")\n"
            + "\n"
            + "@ReadPermission(expression = \"(user AND member) OR (admin AND NOT gu.est user)\")\n"
            + "@TableMeta(\n"
            + "    friendlyName = \"Player Statistics\",\n"
            + "    size = CardinalitySize.LARGE,\n"
            + "    description = \"A long description with newline  and additional space at start of this line.\",\n"
            + "    category=\"Table Category\",\n"
            + "    tags={\"GAME\",\"PLAYER\",\"A tag with newline\"},\n"
            + "    filterTemplate=\"countryIsoCode=={{code}}\",\n"
            + "    isFact=true\n"
            + ")\n"
            + "@Include(type = \"PlayerStats\")\n"
            + "public class PlayerStats extends ParameterizedModel {\n"
            + "\n"
            + "    private String id;\n"
            + "\n"
            + "    @Id\n"
            + "    public String getId() {\n"
            + "        return id;\n"
            + "    }\n"
            + "\n"
            + "    public void setId(String id) {\n"
            + "        this.id = id;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "    private String countryIsoCode;\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        friendlyName = \"Country ISO Code\",\n"
            + "        size = CardinalitySize.SMALL,\n"
            + "        description = \"countryIsoCode\",\n"
            + "        category=\"country detail\",\n"
            + "        values={\"US\",\"HK\"},\n"
            + "        tags={\"PRIVATE\"},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    @DimensionFormula(\"{{playerCountry.isoCode}}\")\n"
            + "    public String getCountryIsoCode() {\n"
            + "        return countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "    public void setCountryIsoCode(String countryIsoCode) {\n"
            + "        this.countryIsoCode = countryIsoCode;\n"
            + "    }\n"
            + "\n"
            + "    private String teamRegion;\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        \n"
            + "        description = \"teamRegion\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"PlayerStatsChild.teamRegion\"\n"
            + "    )\n"
            + "    \n"
            + "    @DimensionFormula(\"{{playerTeam.region}}\")\n"
            + "    public String getTeamRegion() {\n"
            + "        return teamRegion;\n"
            + "    }\n"
            + "\n"
            + "    public void setTeamRegion(String teamRegion) {\n"
            + "        this.teamRegion = teamRegion;\n"
            + "    }\n"
            + "\n"
            + "    private Time createdOn;\n"
            + "\n"
            + "    @Temporal(grains = {\n"
            + "    \n"
            + "            @TimeGrainDefinition(grain = TimeGrain.DAY, expression = \"PARSEDATETIME(FORMATDATETIME(${column}, 'yyyy-MM-dd'), 'yyyy-MM-dd')\")\n"
            + "    \n"
            + "    }, timeZone = \"UTC\")"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        friendlyName = \"Created On\",\n"
            + "        \n"
            + "        description = \"createdOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    @DimensionFormula(\"{{create_on}}\")\n"
            + "    public Time getCreatedOn() {\n"
            + "        return createdOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setCreatedOn(Time createdOn) {\n"
            + "        this.createdOn = createdOn;\n"
            + "    }\n"
            + "\n"
            + "    private Time updatedOn;\n"
            + "\n"
            + "    @Temporal(grains = {\n"
            + "    \n"
            + "    }, timeZone = \"UTC\")"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        \n"
            + "        description = \"updatedOn\",\n"
            + "        category=\"\",\n"
            + "        values={},\n"
            + "        tags={},\n"
            + "        tableSource=\"\"\n"
            + "    )\n"
            + "    \n"
            + "    @DimensionFormula(\"{{updated_on}}\")\n"
            + "    public Time getUpdatedOn() {\n"
            + "        return updatedOn;\n"
            + "    }\n"
            + "\n"
            + "    public void setUpdatedOn(Time updatedOn) {\n"
            + "        this.updatedOn = updatedOn;\n"
            + "    }\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    private Country playerCountry;\n"
            + "\n"
            + "    @Join(value=\"\", type=JoinType.CROSS)\n"
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
            + "\n"
            + "    private Set<Team> playerTeam;\n"
            + "\n"
            + "    @Join(value=\"{{playerTeam.id}} = {{team_id}}\", type=JoinType.LEFT)\n"
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
            + "    @MetricFormula(value=\"MAX({{score}})\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        friendlyName = \"High Score\",\n"
            + "        description = \"very awesome score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "    \n"
            + "    public Long getHighScore() {\n"
            + "        return highScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setHighScore(Long highScore) {\n"
            + "        this.highScore = highScore;\n"
            + "    }\n"
            + "\n"
            + "    private Long newHighScore;\n"
            + "    @MetricFormula(value=\"MAX({{score}})\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @ColumnMeta(\n"
            + "        \n"
            + "        description = \"very awesome score\",\n"
            + "        category=\"\",\n"
            + "        tags={\"PUBLIC\"}\n"
            + "    )\n"
            + "    \n"
            + "    public Long getNewHighScore() {\n"
            + "        return newHighScore;\n"
            + "    }\n"
            + "\n"
            + "    public void setNewHighScore(Long newHighScore) {\n"
            + "        this.newHighScore = newHighScore;\n"
            + "    }\n"
            + "\n"
            + "}\n";

    private static final String VALID_SECURITY_ADMIN_JAVA_NAME = "DynamicConfigOperationChecksAdmin";
    private static final String VALID_SECURITY_GUEST_JAVA_NAME = "DynamicConfigOperationChecksGu$est_user";

    private static final String VALID_SECURITY_ADMIN_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.checks;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.core.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksAdmin.ADMIN)\n"
            + "public class DynamicConfigOperationChecksAdmin extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String ADMIN = \"admin\";\n"
            + "    public DynamicConfigOperationChecksAdmin() {\n"
            + "        super(\"admin\");\n"
            + "    }\n"
            + "}\n";

    private static final String VALID_SECURITY_GUEST_JAVA = "/*\n"
            + " * Copyright 2020, Yahoo Inc.\n"
            + " * Licensed under the Apache License, Version 2.0\n"
            + " * See LICENSE file in project root for terms.\n"
            + " */\n"
            + "package dynamicconfig.checks;\n"
            + "\n"
            + "import com.yahoo.elide.annotation.SecurityCheck;\n"
            + "import com.yahoo.elide.core.security.checks.prefab.Role.RoleMemberCheck;\n"
            + "\n"
            + "\n"
            + "@SecurityCheck(DynamicConfigOperationChecksGu$est_user.GU$EST_USER)\n"
            + "public class DynamicConfigOperationChecksGu$est_user extends RoleMemberCheck {\n"
            + "\n"
            + "    public static final String GU$EST_USER = \"gu.est user\";\n"
            + "    public DynamicConfigOperationChecksGu$est_user() {\n"
            + "        super(\"gu.est user\");\n"
            + "    }\n"
            + "}\n";

    private DynamicConfigValidator testClass;
    private HandlebarsHydrator hydrator;

    @BeforeAll
    public void setup() throws IOException {
        hydrator = new HandlebarsHydrator(new StaticModelsDetails());
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
        assertEquals(VALID_TABLE_CLASS, tableClasses.get(VALID_TABLE_JAVA_NAME));
    }

    @Test
    public void testChildTableHydration() throws IOException {
        Map<String, String> tableClasses = hydrator.hydrateTableTemplate(testClass.getElideTableConfig());

        assertTrue(tableClasses.keySet().contains("PlayerStatsChild"));
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private String highScore")); // overridden measure
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private Long newHighScore;")); // parent measure
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private Long avgScore;")); // child measure
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private Set<Team> playerTeam;")); // join
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private" + " Date createdOn")); // overridden dim
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private" + " Date updatedOn")); // parent dim
        assertTrue(tableClasses.get("PlayerStatsChild").contains("private" + " Date updatedMonth")); // child dim
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
