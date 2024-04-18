/*
 * Copyright 2018, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.paiondata.elide.jsonapi.parser;

import com.paiondata.elide.generated.parsers.CoreLexer;
import com.paiondata.elide.generated.parsers.CoreParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Parses the REST request.
 */
public class JsonApiParser {

    private static final Pattern DUPLICATE_SEPARATOR_PATTERN = Pattern.compile("//+");

    /**
     * Normalize request path.
     *
     * @param path request path
     * @return normalized path string
     */
    public static String normalizePath(String path) {
        String normalizedPath = DUPLICATE_SEPARATOR_PATTERN.matcher(path).replaceAll("/");

        normalizedPath = StringUtils.removeEnd(normalizedPath, "/");

        return StringUtils.removeStart(normalizedPath, "/");
    }

    /**
     * Compile request to AST.
     *
     * @param path request
     * @return AST parse tree
     */
    public static ParseTree parse(String path) {
        String normalizedPath = normalizePath(path);

        CharStream is = CharStreams.fromString(normalizedPath);
        CoreLexer lexer = new CoreLexer(is);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                    int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseCancellationException(msg, e);
            }
        });
        CoreParser parser = new CoreParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.start();
    }
}
