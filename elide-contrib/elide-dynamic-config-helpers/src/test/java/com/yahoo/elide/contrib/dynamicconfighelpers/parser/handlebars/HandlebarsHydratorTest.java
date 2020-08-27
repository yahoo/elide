/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.parser.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class HandlebarsHydratorTest {

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
            + "          }\n"
            + "      ]\n"
            + "      dimensions : [\n"
            + "         {\n"
            + "           name : countryIsoCode\n"
            + "           type : TEXT\n"
            + "           category : country detail\n"
            + "           definition : '{{playerCountry.isoCode}}'\n"
            + "         },\n"
            + "         {\n"
            + "           name : createdOn\n"
            + "           type : TIME\n"
            + "           definition : create_on\n"
            + "           grains: [\n"
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
            + "         }\n"
            + "      ]\n"
            + "  }]\n"
            + "}\n";

    private static final String VALID_TABLE_JAVA_NAME = "PlayerStats";

    private static final String VALID_TABLE_JAVA = "/*\n"
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
            + "import com.yahoo.elide.datastores.aggregation.annotation.DimensionFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.MetricFormula;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Join;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Meta;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.Temporal;\n"
            + "import com.yahoo.elide.datastores.aggregation.annotation.TimeGrainDefinition;\n"
            + "import com.yahoo.elide.datastores.aggregation.metadata.enums.TimeGrain;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromSubquery;\n"
            + "import com.yahoo.elide.datastores.aggregation.queryengines.sql.annotation.FromTable;\n"
            + "\n"
            + "import lombok.EqualsAndHashCode;\n"
            + "import lombok.ToString;\n"
            + "import lombok.Data;\n"
            + "\n"
            + "import java.math.BigDecimal;\n"
            + "import java.util.Date;\n"
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
            + "@Meta(description = \"A long description\", category=\"Table Category\")\n"
            + "@Include(rootLevel = true, type = \"playerStats\")\n"
            + "public class PlayerStats {\n"
            + "\n"
            + "    @Id\n"
            + "    private String id;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @Meta(description = \"countryIsoCode\", category=\"country detail\")\n"
            + "    \n"
            + "    @DimensionFormula(\"{{playerCountry.isoCode}}\")\n"
            + "    private String countryIsoCode;\n"
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
            + "\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @Meta(description = \"createdOn\")\n"
            + "    \n"
            + "    @DimensionFormula(\"create_on\")\n"
            + "    private Date createdOn;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Join(\"${to}.id = ${from}.country_id\")\n"
            + "\n"
            + "    private Country playerCountry;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @Join(\"${to}.id = ${from}.team_id\")\n"
            + "\n"
            + "    private Set<Team> playerTeam;\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "    @MetricFormula(\"MAX(score)\")\n"
            + "    @ReadPermission(expression = \"Prefab.Role.All\")\n"
            + "    @Meta(description = \"very awesome score\")\n"
            + "    \n"
            + "    private Long highScore;\n"
            + "\n"
            + "\n"
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

    @Test
    public void testConfigHydration() throws IOException {

        HandlebarsHydrator obj = new HandlebarsHydrator();
        String path = "src/test/resources";
        File file = new File(path);
        String absolutePath = file.getAbsolutePath();
        String hjsonPath = absolutePath + "/models/tables/table1.hjson";

        DynamicConfigValidator testClass = new DynamicConfigValidator(path);
        testClass.readAndValidateConfigs();

        Map<String, Object> map = testClass.getModelVariables();

        String content = new String (Files.readAllBytes(Paths.get(hjsonPath)));

        assertEquals(content, obj.hydrateConfigTemplate(VALID_TABLE_WITH_VARIABLES, map));
    }

    @Test
    public void testTableHydration() throws IOException {

        HandlebarsHydrator obj = new HandlebarsHydrator();
        String path = "src/test/resources";

        DynamicConfigValidator testClass = new DynamicConfigValidator(path);
        testClass.readAndValidateConfigs();

        Map<String, String> tableClasses = obj.hydrateTableTemplate(testClass.getElideTableConfig());

        assertEquals(true, tableClasses.keySet().contains(VALID_TABLE_JAVA_NAME));
        assertEquals(VALID_TABLE_JAVA, tableClasses.get(VALID_TABLE_JAVA_NAME));
    }

    @Test
    public void testSecurityHydration() throws IOException {
        HandlebarsHydrator obj = new HandlebarsHydrator();
        String path = "src/test/resources";

        DynamicConfigValidator testClass = new DynamicConfigValidator(path);
        testClass.readAndValidateConfigs();

        Map<String, String> securityClasses = obj.hydrateSecurityTemplate(testClass.getElideSecurityConfig());

        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(true, securityClasses.keySet().contains(VALID_SECURITY_GUEST_JAVA_NAME));
        assertEquals(VALID_SECURITY_ADMIN_JAVA, securityClasses.get(VALID_SECURITY_ADMIN_JAVA_NAME));
        assertEquals(VALID_SECURITY_GUEST_JAVA, securityClasses.get(VALID_SECURITY_GUEST_JAVA_NAME));
    }
}
