/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.modelconfig.DynamicConfigHelpers.isNullOrEmpty;
import static com.yahoo.elide.modelconfig.compile.ElideDynamicEntityCompiler.isStaticModel;
import static com.yahoo.elide.modelconfig.compile.ElideDynamicEntityCompiler.staticModelHasField;
import static com.yahoo.elide.modelconfig.parser.handlebars.HandlebarsHelper.REFERENCE_PARENTHESES;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.yahoo.elide.modelconfig.Config;
import com.yahoo.elide.modelconfig.DynamicConfigHelpers;
import com.yahoo.elide.modelconfig.DynamicConfigSchemaValidator;
import com.yahoo.elide.modelconfig.model.DBConfig;
import com.yahoo.elide.modelconfig.model.Dimension;
import com.yahoo.elide.modelconfig.model.ElideDBConfig;
import com.yahoo.elide.modelconfig.model.ElideSQLDBConfig;
import com.yahoo.elide.modelconfig.model.ElideSecurityConfig;
import com.yahoo.elide.modelconfig.model.ElideTableConfig;
import com.yahoo.elide.modelconfig.model.Join;
import com.yahoo.elide.modelconfig.model.Measure;
import com.yahoo.elide.modelconfig.model.Named;
import com.yahoo.elide.modelconfig.model.Table;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
/**
 * Util class to validate and parse the config files.
 */
public class DynamicConfigValidator {

    private static final Set<String> SQL_DISALLOWED_WORDS = new HashSet<>(
            Arrays.asList("DROP", "TRUNCATE", "DELETE", "INSERT", "UPDATE", "ALTER", "COMMENT", "CREATE", "DESCRIBE",
                    "SHOW", "USE", "GRANT", "REVOKE", "CONNECT", "LOCK", "EXPLAIN", "CALL", "MERGE", "RENAME"));
    private static final String[] ROLE_NAME_DISALLOWED_WORDS = new String[] { "," };
    private static final String SQL_SPLIT_REGEX = "\\s+";
    private static final String SEMI_COLON = ";";
    private static final Pattern HANDLEBAR_REGEX = Pattern.compile("<%(.*?)%>");
    private static final String RESOURCES = "resources";
    private static final int RESOURCES_LENGTH = 9; //"resources".length()
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String HJSON_EXTN = "**/*.hjson";

    private final ElideTableConfig elideTableConfig = new ElideTableConfig();
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> modelVariables;
    private Map<String, Object> dbVariables;
    private final ElideDBConfig elideSQLDBConfig = new ElideSQLDBConfig();
    private final String configDir;
    private final DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
    private final Map<String, Resource> resourceMap = new HashMap<>();
    private final PathMatchingResourcePatternResolver resolver;

    public DynamicConfigValidator(String configDir) {
        resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());

        String pattern = CLASSPATH_PATTERN + DynamicConfigHelpers.formatFilePath(formatClassPath(configDir));

        boolean classPathExists = false;
        try {
            classPathExists = (resolver.getResources(pattern).length != 0);
        } catch (IOException e) {
            //NOOP
        }

        if (classPathExists) {
            this.configDir = pattern;
        } else {
            File config = new File(configDir);
            if (! config.exists()) {
                throw new IllegalStateException(configDir + " : config path does not exist");
            }
            this.configDir = FILEPATH_PATTERN + DynamicConfigHelpers.formatFilePath(config.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        Options options = prepareOptions();

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.hasOption("help")) {
                printHelp(options);
                return;
            }
            if (!cli.hasOption("configDir")) {
                printHelp(options);
                System.err.println("Missing required option");
                System.exit(1);
            }
            String configDir = cli.getOptionValue("configDir");

            DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(configDir);
            dynamicConfigValidator.readAndValidateConfigs();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }

        System.out.println("Configs Validation Passed!");
    }

    /**
     * Read and validate config files under config directory.
     * @throws IOException IOException
     */
    public void readAndValidateConfigs() throws IOException {
        this.loadConfigMap();
        this.setModelVariables(readVariableConfig(Config.MODELVARIABLE));
        this.setElideSecurityConfig(readSecurityConfig());
        validateRoleInSecurityConfig(this.elideSecurityConfig);
        this.setDbVariables(readVariableConfig(Config.DBVARIABLE));
        this.elideSQLDBConfig.setDbconfigs(readDbConfig());
        this.elideTableConfig.setTables(readTableConfig());
        validateRequiredConfigsProvided();
        validateNameUniqueness(this.elideSQLDBConfig.getDbconfigs());
        validateNameUniqueness(this.elideTableConfig.getTables());
        populateInheritance(this.elideTableConfig);
        validateTableConfig(this.elideTableConfig);
        validateJoinedTablesDBConnectionName(this.elideTableConfig);
    }

    private static void validateInheritance(ElideTableConfig tables) {
        tables.getTables().stream().forEach(table -> {
            validateInheritance(tables, table, new HashSet<>());
        });
    }

    private static void validateInheritance(ElideTableConfig tables, Table table, Set<Table> visited) {
        visited.add(table);

        if (!table.hasParent()) {
            return;
        }
        Table parent = table.getParent(tables);
        if (parent == null) {
            throw new IllegalStateException(
                    "Undefined model: " + table.getExtend() + " is used as a Parent(extend) for another model.");
        }
        if (visited.contains(parent)) {
            throw new IllegalStateException(
                    String.format("Inheriting from table '%s' creates an illegal cyclic dependency.",
                            parent.getName()));
        } else {
            validateInheritance(tables, parent, visited);
        }
    }

    @SuppressWarnings("unchecked")
    private void populateInheritance(ElideTableConfig elideTableConfig) {
        //ensures validation is run before populate always.
        validateInheritance(this.elideTableConfig);

        List<Table> tables = elideTableConfig.getTables().stream().collect(Collectors.toList());
        while (!tables.isEmpty()) {
            Table table = tables.remove(0);
            if (table.hasParent()) {

                Table parent = table.getParent(elideTableConfig);
                // If parent also extends, ensure parent is processed first.
                if (parent.getExtend() != null && parent.getSql() == null && parent.getTable() == null) {
                    tables.add(table);
                    continue;
                }

                Map<String, Measure> measures = (Map<String, Measure>) getInheritedAttribute(table,
                        new HashMap<String, Measure>(), (tab, result) -> {
                            tab.getMeasures().forEach(measure -> {
                                if (!((Map<String, Measure>) result).containsKey(measure.getName())) {
                                    ((Map<String, Measure>) result).put(measure.getName(), measure);
                                }
                            });
                            return result;
                        }, false
                );
                table.setMeasures(new ArrayList<Measure>(measures.values()));

                Map<String, Dimension> dimensions = (Map<String, Dimension>) getInheritedAttribute(table,
                        new HashMap<String, Dimension>(), (tab, result) -> {
                            tab.getDimensions().forEach(dim -> {
                                if (!((Map<String, Dimension>) result).containsKey(dim.getName())) {
                                    ((Map<String, Dimension>) result).put(dim.getName(), dim);
                                }
                            });
                            return result;
                        }, false
                );
                table.setDimensions(new ArrayList<Dimension>(dimensions.values()));

                Map<String, Join> joins = (Map<String, Join>) getInheritedAttribute(table,
                        new HashMap<String, Join>(), (tab, result) -> {
                            tab.getJoins().forEach(dim -> {
                                if (!((Map<String, Join>) result).containsKey(dim.getName())) {
                                    ((Map<String, Join>) result).put(dim.getName(), dim);
                                }
                            });
                            return result;
                        }, false
                );
                table.setJoins(new ArrayList<Join>(joins.values()));

                String schema = (String) getInheritedAttribute(table, null,
                        (tab, result) -> {
                            return tab.getSchema();
                        }, true
                );
                table.setSchema(schema);

                String dbConnectionName = (String) getInheritedAttribute(table, null,
                        (tab, result) -> {
                            return tab.getDbConnectionName();
                        }, true
                );
                table.setDbConnectionName(dbConnectionName);

                String readAccess = (String) getInheritedAttribute(table, null,
                        (tab, result) -> {
                            return tab.getReadAccess();
                        }, true
                );
                table.setReadAccess(readAccess);

                String sql = (String) getInheritedAttribute(table, null,
                        (tab, result) -> {
                            return tab.getSql();
                        }, true
                );
                table.setSql(sql);

                String tableName = (String) getInheritedAttribute(table, null,
                        (tab, result) -> {
                            return tab.getTable();
                        }, true
                );
                table.setTable(tableName);
            }
        }
    }


    private Object getInheritedAttribute(Table table, Object result, Inheritance action, boolean recurseOnlyIfNull) {
        Object newResult = action.inherit(table, result);
        boolean recurse = true;
        if (recurseOnlyIfNull && newResult != null) {
            recurse = false;
        }
        if (table.hasParent()) {
            newResult = getInheritedAttribute(table.getParent(this.elideTableConfig), newResult, action, recurse);
        }
        return newResult;
    }

    @FunctionalInterface
    public interface Inheritance {
        public Object inherit(Table childTable, Object result);
    }

    /**
     * Add all Hjson resources under configDir in resourceMap.
     * @throws IOException
     */
    private void loadConfigMap() throws IOException {
        int configDirURILength = resolver.getResources(this.configDir)[0].getURI().toString().length();

        Resource[] hjsonResources = resolver.getResources(this.configDir + HJSON_EXTN);
        for (Resource resource : hjsonResources) {
            this.resourceMap.put(resource.getURI().toString().substring(configDirURILength), resource);
        }
    }

    /**
     * Read variable file config.
     * @param config Config Enum
     * @return Map<String, Object> A map containing all the variables if variable config exists else empty map
     */
    private Map<String, Object> readVariableConfig(Config config) {

        return this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(config.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                return DynamicConfigHelpers.stringToVariablesPojo(entry.getValue().getFilename(),
                                                content, schemaValidator);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .findFirst()
                        .orElse(new HashMap<>());
    }

    /**
     * Read and validates security config file.
     */
    private ElideSecurityConfig readSecurityConfig() {

        return this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.SECURITY.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                validateConfigForMissingVariables(content, this.modelVariables);
                                return DynamicConfigHelpers.stringToElideSecurityPojo(entry.getValue().getFilename(),
                                                content, this.modelVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .findAny()
                        .orElse(new ElideSecurityConfig());
    }

    /**
     * Read and validates db config files.
     * @return Set<DBConfig> Set of SQL DB Configs
     */
    private Set<DBConfig> readDbConfig() {

        return this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.SQLDBConfig.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                validateConfigForMissingVariables(content, this.dbVariables);
                                return DynamicConfigHelpers.stringToElideDBConfigPojo(entry.getValue().getFilename(),
                                                content, this.dbVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .flatMap(dbconfig -> dbconfig.getDbconfigs().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Read and validates table config files.
     */
    private Set<Table> readTableConfig() {

        return this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.TABLE.getConfigPath()))
                        .map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                validateConfigForMissingVariables(content, this.modelVariables);
                                return DynamicConfigHelpers.stringToElideTablePojo(entry.getValue().getFilename(),
                                                content, this.modelVariables, schemaValidator);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .flatMap(table -> table.getTables().stream())
                        .collect(Collectors.toSet());
    }

    /**
     * Checks if neither Table nor DB config files provided.
     */
    private void validateRequiredConfigsProvided() {
        if (this.elideTableConfig.getTables().isEmpty() && this.elideSQLDBConfig.getDbconfigs().isEmpty()) {
            throw new IllegalStateException("Neither Table nor DB configs found under: " + this.configDir);
        }
    }

    /**
     * Extracts any handlebar variables in config file and checks if they are
     * defined in variable config. Throw exception for undefined variables.
     * @param config config file
     * @param variables A map of defined variables
     */
    private static void validateConfigForMissingVariables(String config, Map<String, Object> variables) {
        Matcher regexMatcher = HANDLEBAR_REGEX.matcher(config);
        while (regexMatcher.find()) {
            String str = regexMatcher.group(1).trim();
            if (!variables.containsKey(str)) {
                throw new IllegalStateException(str + " is used as a variable in either table or security config files "
                        + "but is not defined in variables config file.");
            }
        }
    }

    /**
     * Validate table configs.
     * @param elideTableConfig ElideTableConfig
     * @return boolean true if all provided table properties passes validation
     */
    private static boolean validateTableConfig(ElideTableConfig elideTableConfig) {
        for (Table table : elideTableConfig.getTables()) {

            validateSql(table.getSql());
            Set<String> tableFields = new HashSet<>();

            table.getDimensions().forEach(dim -> {
                validateFieldNameUniqueness(tableFields, dim.getName(), table.getName());
                validateSql(dim.getDefinition());
                validateTableSource(elideTableConfig, dim.getTableSource());
            });

            table.getMeasures().forEach(measure -> {
                validateFieldNameUniqueness(tableFields, measure.getName(), table.getName());
                validateSql(measure.getDefinition());
            });

            table.getJoins().forEach(join -> {
                validateFieldNameUniqueness(tableFields, join.getName(), table.getName());
                validateJoin(join, elideTableConfig);
            });
        }

        return true;
    }

    private static void validateFieldNameUniqueness(Set<String> alreadyFoundFields, String fieldName,
                    String tableName) {
        if (!alreadyFoundFields.add(fieldName.toLowerCase(Locale.ENGLISH))) {
            throw new IllegalStateException(String.format("Duplicate!! Field name: %s is not unique for table: %s",
                            fieldName, tableName));
        }
    }

    /**
     * Validates tableSource is in format: modelName.logicalColumnName and refers to a defined model and a defined
     * column with in that model.
     */
    private static void validateTableSource(ElideTableConfig elideTableConfig, String tableSource) {
        if (isNullOrEmpty(tableSource)) {
            return; // Nothing to validate
        }

        String[] split = tableSource.split("\\.");
        if (split.length != 2) {
            throw new IllegalStateException("Invalid tableSource : " + tableSource + " . "
                            + "More than one dot(.) found, tableSource must be in format: modelName.logicalColumnName");
        }
        String modelName = split[0];
        String fieldName = split[1];

        if (elideTableConfig.hasTable(modelName)) {
            Table table = elideTableConfig.getTable(modelName);
            if (!table.hasField(elideTableConfig, fieldName)) {
                throw new IllegalStateException("Invalid tableSource : " + tableSource + " . Field : " + fieldName
                                + " is undefined for hjson model: " + modelName);
            }
            return;
        }

        if (isStaticModel(modelName, NO_VERSION)) {
            if (!staticModelHasField(modelName, NO_VERSION, fieldName)) {
                throw new IllegalStateException("Invalid tableSource : " + tableSource + " . Field : " + fieldName
                                + " is undefined for non-hjson model: " + modelName);
            }
            return;
        }

        throw new IllegalStateException("Invalid tableSource : " + tableSource + " . Undefined model: " + modelName);
    }

    /**
     * Validates join clause does not refer to a Table which is not in the same DBConnection. If joined table is not
     * part of dynamic configuration, then ignore
     */
    private static void validateJoinedTablesDBConnectionName(ElideTableConfig elideTableConfig) {

        for (Table table : elideTableConfig.getTables()) {
            if (!table.getJoins().isEmpty()) {

                Set<String> joinedTables = table.getJoins()
                        .stream()
                        .map(join -> join.getTo())
                        .collect(Collectors.toSet());

                Set<String> connections = elideTableConfig.getTables()
                        .stream()
                        .filter(t -> joinedTables.contains(t.getName()))
                        .map(t -> t.getDbConnectionName())
                        .collect(Collectors.toSet());

                if (connections.size() > 1 || (connections.size() == 1
                                && !Objects.equals(table.getDbConnectionName(), connections.iterator().next()))) {
                    throw new IllegalStateException("DBConnection name mismatch between table: " + table.getName()
                                    + " and tables in its Join Clause.");
                }
            }
        }
    }

    /**
     * Validates table (or db connection) name is unique across all the dynamic table (or db connection) configs.
     */
    private static void validateNameUniqueness(Set<? extends Named> configs) {

        Set<String> names = new HashSet<>();
        configs.forEach(obj -> {
            if (!names.add(obj.getName().toLowerCase(Locale.ENGLISH))) {
                throw new IllegalStateException("Duplicate!! Either Table or DB configs found with the same name.");
            }
        });
    }

    /**
     * Check if input sql definition contains either semicolon or any of disallowed
     * keywords. Throw exception if check fails.
     */
    private static void validateSql(String sqlDefinition) {
        if (!DynamicConfigHelpers.isNullOrEmpty(sqlDefinition) && (sqlDefinition.contains(SEMI_COLON)
                || containsDisallowedWords(sqlDefinition, SQL_SPLIT_REGEX, SQL_DISALLOWED_WORDS))) {
            throw new IllegalStateException("sql/definition provided in table config contain either '" + SEMI_COLON
                    + "' or one of these words: " + Arrays.toString(SQL_DISALLOWED_WORDS.toArray()));
        }
    }

    /**
     * Check if input join definition is valid.
     */
    private static void validateJoin(Join join, ElideTableConfig elideTableConfig) {
        validateSql(join.getDefinition());

        String joinModelName = join.getTo();

        if (!(elideTableConfig.hasTable(joinModelName) || isStaticModel(joinModelName, NO_VERSION))) {
            throw new IllegalStateException(
                            "Model: " + joinModelName + " is neither included in dynamic models nor in static models");
        }

        Matcher matcher = REFERENCE_PARENTHESES.matcher(join.getDefinition());
        Set<String> references = new HashSet<>();
        while (matcher.find()) {
            references.add(matcher.group(1).trim());
        }

        if (references.size() < 2) {
            throw new IllegalStateException("Atleast 2 unique references are expected in join definition");
        }

        references.forEach(reference -> {
            if (reference.indexOf('.') != -1) {
                String joinField = reference.substring(0, reference.indexOf('.'));
                if (!joinField.equals(join.getName())) {
                    throw new IllegalStateException("Join name must be used before '.' in join definition. Found '"
                                    + joinField + "' instead of '" + join.getName() + "'");
                }
            }
        });
    }

    /**
     * Validate role name provided in security config.
     * @param elideSecurityConfig ElideSecurityConfig
     * @return boolean true if all role name passes validation else throw exception
     */
    private static boolean validateRoleInSecurityConfig(ElideSecurityConfig elideSecurityConfig) {
        for (String role : elideSecurityConfig.getRoles()) {
            if (containsDisallowedWords(role, ROLE_NAME_DISALLOWED_WORDS)) {
                throw new IllegalStateException("ROLE provided in security config contain one of these words: "
                        + Arrays.toString(ROLE_NAME_DISALLOWED_WORDS));
            }
        }
        return true;
    }

    /**
     * Checks if input string has any of the disallowed words.
     * @param str input string to validate
     * @param keywords Array of disallowed words
     * @return boolean true if input string does not contain any of the keywords
     *         else false
     */
    private static boolean containsDisallowedWords(String str, String[] keywords) {
        return Arrays.stream(keywords).anyMatch(str.toUpperCase(Locale.ENGLISH)::contains);
    }

    /**
     * Checks if any word in the input string matches any of the disallowed words.
     * @param str input string to validate
     * @param splitter regex for splitting input string
     * @param keywords Set of disallowed words
     * @return boolean true if any word in the input string matches any of the
     *         disallowed words else false
     */
    private static boolean containsDisallowedWords(String str, String splitter, Set<String> keywords) {
        return DynamicConfigHelpers.isNullOrEmpty(str) ? false
                : Arrays.stream(str.trim().toUpperCase(Locale.ENGLISH).split(splitter)).anyMatch(keywords::contains);
    }

    /**
     * Define Arguments.
     */
    private static final Options prepareOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "Print a help message and exit."));
        options.addOption(new Option("c", "configDir", true,
                "Path for Configs Directory.\n"
                        + "Expected Directory Structure under Configs Directory:\n"
                        + "./models/security.hjson(optional)\n"
                        + "./models/variables.hjson(optional)\n"
                        + "./models/tables/\n"
                        + "./models/tables/table1.hjson\n"
                        + "./models/tables/table2.hjson\n"
                        + "./models/tables/tableN.hjson\n"
                        + "./db/variables.hjson(optional)\n"
                        + "./db/sql/(optional)\n"
                        + "./db/sql/db1.hjson\n"
                        + "./db/sql/db2.hjson\n"
                        + "./db/sql/dbN.hjson\n"));

        return options;
    }

    /**
     * Print Help.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                "java -cp <Jar File> com.yahoo.elide.contrib.dynamicconfighelpers.validator.DynamicConfigValidator",
                options);
    }

    /**
     * Remove src/.../resources/ from filepath.
     * @param filePath
     * @return Path to model dir
     */
    public static String formatClassPath(String filePath) {
        if (filePath.indexOf(RESOURCES + File.separator) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES + File.separator) + RESOURCES_LENGTH + 1);
        } else if (filePath.indexOf(RESOURCES) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES) + RESOURCES_LENGTH);
        }
        return filePath;
    }
}
