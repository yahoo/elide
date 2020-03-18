/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.contrib.dynamicconfighelpers;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.hjson.JsonValue;
import org.json.JSONObject;
import org.json.JSONTokener;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Util class for HJSON parsing.
 */
@Slf4j
public class ElideHjsonUtil {

    public static final String SCHEMA_TYPE_TABLE = "table";
    public static final String SCHEMA_TYPE_SECURITY = "security";
    public static final String SCHEMA_TYPE_VARIABLE = "variable";

    public static final String ELIDE_TABLE_VALIDATION_SCHEMA = "elideTableSchema.json";
    public static final String ELIDE_SECURITY_SCHEMA = "elideSecuritySchema.json";
    public static final String ELIDE_VARIABLE_SCHEMA = "elideVariableSchema.json";

    public static final String INVALID_ERROR_MSG = "Incompatible or invalid config";
    public static final String HTTP_PREFIX = "http";
    public static final String CHAR_SET = "UTF-8";
    public static final String NEW_LINE = "\n";

   /**
    * HJSON to JSON string conversion.
    * @param hjson hjson string
    * @return json string
    */
    public static String hjsonToJson(String hjson) {
        return JsonValue.readHjson(hjson).toString();
    }

    /**
     * Config file reader. Supports HTTP or Local Filesystem.
     * @param filePath Path to File
     * @return contents of the file
     * @throws Exception
     */
    public static String readConfigFile(String filePath) throws Exception {

        BufferedReader reader = null;
        try {
            reader = (filePath.startsWith(HTTP_PREFIX)
                            ? getHttpFileReader(filePath) : getLocalFileReader(filePath));
            return readFileContent(reader);
        } catch (Exception e) {
            throw e;
        } finally {
            reader.close();
        }
    }

    /**
     * Validates JSON to Schema.
     * @param schemaType type of schema
     * @param jsonConfig json string
     * @return whether json validation passed
     */
    public static boolean validateDataWithSchema(String schemaType, String jsonConfig) {

        try {
            JSONObject schemaObj = schemaToJsonObject(schemaType);
            JSONObject data = new JSONObject(new JSONTokener(jsonConfig));

            Schema schema = SchemaLoader.load(schemaObj);
            schema.validate(data);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * Null check for the string.
     * @param input string to validate
     * @return whether string is null or not
     */
    public static boolean isNull(String input) {
        return (input == null || input.trim().length() == 0) ? true : false;
    }

    /**
     * Read file contents.
     * @param reader BufferedReader object
     * @return contents of file
     * @throws IOException
     */
    private static String readFileContent(BufferedReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append(NEW_LINE);
        }
        return sb.toString();
    }

    /**
     * Get HTTP file reader.
     * @param filePath Path to file
     * @return BufferedReader object
     * @throws Exception
     */
    private static BufferedReader getHttpFileReader(String filePath) throws Exception {
        URL url = new URL(filePath);
        return new BufferedReader(new InputStreamReader(url.openStream()));
    }

    /**
     * Get local file system reader.
     * @param filePath Path to file
     * @return BufferedReader object
     * @throws Exception
     */
    private static BufferedReader getLocalFileReader(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        return Files.newBufferedReader(path, Charset.forName(CHAR_SET));
    }

    /**
     * Get the Json Object matching the schema json.
     * @param confFile
     * @return
     * @throws IOException
     */
    private static JSONObject loadSchema(String confFile) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource(confFile).getFile());
        String content = new String(Files.readAllBytes(file.toPath()));
        return new JSONObject(new JSONTokener(content));
    }

    /**
     * Get the Json Object matching the schema type.
     * @param schemaType Type of Schema
     * @return JSONObject represention of the schema
     * @throws IOException
     */
    private static JSONObject schemaToJsonObject(String schemaType) throws IOException {

        switch (schemaType) {
            case SCHEMA_TYPE_TABLE:
                return loadSchema(ELIDE_TABLE_VALIDATION_SCHEMA);

            case SCHEMA_TYPE_SECURITY:
                return loadSchema(ELIDE_SECURITY_SCHEMA);

            case SCHEMA_TYPE_VARIABLE:
                return loadSchema(ELIDE_VARIABLE_SCHEMA);
            default:
                return null;
        }
    }
}
