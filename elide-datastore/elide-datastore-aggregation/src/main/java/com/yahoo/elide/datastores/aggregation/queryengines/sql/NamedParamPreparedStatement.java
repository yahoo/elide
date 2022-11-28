/*
 * Copyright 2020, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.datastores.aggregation.queryengines.sql;

import com.yahoo.elide.core.filter.predicates.FilterPredicate;

import lombok.Getter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper Class to support named parameter substitution for {@link PreparedStatement}.
 */
public class NamedParamPreparedStatement {

    /**
     * Parameter Pattern as defined in {@link FilterPredicate#getParameters()}.
     * examples:
     * :overallRating_c82e10a5_0
     * :lowScore_7c4e440_0
     */
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("(?<!')(:[\\w]+_[0-9A-Fa-f]+_[\\d]+)(?!')");

    @Getter
    private PreparedStatement preparedStatement;
    private List<String> fields = new ArrayList<>();

    public NamedParamPreparedStatement(Connection conn, String namedParamQuery) throws SQLException {

        Matcher matcher = PARAMETER_PATTERN.matcher(namedParamQuery);
        while (matcher.find()) {
            fields.add(matcher.group().substring(1));
        }
        preparedStatement = conn.prepareStatement(namedParamQuery.replaceAll(PARAMETER_PATTERN.pattern(), "?"));
    }

    public ResultSet executeQuery() throws SQLException {
        return preparedStatement.executeQuery();
    }

    public void close() throws SQLException {
        preparedStatement.close();
    }

    public void cancel() throws SQLException {
        preparedStatement.cancel();
    }

    public boolean isClosed() throws SQLException {
        return preparedStatement.isClosed();
    }

    public void setObject(String paramName, Object paramValue) throws SQLException {
        preparedStatement.setObject(getIndex(paramName), paramValue);
    }

    private int getIndex(String paramName) {
        return fields.indexOf(paramName) + 1;
    }
}
