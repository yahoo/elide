/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers.validator;

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
import org.apache.commons.io.IOUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
    private static final int FILE_PREFIX_LENGTH = 5; //"file:".length()

    private ElideTableConfig elideTableConfig;
    private ElideSecurityConfig elideSecurityConfig;
    private Map<String, Object> variables;
    private String configDir;

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

        DynamicConfigValidator dynamicConfigValidator = new DynamicConfigValidator();

        if (exists(configDir)) {
            //look in local file system
            File configFile = new File(configDir);
            String absoluteBasePath = configFile.getAbsolutePath();
            log.info("Absolute Path for Model Configs Directory: " + absoluteBasePath);

            dynamicConfigValidator.readAndValidateConfigs(absoluteBasePath);

        } else {
            // look in classpath
            log.info("Looking at " + configDir + " in resources");
            dynamicConfigValidator.readAndValidateClasspathConfigs(configDir);
        }

        log.info("Configs Validation Passed!");
    }

    /**
     * Read and validate config files under resources.
     * @throws IOException
     */
    public void readAndValidateClasspathConfigs(String filePath)
            throws IOException {

        this.setConfigDir(DynamicConfigHelpers.formatFilePath(formatClassPath(filePath)));
        ClassLoader classLoader = DynamicConfigValidator.class.getClassLoader();

        URL url = classLoader.getResource(this.configDir);
        if (url == null) {
            throw new IllegalStateException("Model Configs Directory doesn't exists");
        }

        URL jar = DynamicConfigValidator.class.getProtectionDomain().getCodeSource().getLocation();
        Path jarFile = Paths.get(jar.toString().substring(FILE_PREFIX_LENGTH));
        FileSystem fs = FileSystems.newFileSystem(jarFile, null);

        // load variables
        this.readVariableConfig(fs);

        //load and resolve security
        this.readSecurityConfig(fs);

        // load and resolve tables
        this.readTableConfig(fs);

    }

    /**
     * Read and validate config files under config directory.
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
        }
    }

    /**
     * Read variable file config.
     * @return boolean true if variable config file exists else false
     * @throws IOException
     */
    private boolean readVariableConfig() throws IOException {
        boolean isVariableConfig = exists(this.configDir + DynamicConfigHelpers.VARIABLE_CONFIG_PATH);
        this.variables = isVariableConfig ? DynamicConfigHelpers.getVariablesPojo(this.configDir)
                : Collections.<String, Object>emptyMap();
        return isVariableConfig;
    }

    /**
     * Read variable file config from filesystem.
     * @param fs : path to variable file
     * @throws IOException
     */
    private void readVariableConfig(FileSystem fs) throws IOException {
        DirectoryStream<Path> directoryStream = null;
        InputStream inputStream = null;
        try {
            directoryStream = Files.newDirectoryStream(fs.getPath(this.configDir));
            for (Path path : directoryStream) {
                String fileName = path.getFileName().toString();
                if (fileName.equals(DynamicConfigHelpers.VARIABLE_CONFIG_PATH)) {
                    inputStream = DynamicConfigValidator.class.getResourceAsStream(path.toString());
                    this.variables = DynamicConfigHelpers.stringToVariablesPojo(
                            IOUtils.toString(inputStream, StandardCharsets.UTF_8));
                }
            }
        } finally {
            inputStream.close();
            directoryStream.close();
        }

    }

    /**
     * Read security config file and checks for any missing Handlebar variables.
     * @return boolean true if security config file exists else false
     * @throws IOException
     */
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
    }

    /**
     * Read security config file and checks for any missing Handlebar variables.
     * @param fs : FileSystem path to security file
     * @throws IOException
     */
    private void readSecurityConfig(FileSystem fs) throws IOException {
        DirectoryStream<Path> directoryStream = null;
        try {
            directoryStream = Files.newDirectoryStream(fs.getPath(configDir));

            for (Path path: directoryStream) {
                String fileName = path.getFileName().toString();
                if (fileName.equals(DynamicConfigHelpers.SECURITY_CONFIG_PATH)) {
                    String securityConfigContent = IOUtils.toString(DynamicConfigValidator.class.getResourceAsStream(
                            path.toString()), StandardCharsets.UTF_8);
                    validateConfigForMissingVariables(securityConfigContent, this.variables);
                    this.elideSecurityConfig = DynamicConfigHelpers.stringToElideSecurityPojo(securityConfigContent,
                            this.variables);
                }
            }
        } finally {
            directoryStream.close();
        }
    }

    /**
     * Read table config files and checks for any missing Handlebar variables.
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
     * Read table config files and checks for any missing Handlebar variables.
     * @param fs : FileSystem path to tables config
     * @throws IOException
     */
    private void readTableConfig(FileSystem fs) throws IOException {

        DirectoryStream<Path> directoryStream = null;
        Set<Table> tables = new HashSet<>();
        try {
            directoryStream = Files.newDirectoryStream(fs.getPath(configDir + DynamicConfigHelpers.TABLE_CONFIG_PATH));
            for (Path path : directoryStream) {
                String tableConfigContent = IOUtils.toString(DynamicConfigValidator.class.getResourceAsStream(
                        path.toString()), StandardCharsets.UTF_8);
                validateConfigForMissingVariables(tableConfigContent, this.variables);
                ElideTableConfig table = DynamicConfigHelpers.stringToElideTablePojo(tableConfigContent, variables);
                tables.addAll(table.getTables());
            }
            ElideTableConfig elideTableConfig = new ElideTableConfig();
            elideTableConfig.setTables(tables);
            this.elideTableConfig = elideTableConfig;
        } finally {
            directoryStream.close();
        }
    }

    /**
     * Check if file or directory exists.
     * @param filePath path of the file or directory
     * @return boolean true if file or directory exists else false
     */
    public static boolean exists(String filePath) {
        return new File(filePath).exists();
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
        String[] path = filePath.split(File.separator + "resources" + File.separator);
        return (path.length == 2 ? path[1] : filePath);
    }
}
