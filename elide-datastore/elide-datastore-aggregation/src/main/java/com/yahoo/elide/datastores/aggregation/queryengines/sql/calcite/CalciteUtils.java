/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.aggregation.queryengines.sql.calcite;

import static com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect.BACKTICK;
import static com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.AbstractSqlDialect.DOUBLE_QUOTE;

import com.yahoo.elide.datastores.aggregation.queryengines.sql.dialects.SQLDialect;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.CharLiteralStyle;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.parser.SqlParser;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Utility functions for Calcite parsing.
 */
public class CalciteUtils {

    /**
     * Converts an elide dialect into a Calcite parser configuration.
     * @param dialect An elide dialect.
     * @return A new parser configuration.
     */
    public static SqlParser.Config constructParserConfig(SQLDialect dialect) {
        SqlDialect calciteDialect = dialect.getCalciteDialect();

        Quoting quoting;
        switch (dialect.getBeginQuote()) {
            case BACKTICK: {
                quoting = Quoting.BACK_TICK;
                break;
            }
            case DOUBLE_QUOTE: {
                quoting = Quoting.DOUBLE_QUOTE;
                break;
            }
            case '[': {
                quoting = Quoting.BRACKET;
                break;
            }
            default: {
                throw new IllegalStateException("Unrecognized identifier quotation mark");
            }
        }
        return SqlParser.config()
                .withQuoting(quoting)
                .withConformance(calciteDialect.getConformance())
                .withUnquotedCasing(calciteDialect.getUnquotedCasing())
                .withQuotedCasing(calciteDialect.getQuotedCasing())
                .withCharLiteralStyles(new HashSet<>(Arrays.asList(CharLiteralStyle.STANDARD)))
                .withCaseSensitive(calciteDialect.isCaseSensitive());
    }
}
