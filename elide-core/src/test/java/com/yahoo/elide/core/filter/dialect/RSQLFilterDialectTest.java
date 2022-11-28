/*
 * Copyright 2016, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.dialect;

import static com.yahoo.elide.core.dictionary.EntityDictionary.NO_VERSION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.expression.OrFilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.type.ClassType;
import com.yahoo.elide.core.type.Type;
import example.Author;
import example.Book;
import example.Job;
import example.PrimitiveId;
import example.Publisher;
import example.StringId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Tests RSQLFilterDialect
 */
public class RSQLFilterDialectTest {
    private static RSQLFilterDialect dialect;

    @BeforeAll
    public static void init() {
        EntityDictionary dictionary = EntityDictionary.builder().build();

        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Publisher.class);
        dictionary.bindEntity(StringId.class);
        dictionary.bindEntity(Job.class);
        dictionary.bindEntity(PrimitiveId.class);
        dialect = RSQLFilterDialect.builder().dictionary(dictionary).build();
    }

    @Test
    public void testTypedExpressionParsingWithComplexAttribute() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter[author]",
                "homeAddress.street1==*State*"
        );

        Map<String, FilterExpression> expressionMap = dialect.parseTypedExpression("/author", queryParams, NO_VERSION);

        assertEquals(1, expressionMap.size());
        assertEquals(
                "author.homeAddress.street1 INFIX [State]",
                expressionMap.get("author").toString()
        );
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
    public void testGlobalExpressionParsingWithComplexAttribute() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter", "homeAddress.street1==*State*"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/author", queryParams, NO_VERSION);

        assertEquals("author.homeAddress.street1 INFIX [State]", expression.toString());
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
    public void testBetweenOperator() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.add(
                "filter",
                "(publishDate=notbetween=(5,10))"
        );

        FilterExpression expression = dialect.parseGlobalExpression("/book", queryParams, NO_VERSION);

        assertEquals(
                "book.publishDate NOTBETWEEN [5, 10]",
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
                visitor.visit(comparisonNode, ClassType.of(Author.class)).toString()
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
    public void testMemberOfOperatorOnNonCollectionAttributeException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.clear();
        queryParams.add(
                "filter",
                "title=hasmember=title11"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseGlobalExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element has to be a collection type", e.getMessage());
    }

    @Test
    public void testMemberOfOperatorOnRelationshipException() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();

        queryParams.clear();
        queryParams.add(
                "filter",
                "authors=hasmember=1"
        );

        ParseException e = assertThrows(ParseException.class,
                () -> dialect.parseGlobalExpression("/book", queryParams, NO_VERSION));

        assertEquals("Invalid Path: Last Path Element cannot be a collection type", e.getMessage());
    }

    @Test
    public void testFilterArgumentParsingBadInput() throws Exception {

        Exception exception;
        Type<Book> bookType = ClassType.of(Book.class);

        // Empty argument name
        exception = assertThrows(ParseException.class, () -> dialect.parse(bookType, Collections.emptySet(),
                        "genre=in=(sci-fi,action),title[a:][x:y]==Hemingway", NO_VERSION));
        assertEquals("Filter expression is not in expected format at: title[a:][x:y]", exception.getMessage());

        // Empty argument value
        exception = assertThrows(ParseException.class, () -> dialect.parse(bookType, Collections.emptySet(),
                        "genre=in=(sci-fi,action),title[a:b][:y]==Hemingway", NO_VERSION));
        assertEquals("Filter expression is not in expected format at: title[a:b][:y]", exception.getMessage());

        // : missing within []
        exception = assertThrows(ParseException.class, () -> dialect.parse(bookType, Collections.emptySet(),
                        "genre=in=(sci-fi,action),title[ab][x:y]==Hemingway", NO_VERSION));
        assertEquals("Filter expression is not in expected format at: title[ab][x:y]", exception.getMessage());

        // Something found after first '[' and not within []
        exception = assertThrows(ParseException.class, () -> dialect.parse(bookType, Collections.emptySet(),
                        "genre=in=(sci-fi,action),title[a:b]asdf[x:y]==Hemingway", NO_VERSION));
        assertEquals("Filter expression is not in expected format at: title[a:b]asdf[x:y]", exception.getMessage());

        // Invalid encoded argument value
        exception = assertThrows(ParseException.class, () -> dialect.parse(bookType, Collections.emptySet(),
                        "title[a:b][x:Invalid%_Encoding]==Hemingway", NO_VERSION));
        assertEquals("Filter expression is not in expected format at: title[a:b][x:Invalid%_Encoding]. "
                        + "URLDecoder: Illegal hex characters in escape (%) pattern - Error at index 0 in: \"_E\"",
                exception.getMessage());
    }

    @Test
    public void testFilterArgumentParsingEncodeValues() throws Exception {

        FilterExpression expr;
        Type<Book> bookType = ClassType.of(Book.class);
        String argValue1 = "with space";
        String argValue2 = ": \" ' ( ) ; , = ! ~ < > ] [";
        Set<Argument> inputArgs = new HashSet<>();
        inputArgs.add(Argument.builder().name("arg1").value(argValue1).build());
        inputArgs.add(Argument.builder().name("arg2").value(argValue2).build());
        String encodeArgValue1 = URLEncoder.encode(argValue1, "UTF-8");
        String encodeArgValue2 = URLEncoder.encode(argValue2, "UTF-8");

        expr = assertDoesNotThrow(
                        () -> dialect.parse(bookType, Collections.emptySet(), "genre=in=(sci-fi,action),title[arg1:"
                                        + encodeArgValue1 + "][arg2:" + encodeArgValue2 + "]==Hemingway", NO_VERSION));
        assertEquals("(book.genre IN [sci-fi, action] OR book.title IN [Hemingway])", expr.toString());

        FilterPredicate left = (FilterPredicate) ((OrFilterExpression) expr).getLeft();
        Set<Argument> leftArgs = left.getPath().getPathElements().get(0).getArguments();
        assertTrue(leftArgs.isEmpty());

        FilterPredicate right = (FilterPredicate) ((OrFilterExpression) expr).getRight();
        Set<Argument> rightArgs = right.getPath().getPathElements().get(0).getArguments();
        assertTrue(rightArgs.equals(inputArgs));
    }

    @Test
    public void testGraphQLFilterDialectWithComplexAttribute() throws Exception {
        Type<Author> authorType = ClassType.of(Author.class);
        FilterPredicate predicate = (FilterPredicate)
                dialect.parse(authorType, Collections.emptySet(), "homeAddress.street1=in=(foo)", NO_VERSION);

        assertEquals(Operator.IN, predicate.getOperator());
        Path path = predicate.getPath();
        assertEquals(2, path.getPathElements().size());
        assertEquals("homeAddress", path.getPathElements().get(0).getFieldName());
        assertEquals("street1", path.getPathElements().get(1).getFieldName());
    }
}
