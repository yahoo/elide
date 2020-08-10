/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.yahoo.elide.contrib.dynamicconfighelpers.Config;
import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.DBConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Dimension;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideDBConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideNonSQLDBConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSQLDBConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Join;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Measure;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import com.google.common.collect.Sets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
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
    private static final int RESOURCE_LENGTH = 10; //"resources/".length()
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String HJSON_EXTN = "**/*.hjson";

    private ElideTableConfig elideTableConfig = new ElideTableConfig();
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> modelVariables;
    private Map<String, Object> dbVariables;
    private ElideDBConfig elideSQLDBConfig = new ElideSQLDBConfig();
    private ElideDBConfig elideNonSQLDBConfig = new ElideNonSQLDBConfig();
    private String configDir;
    private Map<String, Resource> resourceMap = new HashMap<>();

    public DynamicConfigValidator(String configDir) {
        File config = new File(configDir);

        if (config.exists()) {
            this.setConfigDir(FILEPATH_PATTERN + DynamicConfigHelpers.formatFilePath(config.getAbsolutePath()));
        } else {
            this.setConfigDir(CLASSPATH_PATTERN + DynamicConfigHelpers.formatFilePath(formatClassPath(configDir)));
        }
    }

    public static void main(String[] args) throws IOException, ParseException {

        Options options = prepareOptions();
        CommandLine cli = new DefaultParser().parse(options, args);

        if (cli.hasOption("help")) {
            printHelp(options);
            return;
        }
        if (!cli.hasOption("configDir")) {
            printHelp(options);
            throw new MissingOptionException("Missing required option");
        }
        String configDir = cli.getOptionValue("configDir");

        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(configDir);
        dynamicConfigValidator.readAndValidateConfigs();

        log.info("Configs Validation Passed!");
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
        this.elideSQLDBConfig.setDbconfigs(readDbConfig(Config.SQLDBConfig));
        this.elideNonSQLDBConfig.setDbconfigs(readDbConfig(Config.NONSQLDBConfig));
        validateDBConfigUniqueness(this.elideSQLDBConfig.getDbconfigs(), this.elideNonSQLDBConfig.getDbconfigs());
        this.elideTableConfig.setTables(readTableConfig());
        validateNameUniqueness(this.elideTableConfig.getTables());
        validateSqlInTableConfig(this.elideTableConfig);
        validateJoinedTablesDBConnectionName(this.elideTableConfig);
    }

    /**
     * Add all Hjson resources under configDir in resourceMap.
     * @throws IOException
     */
    private void loadConfigMap() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                this.getClass().getClassLoader());
        Resource[] hjsonResources = resolver.getResources(this.configDir + HJSON_EXTN);
        for (Resource resource : hjsonResources) {
            this.resourceMap.put(resource.getURI().toString().substring(configDir.length()), resource);
        }
    }

    /**
     * Read variable file config.
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
                                return DynamicConfigHelpers.stringToVariablesPojo(content);
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
                                return DynamicConfigHelpers.stringToElideSecurityPojo(content, this.modelVariables);
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
    private Set<DBConfig> readDbConfig(Config config) {

        return this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(config.getConfigPath())).map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                validateConfigForMissingVariables(content, this.dbVariables);
                                return DynamicConfigHelpers.stringToElideDBConfigPojo(content, this.dbVariables);
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

        Set<Table> tables = this.resourceMap
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().startsWith(Config.TABLE.getConfigPath())).map(entry -> {
                            try {
                                String content = IOUtils.toString(entry.getValue().getInputStream(), UTF_8);
                                validateConfigForMissingVariables(content, this.modelVariables);
                                return DynamicConfigHelpers.stringToElideTablePojo(content, this.modelVariables);
                            } catch (IOException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .flatMap(table -> table.getTables().stream())
                        .collect(Collectors.toSet());

        if (tables.isEmpty()) {
            throw new IllegalStateException(
                            "No Table configs found at: " + this.configDir + Config.TABLE.getConfigPath());
        }
        return tables;
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
     * Validate table sql and column definition provided in table configs.
     * @param elideTableConfig ElideTableConfig
     * @return boolean true if all sql/definition passes validation
     */
    private static boolean validateSqlInTableConfig(ElideTableConfig elideTableConfig) {
        for (Table table : elideTableConfig.getTables()) {
            validateSql(table.getSql());
            for (Dimension dim : table.getDimensions()) {
                validateSql(dim.getDefinition());
            }
            for (Join join : table.getJoins()) {
                validateSql(join.getDefinition());
            }
            for (Measure measure : table.getMeasures()) {
                validateSql(measure.getDefinition());
            }
        }
        return true;
    }

    /**
     * Validates join clause does not refer to a Table which is not in the same DBConnection.
     * If joined table is not part of dynamic configuration, then ignore
     */
    private static void validateJoinedTablesDBConnectionName(ElideTableConfig elideTableConfig) {

        for (Table table : elideTableConfig.getTables()) {
            if (!table.getJoins().isEmpty()) {

                Set<String> joinedTables = table.getJoins()
                        .stream()
                        .map(join -> join.getTo().toLowerCase(Locale.ENGLISH))
                        .collect(Collectors.toSet());

                Set<String> connections = elideTableConfig.getTables()
                        .stream()
                        .filter(t -> joinedTables.contains(t.getName().toLowerCase(Locale.ENGLISH)))
                        .map(t -> t.getDbConnectionName())
                        .collect(Collectors.toSet());

                if (connections.size() > 1 || (connections.size() == 1
                                && !table.getDbConnectionName().equals(connections.iterator().next()))) {
                    throw new IllegalStateException("DBConnection name mismatch between table: " + table.getName()
                                    + " and tables in its Join Clause.");
                }
            }
        }
    }

    /**
     * Validates DB configs are unique across SQL and NONSQL configs.
     */
    private void validateDBConfigUniqueness(Set<DBConfig> sqlDBConfigs, Set<DBConfig> nonSQLDBConfigs) {
        if (sqlDBConfigs.size() > 0 && nonSQLDBConfigs.size() > 0
                        && sqlDBConfigs.size() != Sets.difference(sqlDBConfigs, nonSQLDBConfigs).size()) {
            throw new IllegalStateException("Duplicate!! Same DB Config provided for both SQL and NonSQL.");
        }
        validateNameUniqueness(
                        Sets.union(this.elideSQLDBConfig.getDbconfigs(), this.elideNonSQLDBConfig.getDbconfigs()));
    }

    /**
     * Validates table(or db connection) name is unique across all the dynamic table (or db connection) configs.
     */
    private void validateNameUniqueness(Set<? extends Object> configs) {

        Set<String> names = new HashSet<>();

        configs.forEach(obj -> {
            if (obj instanceof Table) {
                String name = ((Table) obj).getName().toLowerCase(Locale.ENGLISH);
                if (!names.add(name)) {
                    throw new IllegalStateException(
                                    "Duplicate!! Table Configs have more than one table with same name.");
                }
            } else if (obj instanceof DBConfig) {
                String name = ((DBConfig) obj).getName().toLowerCase(Locale.ENGLISH);
                if (!names.add(name)) {
                    throw new IllegalStateException(
                                    "Duplicate!! DB Configs have more than one connection with same name.");
                }
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
     * @param String input string to validate
     * @param keywords Array of disallowed words
     * @return boolean true if input string does not contain any of the keywords
     *         else false
     */
    private static boolean containsDisallowedWords(String str, String[] keywords) {
        return Arrays.stream(keywords).anyMatch(str.toUpperCase(Locale.ENGLISH)::contains);
    }

    /**
     * Checks if any word in the input string matches any of the disallowed words.
     * @param String input string to validate
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
                        + "./db/sql/dbN.hjson\n"
                        + "./db/nonsql/(optional)\n"
                        + "./db/nonsql/db1.hjson\n"
                        + "./db/nonsql/dbN.hjson\n"));

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
    private String formatClassPath(String filePath) {
        if (filePath.indexOf(RESOURCES) > -1) {
            return filePath.substring(filePath.indexOf(RESOURCES) + RESOURCE_LENGTH);
        }
        return filePath;
    }
}
