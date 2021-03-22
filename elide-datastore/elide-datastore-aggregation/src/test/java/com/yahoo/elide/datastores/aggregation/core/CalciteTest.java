package com.yahoo.elide.datastores.aggregation.core;

import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

public class CalciteTest {

    @Test
    public void test() throws Exception {
        String sql = "        SUM (CASE\n" +
                "                WHEN number_of_lectures > 20 THEN 1\n" +
                "                ELSE 0\n" +
                "        END) / SUM(\"{{$$blah}}\")";
        SqlParser.ConfigBuilder parserBuilder = SqlParser.configBuilder();
        SqlParser sqlParser = SqlParser.create(sql, parserBuilder.build());
        SqlNode node = sqlParser.parseExpression();
        System.out.println(node.toString());
    }

    @Test
    public void test2() throws Exception {
        String sql = "SUM(foo)";
        SqlParser.ConfigBuilder parserBuilder = SqlParser.configBuilder();
        SqlParser sqlParser = SqlParser.create(sql, parserBuilder.build());
        SqlBasicCall call = (SqlBasicCall) sqlParser.parseExpression();
        SqlNode inner = call.getOperands()[0];

        call.setOperand(0, new SqlIdentifier("bar", inner.getParserPosition()));

        System.out.println(call.toString());
    }
}
