/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.modelconfig.DynamicConfigHelpers.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.EntityPermissions;
import com.yahoo.elide.core.type.Type;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.modelconfig.Config;
import com.yahoo.elide.modelconfig.DynamicConfigHelpers;
import com.yahoo.elide.modelconfig.DynamicConfigSchemaValidator;
import com.yahoo.elide.modelconfig.DynamicConfiguration;
import com.yahoo.elide.modelconfig.model.Argument;
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

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
/**
 * Util class to validate and parse the config files. Optionally compiles config files.
 */
public class DynamicConfigValidator implements DynamicConfiguration {
    public static final Pattern REFERENCE_PARENTHESES = Pattern.compile("\\{\\{(.+?)}}");


    private static final Set<String> SQL_DISALLOWED_WORDS = new HashSet<>(
            Arrays.asList("DROP", "TRUNCATE", "DELETE", "INSERT", "UPDATE", "ALTER", "COMMENT", "CREATE", "DESCRIBE",
                    "SHOW", "USE", "GRANT", "REVOKE", "CONNECT", "LOCK", "EXPLAIN", "CALL", "MERGE", "RENAME"));
    private static final String SQL_SPLIT_REGEX = "\\s+";
    private static final String SEMI_COLON = ";";
    private static final Pattern HANDLEBAR_REGEX = Pattern.compile("<%(.*?)%>");
    private static final String RESOURCES = "resources";
    private static final int RESOURCES_LENGTH = 9; //"resources".length()
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String HJSON_EXTN = "**/*.hjson";
    private static final String SQL_HELPER_PREFIX = "sql ";
    private static final String COLUMN_ARGS_PREFIX = "$$column.args.";
    private static final String TABLE_ARGS_PREFIX = "$$table.args.";
    private static final String USER_CTX_PREFIX = "$$user.";
    private static final String REQUEST_CTX_PREFIX = "$$request.";
    private static final String TABLES_CTX_PREFIX = "$$tables.";

    // eg: sql table='tableName' column='columnName[arg1:value1][arg2:value2]'
    private static final Pattern SQL_HELPER_PATTERN = Pattern.compile("sql\\s+from='(.+)'\\s+column='(.+)'");
    // square brackets having non-empty argument name and  encoded agument value separated by ':'
    // eg: [abc:xyz] , [foo:bar][blah:Encoded+Value]
    private static final Pattern SQL_HELPER_COLUMN_ARGS_PATTERN = Pattern.compile("\\[(\\w+):([^\\]]+)\\]");
    // field name followed by zero or more filter arguments
    // eg: name, orderDate[grain:month] , title[foo:bar][blah:Encoded+Value]
    private static final Pattern SQL_HELPER_COLUMN_PATTERN =
                    Pattern.compile("(\\w+)(" + SQL_HELPER_COLUMN_ARGS_PATTERN + ")*$");

    @Getter private final ElideTableConfig elideTableConfig = new ElideTableConfig();
    @Getter private ElideSecurityConfig elideSecurityConfig;
    @Getter private Map<String, Object> modelVariables;
    private Map<String, Object> dbVariables;
    @Getter private final ElideDBConfig elideSQLDBConfig = new ElideSQLDBConfig();
    private final String configDir;
    private final DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
    private final Map<String, Resource> resourceMap = new HashMap<>();
    private final PathMatchingResourcePatternResolver resolver;
    private final EntityDictionary dictionary = new EntityDictionary(new HashMap<>());

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

        initialize();
    }

    private void initialize() {

        Set<Class<?>> annotatedClasses =
                        ClassScanner.getAnnotatedClasses(Arrays.asList(Include.class, SecurityCheck.class));

        annotatedClasses.forEach(cls -> {
            if (cls.getAnnotation(Include.class) != null) {
                dictionary.bindEntity(cls);
            } else {
                dictionary.addSecurityCheck(cls);
            }
        });
    }

    public static void main(String[] args) {
        Options options = prepareOptions();

        try {
            CommandLine cli = new DefaultParser().parse(options, args);

            if (cli.hasOption("help")) {
                printHelp(options);
                System.exit(0);
            }
            if (!cli.hasOption("configDir")) {
                printHelp(options);
                System.err.println("Missing required option");
                System.exit(1);
            }
            String configDir = cli.getOptionValue("configDir");

            DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(configDir);
            dynamicConfigValidator.readAndValidateConfigs();
            System.out.println("Configs Validation Passed!");
            System.exit(0);

        } catch (Exception e) {
            String msg = isNullOrEmpty(e.getMessage()) ? "Process Failed!" : e.getMessage();
            System.err.println(msg);
            System.exit(2);
        }
    }

    /**
     * Read and validate config files under config directory.
     * @throws IOException IOException
     */
    public void readAndValidateConfigs() throws IOException {
        this.loadConfigMap();
        this.modelVariables = readVariableConfig(Config.MODELVARIABLE);
        this.elideSecurityConfig = readSecurityConfig();
        validateSecurityConfig();
        this.dbVariables = readVariableConfig(Config.DBVARIABLE);
        this.elideSQLDBConfig.setDbconfigs(readDbConfig());
        this.elideTableConfig.setTables(readTableConfig());
        validateRequiredConfigsProvided();
        validateNameUniqueness(this.elideSQLDBConfig.getDbconfigs(), "Multiple DB configs found with the same name: ");
        validateNameUniqueness(this.elideTableConfig.getTables(), "Multiple Table configs found with the same name: ");
        populateInheritance(this.elideTableConfig);
        validateTableConfig();
        validateJoinedTablesDBConnectionName(this.elideTableConfig);
    }

    @Override
    public Set<Table> getTables() {
        return elideTableConfig.getTables();
    }

    @Override
    public Set<String> getRoles() {
        return elideSecurityConfig.getRoles();
    }

    @Override
    public Set<DBConfig> getDatabaseConfigurations() {
        return elideSQLDBConfig.getDbconfigs();
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

    private void populateInheritance(ElideTableConfig elideTableConfig) {
        //ensures validation is run before populate always.
        validateInheritance(this.elideTableConfig);

        Set<Table> processed = new HashSet<Table>();
        elideTableConfig.getTables().stream().forEach(table -> {
            populateInheritance(table, processed);
        });
    }

    private void populateInheritance(Table table, Set<Table> processed) {
        if (processed.contains(table)) {
            return;
        }

        processed.add(table);

        if (!table.hasParent()) {
            return;
        }

        Table parent = table.getParent(this.elideTableConfig);
        if (!processed.contains(parent)) {
            populateInheritance(parent, processed);
        }

        Map<String, Measure> measures = getInheritedMeasures(parent, attributesListToMap(table.getMeasures()));
        table.setMeasures(new ArrayList<Measure>(measures.values()));

        Map<String, Dimension> dimensions = getInheritedDimensions(parent, attributesListToMap(table.getDimensions()));
        table.setDimensions(new ArrayList<Dimension>(dimensions.values()));

        Map<String, Join> joins = getInheritedJoins(parent, attributesListToMap(table.getJoins()));
        table.setJoins(new ArrayList<Join>(joins.values()));

        String schema = getInheritedSchema(parent, table.getSchema());
        table.setSchema(schema);

        String dbConnectionName = getInheritedConnection(parent, table.getDbConnectionName());
        table.setDbConnectionName(dbConnectionName);

        String sql = getInheritedSql(parent, table.getSql());
        table.setSql(sql);

        String tableName = getInheritedTable(parent, table.getTable());
        table.setTable(tableName);
        // isFact, isHidden, ReadAccess have default Values in schema, so can not be inherited.
        // Other properties (tags, cardinality, etc.) have been categorized as non-inheritable too.
    }

    private <T extends Named> Map<String, T> attributesListToMap(List<T> attributes) {
        return attributes.stream().collect(Collectors.toMap(T::getName, attribute -> attribute));
    }

    @FunctionalInterface
    public interface Inheritance<T> {
        public T inherit();
    }

    private Map<String, Measure> getInheritedMeasures(Table table, Map<String, Measure> measures) {
        Inheritance action = () -> {
            table.getMeasures().forEach(measure -> {
                if (!measures.containsKey(measure.getName())) {
                    measures.put(measure.getName(), measure);
                }
            });
            return measures;
        };

        action.inherit();
        return measures;
    }

    private Map<String, Dimension> getInheritedDimensions(Table table, Map<String, Dimension> dimensions) {
        Inheritance action = () -> {
            table.getDimensions().forEach(dimension -> {
                if (!dimensions.containsKey(dimension.getName())) {
                    dimensions.put(dimension.getName(), dimension);
                }
            });
            return dimensions;
        };

        action.inherit();
        return dimensions;
    }

    private Map<String, Join> getInheritedJoins(Table table, Map<String, Join> joins) {
        Inheritance action = () -> {
            table.getJoins().forEach(join -> {
                if (!joins.containsKey(join.getName())) {
                    joins.put(join.getName(), join);
                }
            });
            return joins;
        };

        action.inherit();
        return joins;
    }

    private <T> T getInheritedAttribute(Inheritance action, T property) {
        return property == null ? (T) action.inherit() : property;
    }

    private String getInheritedSchema(Table table, String schema) {
        Inheritance action = () -> table.getSchema();

        return getInheritedAttribute(action, schema);
    }

    private String getInheritedConnection(Table table, String connection) {
        Inheritance action = () -> table.getDbConnectionName();

        return getInheritedAttribute(action, connection);
    }

    private String getInheritedSql(Table table, String sql) {
        Inheritance action = () -> table.getSql();

        return getInheritedAttribute(action, sql);
    }

    private String getInheritedTable(Table table, String tableName) {
        Inheritance action = () -> table.getTable();

        return getInheritedAttribute(action, tableName);
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
     * @return boolean true if all provided table properties passes validation
     */
    private boolean validateTableConfig() {
        Set<String> extractedChecks = new HashSet<>();
        PermissionExpressionVisitor visitor = new PermissionExpressionVisitor();

        for (Table table : elideTableConfig.getTables()) {

            validateSql(table.getSql());
            validateArguments(table.getArguments());
            Set<String> tableFields = new HashSet<>();

            table.getJoins().forEach(join -> {
                validateFieldNameUniqueness(tableFields, join.getName(), table.getName());
                validateSql(join.getDefinition());
                validateModelExists(join.getTo());
                if (join.getType() == Join.Type.CROSS && !isNullOrEmpty(join.getDefinition())) {
                    throw new IllegalStateException(String.format(
                                    "Join definition is not supported for Cross Join: %s in Model: %s",
                                    join.getName(), table.getName()));
                } else if (join.getType() != Join.Type.CROSS && isNullOrEmpty(join.getDefinition())) {
                    throw new IllegalStateException(String.format(
                                    "Join definition must be provided for Join: %s in Model: %s",
                                    join.getName(), table.getName()));
                }
            });

            table.getDimensions().forEach(dim -> {
                validateFieldNameUniqueness(tableFields, dim.getName(), table.getName());
                validateArguments(dim.getArguments());
                validateSql(dim.getDefinition());
                validateTableSource(dim.getTableSource());
                extractChecksFromExpr(dim.getReadAccess(), extractedChecks, visitor);
            });

            table.getMeasures().forEach(measure -> {
                validateFieldNameUniqueness(tableFields, measure.getName(), table.getName());
                validateArguments(measure.getArguments());
                validateSql(measure.getDefinition());
                extractChecksFromExpr(measure.getReadAccess(), extractedChecks, visitor);
            });

            extractChecksFromExpr(table.getReadAccess(), extractedChecks, visitor);
            validateChecks(extractedChecks);
        }

        // Verify definitions
        for (Table table : elideTableConfig.getTables()) {
            // First verify Join definitions.
            table.getJoins().forEach(join -> validateDefinition(table, join));
            table.getDimensions().forEach(dim -> validateDefinition(table, dim));
            table.getMeasures().forEach(measure -> validateDefinition(table, measure));
        }

        return true;
    }

    private void validateArguments(Set<Argument> arguments) {
        validateNameUniqueness(arguments, "Multiple Arguments found with the same name: ");
        arguments.forEach(arg -> {
            validateTableSource(arg.getTableSource());
        });
    }

    private void validateChecks(Set<String> checks) {

        if (checks.isEmpty()) {
            return; // Nothing to validate
        }

        Set<String> staticChecks = dictionary.getCheckIdentifiers();

        List<String> undefinedChecks = checks
                        .stream()
                        .filter(check -> !(elideSecurityConfig.hasCheckDefined(check) || staticChecks.contains(check)))
                        .sorted()
                        .collect(Collectors.toList());

        if (!undefinedChecks.isEmpty()) {
            throw new IllegalStateException("Found undefined security checks: " + undefinedChecks);
        }
    }

    private static void extractChecksFromExpr(String readAccess, Set<String> extractedChecks,
                    PermissionExpressionVisitor visitor) {
        if (!isNullOrEmpty(readAccess)) {
            ParseTree root = EntityPermissions.parseExpression(readAccess);
            extractedChecks.addAll(visitor.visit(root));
        }
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
    private void validateTableSource(String tableSource) {
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
            if (!table.hasField(fieldName)) {
                throw new IllegalStateException("Invalid tableSource : " + tableSource + " . Field : " + fieldName
                                + " is undefined for hjson model: " + modelName);
            }
            return;
        }

        if (isStaticModel(modelName, NO_VERSION)) {
            if (!isFieldInStaticModel(modelName, NO_VERSION, fieldName)) {
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
    private static void validateNameUniqueness(Set<? extends Named> configs, String errorMsg) {

        Set<String> names = new HashSet<>();
        configs.forEach(obj -> {
            if (!names.add(obj.getName().toLowerCase(Locale.ENGLISH))) {
                throw new IllegalStateException(errorMsg + obj.getName());
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

    private void validateDefinition(Table currentModel, Named currentColumn) {
        Set<String> references = getAllReferences(currentColumn.getDefinition());

        for (String reference : references) {

            // validate sql handlebar helper
            // {{sql from=<table/join name> column='<columnName>[<arg1>:<value1>][<argN>:<valueN>]'}}
            if (isSQLHelper(reference)) {
                validateSQLHelper(reference, currentModel, currentColumn);
                continue;
            }

            // validate arg1 exists when {{$$column.args.arg1}}
            if (reference.startsWith(COLUMN_ARGS_PREFIX)) {
                validateArgumentExists(currentModel, currentColumn, reference.substring(COLUMN_ARGS_PREFIX.length()));
                continue;
            }

            // validate arg1 exists when {{$$table.args.arg1}}
            if (reference.startsWith(TABLE_ARGS_PREFIX)) {
                validateArgumentExists(currentModel, reference.substring(TABLE_ARGS_PREFIX.length()));
                continue;
            }

            // Skip validation for $$user, $$request & $$tables
            if (reference.startsWith(USER_CTX_PREFIX) || reference.startsWith(REQUEST_CTX_PREFIX)
                            || reference.startsWith(TABLES_CTX_PREFIX)) {
                continue;
            }

            // Skip validation for physical column
            if (reference.indexOf('$') == 0) {
                continue;
            }

            // Joined reference eg: {{rates.conversionRate}}
            if (reference.indexOf('.') != -1) {
                validateJoinPath(reference, currentModel, currentColumn);
                continue;
            }

            // {{highScore}}
            validateLogicalColumn(reference, currentModel, currentColumn);
        }
    }

    private void validateJoinPath(String definition, Table currentModel, Named currentCol) {
        int dotIndex = definition.indexOf('.');
        String joinName = definition.substring(0, dotIndex);
        Join joinedVia;

        if ((currentCol instanceof Join && currentCol.getName().equals(joinName))
                        || (currentCol instanceof Join == false && currentModel.hasJoinField(joinName))) {
            joinedVia = currentModel.getJoin(joinName);

        // Name before '.' must match with any of the Join names
        } else {
            throw new IllegalArgumentException(String.format(
                            "Join name must be used before '.'. Found '%s' for Column: '%s' in Model: '%s'",
                            joinName, currentCol.getName(), currentModel.getName()));
        }

        // Any column args required in Join definition must be defined for current dimension/measure.
        validateCurrentColumnArgsAgainstColumnArgsInJoinDefinition(currentModel, currentCol, joinedVia);

        String invokedModelName = joinedVia.getTo();
        String invokedColName = definition.substring(dotIndex + 1);

        if (isStaticModel(invokedModelName, NO_VERSION)) {
            validateNonHJSONColumnInvocation(definition, currentModel, currentCol, invokedModelName, invokedColName);
            return;
        }

        if (this.elideTableConfig.hasTable(invokedModelName)) {
            validateHJSONColumnInvocation(definition, currentModel, currentCol, invokedModelName, invokedColName,
                            Collections.emptySet());
            return;
        }

        throw new IllegalArgumentException(String.format(
                        "Can't invoke '%s' provided in \"%s\" for Column: '%s' in Model: '%s'. "
                        + ". Undefined Model: '%s' ",
                        joinName, definition, currentCol.getName(), currentModel.getName(),
                        invokedModelName));
    }

    private void validateCurrentColumnArgsAgainstColumnArgsInJoinDefinition(Table currentModel, Named currentCol,
                    Join joinedVia) {
        if (currentCol == joinedVia) {
            return;
        }
        joinedVia.getRequiredColumnArgs().forEach(arg -> {
            if (!currentCol.hasArgument(arg)) {
                throw new IllegalArgumentException(String.format(
                                "Join: '%s' uses a column argument: '%s' in its definition in Model: '%s'. "
                                                + "This argument must be defined for column: '%s'",
                                joinedVia.getName(), arg, currentModel.getName(), currentCol.getName()));
            }
        });
    }

    private void validateLogicalColumn(String definition, Table currentModel, Named currentCol) {
        String invokedModelName = currentModel.getName();
        String invokedColName = definition;
        validateHJSONColumnInvocation(definition, currentModel, currentCol, invokedModelName, invokedColName,
                        Collections.emptySet());
    }

    private void validateSQLHelper(String definition, Table currentModel, Named currentCol) {
        Matcher sqlHandlebarMatcher = SQL_HELPER_PATTERN.matcher(definition);
        if (!sqlHandlebarMatcher.matches()) {
            throw new IllegalArgumentException(String.format("sql helper must be in format "
                            + "\"sql from='<table/join name>' column='<colName>[<arg1>:<val1>][<argN>:<val1N>]'\"."
                            + " Found %s instead", definition));
        }

        String fromStr = sqlHandlebarMatcher.group(1);
        String invokedModelName;

        if (currentModel.getName().equals(fromStr)) {
            invokedModelName = fromStr;

        // For Join definition, 'from' must match with column's name
        // For Dimension & Measure definition, 'from' must match with one of Join column's name
        } else if ((currentCol instanceof Join && currentCol.getName().equals(fromStr))
                        || (currentCol instanceof Join == false && currentModel.hasJoinField(fromStr))) {
            Join join = currentModel.getJoin(fromStr);
            invokedModelName = join.getTo();

        // Invoked Table name must match with either current table or any of the Join names
        } else {
            throw new IllegalArgumentException(String.format("Can't invoke '%s' provided in \"%s\". "
                            + "'From' must match with either current table name or any of the Join names", fromStr,
                            definition));
        }

        String columnStr = sqlHandlebarMatcher.group(2);
        String invokedColName;
        Set<String> fixedArgs = new HashSet<>();
        if (columnStr.indexOf('$') == 0) {
            invokedColName = columnStr;
        } else {
            Matcher sqlColumnMatcher = SQL_HELPER_COLUMN_PATTERN.matcher(columnStr);
            if (!sqlColumnMatcher.matches()) {
                throw new IllegalArgumentException(String.format("sql helper must be in format "
                                + "\"sql from='<table/join name>' column='<colName>[<arg1>:<val1>][<argN>:<val1N>]'\"."
                                + " Found \"%s\" instead", definition));
            }
            invokedColName = sqlColumnMatcher.group(1);
            parseFixedArguments(columnStr.substring(invokedColName.length()), fixedArgs);
        }

        if (isStaticModel(invokedModelName, NO_VERSION)) {
            validateNonHJSONColumnInvocation(definition, currentModel, currentCol, invokedModelName, invokedColName);
            return;
        }

        if (this.elideTableConfig.hasTable(invokedModelName)) {
            validateHJSONColumnInvocation(definition, currentModel, currentCol, invokedModelName, invokedColName,
                            fixedArgs);
            return;
        }

        throw new IllegalArgumentException(String.format("Can't invoke '%s' provided in \"%s\". Undefined Model: '%s' ",
                        fromStr, definition, invokedModelName));
    }

    private void validateNonHJSONColumnInvocation(String definition, Table currentModel, Named currentCol,
                    String invokedModelName, String invokedColName) {
        if (invokedColName.indexOf('$') == 0) {
            // No validation needed for physical column
            return;
        }

        // Validate column exists in static table.
        if (!isFieldInStaticModel(invokedModelName, NO_VERSION, invokedColName)) {
            throw new IllegalArgumentException(String.format(
                            "Can't invoke '%s' provided in \"%s\" for Column: '%s' in Model: '%s'. "
                                            + "Column: '%s' is undefined for non-hjson model: '%s'",
                            invokedColName, definition, currentCol.getName(), currentModel.getName(),
                            invokedColName, invokedModelName));
        }

        // TODO
        // Somehow verify column is invoked with correct arguments.
    }

    private void validateHJSONColumnInvocation(String definition, Table currentModel, Named currentCol,
                    String invokedModelName, String invokedColName, Set<String> fixedArgs) {
        if (invokedColName.indexOf('$') == 0) {
            // No validation needed for physical column
            return;
        }

        Table invokedTable = this.elideTableConfig.getTable(invokedModelName);
        if (!invokedTable.hasField(invokedColName)) {
            throw new IllegalArgumentException(String.format(
                            "Can't invoke '%s' provided in \"%s\" for Column: '%s' in Model: '%s'. "
                                            + "Column: '%s' is undefined for hjson model: '%s'",
                            invokedColName, definition, currentCol.getName(), currentModel.getName(),
                            invokedColName, invokedModelName));
        }

        Named invokedCol = invokedTable.getField(invokedColName);
        if (isSQLHelper(definition)) {
            validateCurrentColumnArgsAgainstInvokedColumnArgs(definition, currentCol, currentModel, invokedCol,
                            fixedArgs);
        } else {
            validateInvokedColumnArgsHaveDefaultValue(definition, currentCol, currentModel, invokedCol);
        }
    }

    private void validateCurrentColumnArgsAgainstInvokedColumnArgs(String definition, Named currentCol,
                    Table currentModel, Named invokedCol, Set<String> fixedArgs) {

        invokedCol.getArguments().forEach(invokedColArg -> {
            if (fixedArgs.contains(invokedColArg.getName())) {
                // Do nothing
                // May be somehow verify fixed argument value is of invokedColumn's type
            } else {
                // Find an argument for current column which matches with invoked column
                Optional<Argument> argument = currentCol.getArguments().stream()
                                .filter(currentColArg -> currentColArg.getName().equals(invokedColArg.getName())
                                                && currentColArg.getType().equals(invokedColArg.getType()))
                                .findFirst();
                if (!argument.isPresent()) {
                    throw new IllegalArgumentException(String.format(
                                    "Can't invoke '%s' provided in \"%s\" for Column: '%s' in Model: '%s'. "
                                                    + "Argument: '%s' with type: '%s' is required for column: '%s'",
                                    invokedCol.getName(), definition, currentCol.getName(), currentModel.getName(),
                                    invokedColArg.getName(), invokedColArg.getType(), currentCol.getName()));
                }
            }
        });
    }

    private void validateInvokedColumnArgsHaveDefaultValue(String definition, Named currentCol,
                    Table currentModel, Named invokedCol) {

        invokedCol.getArguments().forEach(invokedColArg -> {
            if (invokedColArg.getDefaultValue() == null) {
                throw new IllegalArgumentException(String.format(
                                "Can't invoke '%s' provided in \"%s\" for Column: '%s' in Model: '%s'. "
                                                + "Argument: '%s' for invoked Column: '%s' must have default value.",
                                invokedCol.getName(), definition, currentCol.getName(), currentModel.getName(),
                                invokedColArg.getName(), invokedCol.getName()));
            }
        });
    }

    /**
     * Validate role name provided in security config.
     * @return boolean true if all role name passes validation else throw exception
     */
    private boolean validateSecurityConfig() {
        Set<String> alreadyDefinedRoles = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        alreadyDefinedRoles.addAll(dictionary.getCheckIdentifiers());

        elideSecurityConfig.getRoles().forEach(role -> {
            if (alreadyDefinedRoles.contains(role)) {
                throw new IllegalStateException(String.format(
                                "Duplicate!! Role name: '%s' is already defined. Please use different role.", role));
            } else {
                alreadyDefinedRoles.add(role);
            }
        });

        return true;
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

    private void validateArgumentExists(Table currentModel, Named currentColumn, String argName) {
        // Arguments are not supported for Joins. Maintain a set of required Column arguments, and this set must be
        // validated for each logical column which references this join.
        if (currentColumn instanceof Join) {
            ((Join) currentColumn).getRequiredColumnArgs().add(argName);
            return;
        }
        if (!currentColumn.hasArgument(argName)) {
            throw new IllegalArgumentException(String.format(
                            "Argument: '%s' must be defined for Column: '%s' in Model: '%s'",
                            argName, currentColumn.getName(), currentModel.getName()));
        }
    }

    private void validateArgumentExists(Table currentModel, String argName) {
        if (!currentModel.hasArgument(argName)) {
            throw new IllegalArgumentException(String.format(
                            "Argument: '%s' must be defined for Model: '%s'", argName, currentModel.getName()));
        }
    }

    private void validateModelExists(String name) {
        if (!(elideTableConfig.hasTable(name) || isStaticModel(name, NO_VERSION))) {
            throw new IllegalStateException(
                            "Model: " + name + " is neither included in dynamic models nor in static models");
        }
    }

    private static Set<String> getAllReferences(String definition) {
        Set<String> references = new HashSet<>();

        if (isNullOrEmpty(definition)) {
            return references;
        }

        Matcher matcher = REFERENCE_PARENTHESES.matcher(definition);
        while (matcher.find()) {
            references.add(matcher.group(1).trim());
        }
        return references;
    }

    private static boolean isSQLHelper(String reference) {
        return reference.startsWith(SQL_HELPER_PREFIX);
    }

    private static void parseFixedArguments(String argString, Set<String> fixedArgs) {
        if (isNullOrEmpty(argString)) {
            return;
        }

        Matcher sqlColumnArgsMatcher = SQL_HELPER_COLUMN_ARGS_PATTERN.matcher(argString);
        while (sqlColumnArgsMatcher.find()) {
            fixedArgs.add(sqlColumnArgsMatcher.group(1));
        }
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
                        + "./models/tables/(optional)\n"
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
                "java -cp <Jar File> com.yahoo.elide.modelconfig.validator.DynamicConfigValidator",
                options);
    }

    /**
     * Remove src/.../resources/ from filepath.
     * @param filePath
     * @return Path to model dir
     */
    public static String formatClassPath(String filePath) {
        if (filePath.indexOf(RESOURCES + "/") > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES + "/") + RESOURCES_LENGTH + 1);
        } else if (filePath.indexOf(RESOURCES) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES) + RESOURCES_LENGTH);
        }
        return filePath;
    }

    private boolean isFieldInStaticModel(String modelName, String version, String fieldName) {
        Type<?> modelType = dictionary.getEntityClass(modelName, version);
        if (modelType == null) {
            return false;
        }

        try {
            return (modelType.getDeclaredField(fieldName) != null);
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private boolean isStaticModel(String modelName, String version) {
        Type<?> modelType = dictionary.getEntityClass(modelName, version);
        return modelType != null;
    }
}
