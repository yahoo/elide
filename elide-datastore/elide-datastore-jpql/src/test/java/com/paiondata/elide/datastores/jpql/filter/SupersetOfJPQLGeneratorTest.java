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

public class SupersetOfJPQLGeneratorTest {
    private EntityDictionary dictionary;
    private RSQLFilterDialect dialect;
    private Function<Path, String> aliasGenerator;

    public SupersetOfJPQLGeneratorTest() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Chapter.class);

        dialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        aliasGenerator = (path) -> getFieldAlias(getPathAlias(path, dictionary),
                path.lastElement().map(Path.PathElement::getFieldName).orElse(null));
    }

    @Test
    void testSupersetOfBookAuthorName() throws Exception {
        SupersetOfJPQLGenerator generator = new SupersetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=supersetof=('Jon Doe','Jane Doe')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                2 = (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testNotSupersetOfBookAuthorName() throws Exception {
        SupersetOfJPQLGenerator generator = new SupersetOfJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=notsupersetof=('Jon Doe','Jane Doe')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                NOT 2 = (SELECT COUNT(DISTINCT _INNER_example_Book_authors.name) FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testSupersetOfAuthorBookChapterTitle() throws Exception {
        SupersetOfJPQLGenerator generator = new SupersetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("books.chapters.title=supersetof=('A title','Another title')",
                ClassType.of(Author.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                2 = (SELECT COUNT(DISTINCT _INNER_example_Author_books_chapters.title) FROM example.Author _INNER_example_Author LEFT JOIN _INNER_example_Author.books _INNER_example_Author_books LEFT JOIN _INNER_example_Author_books.chapters _INNER_example_Author_books_chapters WHERE _INNER_example_Author.id = example_Author.id AND _INNER_example_Author_books_chapters.title IN (:XXX,:XXX))""";

        assertEquals(expected, actual);
    }

    @Test
    void testSupersetOfBookAwards() throws Exception {
        SupersetOfJPQLGenerator generator = new SupersetOfJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("awards=supersetof=('Foo','Bar')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                :XXX MEMBER OF example_Book.awards AND :XXX MEMBER OF example_Book.awards""";

        assertEquals(expected, actual);
    }

    @Test
    void testNotSupersetOfBookAwards() throws Exception {
        SupersetOfJPQLGenerator generator = new SupersetOfJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("awards=notsupersetof=('Foo','Bar')",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceAll(":\\w+", ":XXX");

        String expected = """
                :XXX NOT MEMBER OF example_Book.awards OR :XXX NOT MEMBER OF example_Book.awards""";

        assertEquals(expected, actual);
    }
}
