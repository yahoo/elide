/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

import com.yahoo.elide.contrib.dynamicconfighelpers.Config;
import com.yahoo.elide.contrib.dynamicconfighelpers.DynamicConfigHelpers;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Dimension;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideSecurityConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.ElideTableConfig;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Join;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Measure;
import com.yahoo.elide.contrib.dynamicconfighelpers.model.Table;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    private static final String RESOURCES = "resources";
    private static final int RESOURCE_LENGTH = 10; //"resources/".length()
    private static final String CLASSPATH_PATTERN = "classpath*:";
    private static final String HJSON_EXTN = "*.hjson";

    private ElideTableConfig elideTableConfig;
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> variables;
    private String configDir;
    private boolean configInClassPath;

    public DynamicConfigValidator(String configDir) {
        File config = new File(configDir);

        if (config.exists()) {
            this.setConfigDir(DynamicConfigHelpers.formatFilePath(config.getAbsolutePath()));
        } else {
            this.setConfigDir(DynamicConfigHelpers.formatFilePath(formatClassPath(configDir)));
            this.setConfigInClassPath(true);
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

        if (this.configInClassPath) {
            readAndValidateClasspathConfigs();
        } else {
            this.readVariableConfig();
            if (this.readSecurityConfig()) {
                validateRoleInSecurityConfig(this.getElideSecurityConfig());
            }
            if (this.readTableConfig()) {
                validateSqlInTableConfig(this.getElideTableConfig());
            }
        }
    }

    /**
     * Read and validate config files under resources.
     * @throws IOException
     */
    public void readAndValidateClasspathConfigs() throws IOException {
        log.info("config dir " + this.getConfigDir());
        configExists();

        String variableConfig = readResource(this.configDir + Config.VARIABLE.getConfigPath());
        if (variableConfig != null) {
            this.variables = DynamicConfigHelpers.stringToVariablesPojo(variableConfig);
        }
        String securityConfig = readResource(this.configDir + Config.SECURITY.getConfigPath());
        if (securityConfig != null) {
            this.setElideSecurityConfig(DynamicConfigHelpers.stringToElideSecurityPojo(securityConfig, this.variables));
            validateRoleInSecurityConfig(this.getElideSecurityConfig());
        }
        this.setElideTableConfig(getTableConfig(this.configDir + Config.TABLE.getConfigPath()));
        validateSqlInTableConfig(this.getElideTableConfig());
    }

    /**
     * Check if the config exists in classpath.
     */
    private void configExists() {
        URL url = this.getClass().getClassLoader().getResource(this.configDir);
        if (url == null) {
            throw new IllegalStateException("Model Configs Directory doesn't exists");
        }
    }

    /**
     * Read table config files and checks for any missing Handlebar variables.
     * @param tablePath : Path to the table config
     * @return ElideTableConfig
     * @throws IOException
     */
    private ElideTableConfig getTableConfig(String tablePath) throws IOException {
        Set<Table> tables = new HashSet<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
        Resource[] tableResources = resolver.getResources(CLASSPATH_PATTERN + tablePath + HJSON_EXTN);
        for (Resource resource : tableResources) {
            log.info("reading table config : " + tablePath + resource.getFilename());
            String content = readResource(tablePath + resource.getFilename());
            validateConfigForMissingVariables(content, this.variables);
            ElideTableConfig table = DynamicConfigHelpers.stringToElideTablePojo(content, this.variables);
            tables.addAll(table.getTables());
        }
        ElideTableConfig elideTableConfig = new ElideTableConfig();
        elideTableConfig.setTables(tables);
        return elideTableConfig;
    }

    /**
     * Read config from classpath.
     * @param resourcePath : path to resource
     * @return content of resource
     * @throws IOException
     */
    private String readResource(String resourcePath) throws IOException {
        InputStream stream = null;
        BufferedReader reader = null;
        String content = null;
        try {
            stream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(stream));
            content =  reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (reader != null) {
                reader.close();
            }
        }
        return content;
    }

    /**
     * Read variable file config.
     * @return boolean true if variable config file exists else false
     * @throws IOException
     */
    private boolean readVariableConfig() throws IOException {
        boolean isVariableConfig = new File(this.configDir + Config.VARIABLE.getConfigPath()).exists();
        this.variables = isVariableConfig ? DynamicConfigHelpers.getVariablesPojo(this.configDir)
                : Collections.<String, Object>emptyMap();
        return isVariableConfig;
    }

    /**
     * Read security config file and checks for any missing Handlebar variables.
     * @return boolean true if security config file exists else false
     * @throws IOException
     */
    private boolean readSecurityConfig() throws IOException {
        String securityConfigPath = this.configDir + Config.SECURITY.getConfigPath();
        boolean isSecurityConfig = new File(securityConfigPath).exists();
        if (isSecurityConfig) {
            String securityConfigContent = DynamicConfigHelpers.readConfigFile(new File(securityConfigPath));
            validateConfigForMissingVariables(securityConfigContent, this.variables);
            this.elideSecurityConfig = DynamicConfigHelpers.stringToElideSecurityPojo(securityConfigContent,
                    this.variables);
        }
        return isSecurityConfig;
    }

    /**
     * Read table config files and checks for any missing Handlebar variables.
     * @return boolean true if table config directory exists else false
     * @throws IOException
     */
    private boolean readTableConfig() throws IOException {
        String tableConfigsPath = this.configDir + Config.TABLE.getConfigPath();
        boolean isTableConfig = new File(tableConfigsPath).exists();
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
                        + "Expected Directory Structure:\n"
                        + "./security.hjson(optional)\n"
                        + "./variables.hjson(optional)\n"
                        + "./tables/\n"
                        + "./tables/table1.hjson\n"
                        + "./tables/table2.hjson\n"
                        + "./tables/tableN.hjson\n"));
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
