/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.type.ClassType;
import example.Author;
import example.Book;
import example.Job;
import example.PrimitiveId;
import example.StringId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.util.Collections;
import java.util.Map;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests RSQLFilterDialect
 */
public class RSQLFilterDialectTest {
    private static RSQLFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = new EntityDictionary(Collections.EMPTY_MAP);

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(StringId.class);
        dictionary.bindEntity(Job.class);
        dictionary.bindEntity(PrimitiveId.class);
        dialect = new RSQLFilterDialect(dictionary);
    }

    @Test
    public void testTypedExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[book]",
                "title==*foo*;title!=bar*;(genre=in=(sci-fi,action),publishDate>123)"
        );

        queryParams.add(
                "filter[author]",
                "books.title=in=(foo,bar,baz)"
        );


        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(2, expressionMap.size());
        assertEquals(
                "((book.title INFIX [foo] AND NOT (book.title PREFIX [bar])) "
                        + "AND (book.genre IN [sci-fi, action] OR book.publishDate GT [123]))",
                expressionMap.get("book").toString()
        );

        assertEquals(
                "author.books.title IN [foo, bar, baz]",
                expressionMap.get("author").toString()
        );

        queryParams.clear();
        queryParams.add(
                "filter[author]",
                "books.title=ini=(foo,bar,baz)"
        );

        // Case Insensitive
        expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);
        assertEquals(1, expressionMap.size());
        assertEquals(
                "author.books.title IN_INSENSITIVE [foo, bar, baz]",
                expressionMap.get("author").toString()
        );
    }

    @Test
    public void testGlobalExpressionParsing() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==*foo*;authors.name=ini=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "(book.title INFIX [foo] AND book.authors.name IN_INSENSITIVE [Hemingway])",
                expression.toString()
        );
    }

    @Test
    public void testEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title IN [Hemingway]", expression.toString());
    }

    @Test
    public void testNotEqualOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title!=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("NOT (book.title IN [Hemingway])", expression.toString());
    }

    @Test
    public void testInOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=in=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title IN [Hemingway]", expression.toString());
    }

    @Test
    public void testInInsensitiveOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=ini=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title IN_INSENSITIVE [Hemingway]", expression.toString());
    }

    @Test
    public void testOutOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=out=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("NOT (book.title IN [Hemingway])", expression.toString());
    }

    @Test
    public void testOutInsensitiveOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=outi=Hemingway"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("NOT (book.title IN_INSENSITIVE [Hemingway])", expression.toString());
    }

    @Test
    public void testNumericComparisonOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "(publishDate=gt=5,publishDate=ge=5,publishDate=lt=10,publishDate=le=10);"
                        + "(publishDate>5,publishDate>=5,publishDate<10,publishDate<=10)"

        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "((((book.publishDate GT [5] OR book.publishDate GE [5]) "
                        + "OR book.publishDate LT [10]) OR book.publishDate LE [10]) AND "
                        + "(((book.publishDate GT [5] OR book.publishDate GE [5]) "
                        + "OR book.publishDate LT [10]) OR book.publishDate LE [10]))",
                expression.toString()
        );
    }

    @Test
    public void testSubstringOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title==*Hemingway*,title==*Hemingway,title==Hemingway*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "((book.title INFIX [Hemingway] "
                        + "OR book.title POSTFIX [Hemingway]) "
                        + "OR book.title PREFIX [Hemingway])",
                expression.toString()
        );
    }

    @Test
    public void testSubstringCIOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=ini=*Hemingway*,title=ini=*Hemingway,title=ini=Hemingway*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "((book.title INFIX_CASE_INSENSITIVE [Hemingway] "
                        + "OR book.title POSTFIX_CASE_INSENSITIVE [Hemingway]) "
                        + "OR book.title PREFIX_CASE_INSENSITIVE [Hemingway])",
                expression.toString()
        );
    }

    @Test
    public void testExpressionGrouping() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=ini=foo;(title==bar;title==baz)"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "(book.title IN_INSENSITIVE [foo] AND (book.title IN [bar] "
                        + "AND book.title IN [baz]))",
                expression.toString()
        );
    }

    @Test
    public void testIsnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=true"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testIsnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title ISNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=false"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title NOTNULL []", expression.toString());
    }

    @Test
    public void testNotnullOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isnull=0"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title NOTNULL []", expression.toString());
    }

    @Test
    public void testVisit() {
        ComparisonNode comparisonNode = new ComparisonNode(
                new ComparisonOperator("==", false),
                "id",
                Collections.singletonList("*20*")
        );

        RSQLFilterDialect.RSQL2FilterExpressionVisitor visitor = dialect.new RSQL2FilterExpressionVisitor(true);

        assertEquals(
                "author.id INFIX [20]",
                visitor.visit(comparisonNode, new ClassType(Author.class)).toString()
        );
    }

    @Test
    public void testFilterOnCustomizedLongIdField() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "id==1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/job", queryParams, NO_VERSION);

        assertEquals("job.jobId IN [1]", expression.toString());
    }

    @Test
    public void testFilterOnCustomizedStringIdField() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "id==*identifier*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/stringForId", queryParams, NO_VERSION);

        assertEquals("stringId.surrogateKey INFIX [identifier]", expression.toString());
    }

    @Test
    public void testInfixFilterOnPrimitiveIdField() throws ParseException {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "id==*1*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/primitiveTypeId", queryParams, NO_VERSION);

        assertEquals(expression.toString(),
                "primitiveId.primitiveId INFIX [1]"
        );
    }

    //TODO: add test for =isempty= case

    @Test
    public void testIsemptyOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isempty=true"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title ISEMPTY []", expression.toString());
    }

    @Test
    public void testIsemptyOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "authors=isempty=1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.authors ISEMPTY []", expression.toString());
    }

    @Test
    public void testNotemptyOperatorBool() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "authors=isempty=false"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.authors NOTEMPTY []", expression.toString());
    }

    @Test
    public void testNotemptyOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "title=isempty=0"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.title NOTEMPTY []", expression.toString());
    }

    @Test
    public void testEmptyOperatorException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "authors.name=isempty=0"
        );

        assertThrows(ParseException.class,
                () -> dialect.parseTypedExpression("/book", queryParams, NO_VERSION));
    }

    @Test
    public void testMemberOfOperatorInt() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "awards=hasmember=title1"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals("book.awards HASMEMBER [title1]", expression.toString());
    }

    @Test
    public void testMemberOfToManyRelationship() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "authors.name=hasmember='0'"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);
        assertEquals("book.authors.name HASMEMBER [0]", expression.toString());
    }

    @Test
    public void testMemberOfOperatorException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.clear();
        queryParams.add(
                "filter",
                "title=hasmember=title11"
        );

        assertThrows(ParseException.class,
                () -> dialect.parseGlobalExpression("/book", queryParams, NO_VERSION));
    }
}
