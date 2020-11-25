/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.modelconfig.validator;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static com.yahoo.elide.modelconfig.DynamicConfigHelpers.getDataSource;
import static com.yahoo.elide.modelconfig.DynamicConfigHelpers.isNullOrEmpty;
import static com.yahoo.elide.modelconfig.parser.handlebars.HandlebarsHelper.EMPTY_STRING;
import static com.yahoo.elide.modelconfig.parser.handlebars.HandlebarsHelper.REFERENCE_PARENTHESES;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.yahoo.elide.annotation.Include;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.dictionary.EntityPermissions;
import com.yahoo.elide.core.utils.ClassScanner;
import com.yahoo.elide.modelconfig.Config;
import com.yahoo.elide.modelconfig.DBPasswordExtractor;
import com.yahoo.elide.modelconfig.DynamicConfigHelpers;
import com.yahoo.elide.modelconfig.DynamicConfigSchemaValidator;
import com.yahoo.elide.modelconfig.StaticModelsDetails;
import com.yahoo.elide.modelconfig.compile.ConnectionDetails;
import com.yahoo.elide.modelconfig.compile.ElideDynamicInMemoryCompiler;
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
import com.yahoo.elide.modelconfig.parser.handlebars.HandlebarsHydrator;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
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

@Slf4j
/**
 * Util class to validate and parse the config files. Optionally compiles config files.
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
    private static final String MODEL_PACKAGE_NAME = "dynamicconfig.models.";
    private static final String SECURITY_PACKAGE_NAME = "dynamicconfig.checks.";

    @Getter private final ElideTableConfig elideTableConfig = new ElideTableConfig();
    @Getter private ElideSecurityConfig elideSecurityConfig;
    @Getter private Map<String, Object> modelVariables;
    private Map<String, Object> dbVariables;
    private final ElideDBConfig elideSQLDBConfig = new ElideSQLDBConfig();
    private final String configDir;
    private final DynamicConfigSchemaValidator schemaValidator = new DynamicConfigSchemaValidator();
    private final Map<String, Resource> resourceMap = new HashMap<>();
    private final PathMatchingResourcePatternResolver resolver;
    private final EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
    private final StaticModelsDetails staticModelDetails = new StaticModelsDetails();

    @Getter private Map<String, Class<?>> compiledObjects;
    @Getter private final Map<String, ConnectionDetails> connectionDetailsMap = new HashMap<>();

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
                staticModelDetails.add(dictionary, cls);
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

            if (cli.hasOption("nocompile")) {
                System.out.println("Skipped compilation for both Model and DB configs");
                System.exit(0);
            }

            if (cli.hasOption("nomodelcompile")) {
                System.out.println("Skipped compilation for Model configs");
            } else {
                System.out.println("Compiling Model configs (Use '--nomodelcompile' to skip this step).");
                dynamicConfigValidator.hydrateAndCompileModelConfigs();
                System.out.println("Model Configs Compilation Passed!");
            }

            if (cli.hasOption("nodbcompile")) {
                System.out.println("Skipped compilation for DB configs");
            } else {
                System.out.println("Compiling DB configs (Use '--nodbcompile' to skip this step).");
                dynamicConfigValidator.compileDBConfigs();
                System.out.println("DB Configs Compilation Passed!");
            }

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
        validateRoleInSecurityConfig(this.elideSecurityConfig);
        this.dbVariables = readVariableConfig(Config.DBVARIABLE);
        this.elideSQLDBConfig.setDbconfigs(readDbConfig());
        this.elideTableConfig.setTables(readTableConfig());
        validateRequiredConfigsProvided();
        validateNameUniqueness(this.elideSQLDBConfig.getDbconfigs());
        validateNameUniqueness(this.elideTableConfig.getTables());
        validateInheritance(this.elideTableConfig);
        populateInheritance(this.elideTableConfig);
        validateTableConfig();
        validateJoinedTablesDBConnectionName(this.elideTableConfig);
    }

    public void hydrateAndCompileModelConfigs() throws Exception {
        hydrateAndCompileModelConfigs(ElideDynamicInMemoryCompiler.newInstance().ignoreWarnings());
    }

    public void hydrateAndCompileModelConfigs(ElideDynamicInMemoryCompiler compiler) throws Exception {
        HandlebarsHydrator hydrator = new HandlebarsHydrator(staticModelDetails);
        Map<String, String> tableClasses = hydrator.hydrateTableTemplate(this.elideTableConfig);
        Map<String, String> securityClasses = hydrator.hydrateSecurityTemplate(this.elideSecurityConfig);

        compiler.useParentClassLoader(getClass().getClassLoader());

        for (Map.Entry<String, String> tablePojo : tableClasses.entrySet()) {
            log.debug("key: " + tablePojo.getKey() + ", value: " + tablePojo.getValue());
            compiler.addSource(MODEL_PACKAGE_NAME + tablePojo.getKey(), tablePojo.getValue());
        }

        for (Map.Entry<String, String> secPojo : securityClasses.entrySet()) {
            log.debug("key: " + secPojo.getKey() + ", value: " + secPojo.getValue());
            compiler.addSource(SECURITY_PACKAGE_NAME + secPojo.getKey(), secPojo.getValue());
        }

        compiledObjects = compiler.compileAll();
    }

    public void compileDBConfigs() {
        compileDBConfigs(new DBPasswordExtractor() {
            @Override
            public String getDBPassword(DBConfig config) {
                return EMPTY_STRING;
            }
        });
    }

    public void compileDBConfigs(DBPasswordExtractor dbPasswordExtractor) {
        this.elideSQLDBConfig.getDbconfigs().forEach(config -> {
            connectionDetailsMap.put(config.getName(),
                            new ConnectionDetails(getDataSource(config, dbPasswordExtractor), config.getDialect()));
        });
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
        for (Table table : elideTableConfig.getTables()) {
            if (table.hasParent()) {
                Map<String, Measure> measures = getInheritedMeasures(table, new HashMap<String, Measure>());
                table.setMeasures(new ArrayList<Measure>(measures.values()));

                Map<String, Dimension> dimensions = getInheritedDimensions(table, new HashMap<String, Dimension>());
                table.setDimensions(new ArrayList<Dimension>(dimensions.values()));

                Map<String, Join> joins = getInheritedJoins(table, new HashMap<String, Join>());
                table.setJoins(new ArrayList<Join>(joins.values()));
            }
        }
    }

    private Map<String, Measure> getInheritedMeasures(Table table, Map<String, Measure> measures) {
        table.getMeasures().forEach(m -> {
            if (!measures.containsKey(m.getName())) {
                measures.put(m.getName(), m);
            }
        });
        if (table.hasParent()) {
            getInheritedMeasures(table.getParent(this.elideTableConfig), measures);
        }
        return measures;
    }

    private Map<String, Dimension> getInheritedDimensions(Table table, Map<String, Dimension> dimensions) {
        table.getDimensions().forEach(dim -> {
            if (!dimensions.containsKey(dim.getName())) {
                dimensions.put(dim.getName(), dim);
            }
        });
        if (table.hasParent()) {
            getInheritedDimensions(table.getParent(this.elideTableConfig), dimensions);
        }
        return dimensions;
    }

    private Map<String, Join> getInheritedJoins(Table table, Map<String, Join> joins) {
        table.getJoins().forEach(join -> {
            if (!joins.containsKey(join.getName())) {
                joins.put(join.getName(), join);
            }
        });
        if (table.hasParent()) {
            getInheritedJoins(table.getParent(this.elideTableConfig), joins);
        }
        return joins;
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
    private boolean validateTableConfig() {
        Set<String> extractedChecks = new HashSet<>();
        for (Table table : elideTableConfig.getTables()) {

            validateSql(table.getSql());
            Set<String> tableFields = new HashSet<>();

            table.getDimensions().forEach(dim -> {
                validateFieldNameUniqueness(tableFields, dim.getName(), table.getName());
                validateSql(dim.getDefinition());
                validateTableSource(dim.getTableSource());
                extractChecksFromExpr(dim.getReadAccess(), extractedChecks);
            });

            table.getMeasures().forEach(measure -> {
                validateFieldNameUniqueness(tableFields, measure.getName(), table.getName());
                validateSql(measure.getDefinition());
                extractChecksFromExpr(measure.getReadAccess(), extractedChecks);
            });

            table.getJoins().forEach(join -> {
                validateFieldNameUniqueness(tableFields, join.getName(), table.getName());
                validateJoin(join);
            });

            extractChecksFromExpr(table.getReadAccess(), extractedChecks);
            validateChecks(extractedChecks);
        }

        return true;
    }

    private void validateChecks(Set<String> checks) {

        if (checks.isEmpty()) {
            return; // Nothing to validate
        }

        Set<String> staticChecks = dictionary.getCheckMappings().keySet();

        List<String> undefinedChecks = checks
                        .stream()
                        .filter(check -> !(elideSecurityConfig.hasCheckDefined(check) || staticChecks.contains(check)))
                        .collect(Collectors.toList());

        if (!undefinedChecks.isEmpty()) {
            throw new IllegalStateException("Found undefined security checks: " + undefinedChecks);
        }
    }

    private static void extractChecksFromExpr(String readAccess, Set<String> extractedChecks) {

        if (!isNullOrEmpty(readAccess)) {
            ParseTree root = EntityPermissions.parseExpression(readAccess);
            extractChecksFromTree(root, extractedChecks);
        }
    }

    private static void extractChecksFromTree(ParseTree root, Set<String> extractedChecks) {

        if (root == null || root.getChildCount() == 0) {
            return;
        }

        boolean allChildrenAreTerminalNodes = true;
        for (int i = 0; i < root.getChildCount(); i++) {
            if (!(root.getChild(i) instanceof TerminalNodeImpl)) {
                allChildrenAreTerminalNodes = false;
                extractChecksFromTree(root.getChild(i), extractedChecks);
            }
        }

        if (allChildrenAreTerminalNodes) {
            extractedChecks.add(root.getText());
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
            if (!table.hasField(elideTableConfig, fieldName)) {
                throw new IllegalStateException("Invalid tableSource : " + tableSource + " . Field : " + fieldName
                                + " is undefined for hjson model: " + modelName);
            }
            return;
        }

        if (staticModelDetails.exists(modelName, NO_VERSION)) {
            if (!staticModelDetails.hasField(modelName, NO_VERSION, fieldName)) {
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
    private void validateJoin(Join join) {
        validateSql(join.getDefinition());

        String joinModelName = join.getTo();

        if (!(elideTableConfig.hasTable(joinModelName) || staticModelDetails.exists(joinModelName, NO_VERSION))) {
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
        options.addOption(new Option("nocompile", "nocompile", false, "Do not compile Model and DB configs."));
        options.addOption(new Option("nodbcompile", "nodbcompile", false, "Do not compile DB configs."));
        options.addOption(new Option("nomodelcompile", "nomodelcompile", false, "Do not compile Model configs."));
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
        if (filePath.indexOf(RESOURCES + File.separator) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES + File.separator) + RESOURCES_LENGTH + 1);
        } else if (filePath.indexOf(RESOURCES) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES) + RESOURCES_LENGTH);
        }
        return filePath;
    }
}
