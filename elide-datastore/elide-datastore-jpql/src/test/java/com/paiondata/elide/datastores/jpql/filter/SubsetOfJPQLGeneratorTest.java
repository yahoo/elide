/*
 * Copyright 2023, the original author or authors.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.paiondata.elide.datastores.jpql.filter;

import static com.paiondata.elide.core.utils.TypeHelper.getFieldAlias;
import static com.paiondata.elide.core.utils.TypeHelper.getPathAlias;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.paiondata.elide.core.Path;
import com.paiondata.elide.core.dictionary.EntityDictionary;
import com.paiondata.elide.core.filter.dialect.RSQLFilterDialect;
import com.paiondata.elide.core.filter.expression.FilterExpression;
import com.paiondata.elide.core.filter.predicates.FilterPredicate;
import com.paiondata.elide.core.type.ClassType;
import example.Author;
import example.Book;
import example.Chapter;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class SubsetOfJPQLGeneratorTest {
    private EntityDictionary dictionary;
    private RSQLFilterDialect dialect;
    private Function<Path, String> aliasGenerator;

    public SubsetOfJPQLGeneratorTest() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Chapter.class);

        dialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        aliasGenerator = (path) -> getFieldAlias(getPathAlias(path, dictionary),
                path.lastElement().map(Path.PathElement::getFieldName).orElse(null));
    }

    @Test
    void testSubsetOfBookAuthorName() throws Exception {
        SubsetOfJPQLGenerator generator = new SubsetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=subsetof=('Jon Doe','Jane Doe')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id) = (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testNotSubsetOfBookAuthorName() throws Exception {
        SubsetOfJPQLGenerator generator = new SubsetOfJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=notsubsetof=('Jon Doe','Jane Doe')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                NOT (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id) = (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testSubsetOfAuthorBookChapterTitle() throws Exception {
        SubsetOfJPQLGenerator generator = new SubsetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("books.chapters.title=subsetof=('A title','Another title')",
                ClassType.of(Author.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                (SELECT COUNT(DISTINCT _INNER_example_Author_books_chapters.title) FROM example.Author _INNER_example_Author LEFT JOIN _INNER_example_Author.books _INNER_example_Author_books LEFT JOIN _INNER_example_Author_books.chapters _INNER_example_Author_books_chapters WHERE _INNER_example_Author.id = example_Author.id) = (SELECT COUNT(DISTINCT _INNER_example_Author_books_chapters.title) FROM example.Author _INNER_example_Author LEFT JOIN _INNER_example_Author.books _INNER_example_Author_books LEFT JOIN _INNER_example_Author_books.chapters _INNER_example_Author_books_chapters WHERE _INNER_example_Author.id = example_Author.id AND _INNER_example_Author_books_chapters.title IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testSubsetOfBookAwards() throws Exception {
        SubsetOfJPQLGenerator generator = new SubsetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("awards=subsetof=('Foo','Bar')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                size(example_Book.awards) = (CASE :XXX MEMBER OF example_Book.awards WHEN true THEN 1 ELSE 0 END) + (CASE :XXX MEMBER OF example_Book.awards WHEN true THEN 1 ELSE 0 END)""";

        assertEquals(expected, actual);
    }

    @Test
    void testNotSubsetOfAllBookAwards() throws Exception {
        SubsetOfJPQLGenerator generator = new SubsetOfJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("awards=notsubsetof=('Foo','Bar')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                NOT size(example_Book.awards) = (CASE :XXX MEMBER OF example_Book.awards WHEN true THEN 1 ELSE 0 END) + (CASE :XXX MEMBER OF example_Book.awards WHEN true THEN 1 ELSE 0 END)""";

        assertEquals(expected, actual);
    }
}
