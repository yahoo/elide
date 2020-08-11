/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

<<<<<<< HEAD
=======
import com.yahoo.elide.contrib.dynamicconfighelpers.Config;
>>>>>>> elide-5.x
import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Dimension;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Join;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Measure;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

<<<<<<< HEAD
import com.fasterxml.jackson.core.JsonProcessingException;

=======
>>>>>>> elide-5.x
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
<<<<<<< HEAD
import org.apache.commons.io.FileUtils;
=======
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
>>>>>>> elide-5.x

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
<<<<<<< HEAD
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
=======
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
>>>>>>> elide-5.x
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Data
/**
 * Util class to validate and parse the model config files.
 */
public class DynamicConfigValidator {

    private static final Set<String> SQL_DISALLOWED_WORDS = new HashSet<>(
            Arrays.asList("DROP", "TRUNCATE", "DELETE", "INSERT", "UPDATE", "ALTER", "COMMENT", "CREATE", "DESCRIBE",
                    "SHOW", "USE", "GRANT", "REVOKE", "CONNECT", "LOCK", "EXPLAIN", "CALL", "MERGE", "RENAME"));
    private static final String[] ROLE_NAME_DISALLOWED_WORDS = new String[] { "," };
    private static final String SQL_SPLIT_REGEX = "\\s+";
    private static final String SEMI_COLON = ";";
    private static final Pattern HANDLEBAR_REGEX = Pattern.compile("<%(.*?)%>");
<<<<<<< HEAD

    private ElideTableConfig elideTableConfig;
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> variables;
    private String configDir;
=======
    private static final String RESOURCES = "resources";
    private static final int RESOURCE_LENGTH = 10; //"resources/".length()
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String FILEPATH_PATTERN = "file:";
    private static final String HJSON_EXTN = "**/*.hjson";

    private ElideTableConfig elideTableConfig = new ElideTableConfig();
    private ElideSecurityConfig elideSecurityConfig = new ElideSecurityConfig();
    private Map<String, Object> variables = new HashMap<>();;
    private String configDir;
    private Map<String, Resource> resourceMap = new HashMap<>();

    public DynamicConfigValidator(String configDir) {
        File config = new File(configDir);

        if (config.exists()) {
            this.setConfigDir(FILEPATH_PATTERN + DynamicConfigHelpers.formatFilePath(config.getAbsolutePath())
                + HJSON_EXTN);
        } else {
            this.setConfigDir(CLASSPATH_PATTERN + DynamicConfigHelpers.formatFilePath(formatClassPath(configDir))
                + HJSON_EXTN);
        }
    }
>>>>>>> elide-5.x

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
<<<<<<< HEAD
        File file = new File(configDir);
        String absoluteBasePath = file.getAbsolutePath();
        log.info("Absolute Path for Model Configs Directory: " + absoluteBasePath);

        if (!file.isDirectory()) {
            throw new IllegalStateException("Model Configs Directory doesn't exists");
        }

        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator();
        dynamicConfigValidator.readAndValidateConfigs(absoluteBasePath);
=======

        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator(configDir);
        dynamicConfigValidator.readAndValidateConfigs();
>>>>>>> elide-5.x

        log.info("Configs Validation Passed!");
    }

    /**
<<<<<<< HEAD
     * Read and validate config files under config directory
     * @param filePath path for config directory
     * @throws IOException IOException
     */
    public void readAndValidateConfigs(String filePath) throws IOException {

        this.setConfigDir(DynamicConfigHelpers.formatFilePath(filePath));
        this.readVariableConfig();
        if (this.readSecurityConfig()) {
            validateRoleInSecurityConfig(this.getElideSecurityConfig());
        }
        if (this.readTableConfig()) {
            validateSqlInTableConfig(this.getElideTableConfig());
=======
     * Read and validate config files under config directory.
     * @throws IOException IOException
     */
    public void readAndValidateConfigs() throws IOException {
        this.loadConfigMap();
        this.readVariableConfig();
        this.readSecurityConfig();
        this.readTableConfig();
    }

    /**
     * Add all Hjson resources under configDir in resourceMap.
     * @throws IOException
     */
    private void loadConfigMap() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                this.getClass().getClassLoader());
        Resource[] modelResources = resolver.getResources(this.configDir);
        for (Resource resource : modelResources) {
            this.resourceMap.put(resource.getFilename(), resource);
>>>>>>> elide-5.x
        }
    }

    /**
     * Read variable file config.
     * @return boolean true if variable config file exists else false
<<<<<<< HEAD
     * @throws JsonProcessingException
     */
    private boolean readVariableConfig() throws JsonProcessingException {
        boolean isVariableConfig = exists(this.configDir + DynamicConfigHelpers.VARIABLE_CONFIG_PATH);
        this.variables = isVariableConfig ? DynamicConfigHelpers.getVariablesPojo(this.configDir)
                : Collections.<String, Object>emptyMap();
        return isVariableConfig;
=======
     * @throws IOException
     */
    private void readVariableConfig() throws IOException {
        String key = Config.VARIABLE.getConfigPath();
        if (this.resourceMap.containsKey(key)) {
            String content = IOUtils.toString(this.resourceMap.get(key).getInputStream(), StandardCharsets.UTF_8);
            this.setVariables(DynamicConfigHelpers.stringToVariablesPojo(content));
            this.resourceMap.remove(Config.VARIABLE.getConfigPath());
        }
>>>>>>> elide-5.x
    }

    /**
     * Read security config file and checks for any missing Handlebar variables.
     * @return boolean true if security config file exists else false
     * @throws IOException
     */
<<<<<<< HEAD
    private boolean readSecurityConfig() throws IOException {
        String securityConfigPath = this.configDir + DynamicConfigHelpers.SECURITY_CONFIG_PATH;
        boolean isSecurityConfig = exists(securityConfigPath);
        if (isSecurityConfig) {
            String securityConfigContent = DynamicConfigHelpers.readConfigFile(new File(securityConfigPath));
            validateConfigForMissingVariables(securityConfigContent, this.variables);
            this.elideSecurityConfig = DynamicConfigHelpers.stringToElideSecurityPojo(securityConfigContent,
                    this.variables);
        }
        return isSecurityConfig;
=======
    private void readSecurityConfig() throws IOException {
        String key = Config.SECURITY.getConfigPath();
        if (this.resourceMap.containsKey(key)) {
            String content = IOUtils.toString(this.resourceMap.get(key).getInputStream(), StandardCharsets.UTF_8);
            validateConfigForMissingVariables(content, this.variables);
            this.setElideSecurityConfig(DynamicConfigHelpers.stringToElideSecurityPojo(content, this.variables));
            validateRoleInSecurityConfig(this.getElideSecurityConfig());
            this.resourceMap.remove(Config.SECURITY.getConfigPath());
        }
>>>>>>> elide-5.x
    }

    /**
     * Read table config files and checks for any missing Handlebar variables.
<<<<<<< HEAD
     * @return boolean true if table config directory exists else false
     * @throws IOException
     */
    private boolean readTableConfig() throws IOException {
        String tableConfigsPath = this.configDir + DynamicConfigHelpers.TABLE_CONFIG_PATH;
        boolean isTableConfig = exists(tableConfigsPath);
        if (isTableConfig) {
            Collection<File> tableConfigs = FileUtils.listFiles(new File(tableConfigsPath), new String[] { "hjson" },
                    false);
            if (tableConfigs.isEmpty()) {
                throw new IllegalStateException("No Table Configs found at location: " + tableConfigsPath);
            }
            Set<Table> tables = new HashSet<>();
            for (File tableConfig : tableConfigs) {
                String tableConfigContent = DynamicConfigHelpers.readConfigFile(tableConfig);
                validateConfigForMissingVariables(tableConfigContent, this.variables);
                ElideTableConfig table = DynamicConfigHelpers.stringToElideTablePojo(tableConfigContent, variables);
                tables.addAll(table.getTables());
            }
            ElideTableConfig elideTableConfig = new ElideTableConfig();
            elideTableConfig.setTables(tables);
            this.elideTableConfig = elideTableConfig;
        } else {
            throw new IllegalStateException("Table Configs Directory doesn't exists at location: " + tableConfigsPath);
        }
        return isTableConfig;
    }

    /**
     * Check if file or directory exists.
     * @param filePath path of the file or directory
     * @return boolean true if file or directory exists else false
     */
    private static boolean exists(String filePath) {
        return new File(filePath).exists();
=======
     * @throws IOException
     */
    private void readTableConfig() throws IOException {
        Set<Table> tables = new HashSet<>();
        if (this.resourceMap.isEmpty()) {
            throw new IllegalStateException("No Table configs found at: " + this.configDir);
        }
        for (Entry<String, Resource> entry : this.resourceMap.entrySet()) {
            String content = IOUtils.toString(entry.getValue().getInputStream(), StandardCharsets.UTF_8);
            validateConfigForMissingVariables(content, this.variables);
            ElideTableConfig table = DynamicConfigHelpers.stringToElideTablePojo(content, this.variables);
            tables.addAll(table.getTables());
        }
        this.elideTableConfig.setTables(tables);
        validateSqlInTableConfig(this.elideTableConfig);
>>>>>>> elide-5.x
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
                "Path for Model Configs Directory.\n"
<<<<<<< HEAD
                + "Expected Directory Structure:\n"
                + "./security.hjson(optional)\n"
                + "./variables.hjson(optional)\n"
                + "./tables/\n"
                + "./tables/table1.hjson\n"
                + "./tables/table2.hjson\n"
                + "./tables/tableN.hjson\n"));
=======
                        + "Expected Directory Structure:\n"
                        + "./security.hjson(optional)\n"
                        + "./variables.hjson(optional)\n"
                        + "./tables/\n"
                        + "./tables/table1.hjson\n"
                        + "./tables/table2.hjson\n"
                        + "./tables/tableN.hjson\n"));
>>>>>>> elide-5.x
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
<<<<<<< HEAD
=======

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
>>>>>>> elide-5.x
}
