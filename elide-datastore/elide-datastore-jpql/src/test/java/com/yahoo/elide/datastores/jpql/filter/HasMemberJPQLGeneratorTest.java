/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jpql.filter;

import static com.yahoo.elide.core.utils.TypeHelper.getFieldAlias;
import static com.yahoo.elide.core.utils.TypeHelper.getPathAlias;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.filter.dialect.RSQLFilterDialect;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.type.ClassType;
import example.Author;
import example.Book;
import example.Chapter;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

public class HasMemberJPQLGeneratorTest {
    private EntityDictionary dictionary;
    private RSQLFilterDialect dialect;
    private Function<Path, String> aliasGenerator;

    public HasMemberJPQLGeneratorTest() {
        dictionary = EntityDictionary.builder().build();
        dictionary.bindEntity(Author.class);
        dictionary.bindEntity(Book.class);
        dictionary.bindEntity(Chapter.class);

        dialect = RSQLFilterDialect.builder().dictionary(dictionary).build();

        aliasGenerator = (path) -> getFieldAlias(getPathAlias(path, dictionary),
                path.lastElement().map(Path.PathElement::getFieldName).orElse(null));
    }

    @Test
    void testHasMemberBookAuthorName() throws Exception {
        HasMemberJPQLGenerator generator = new HasMemberJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=hasmember='Jon Doe'",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceFirst(":\\w+", ":XXX");

        String expected = "EXISTS (SELECT 1 FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors "
                + "WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name = :XXX)";

        assertEquals(expected, actual);
    }

    @Test
    void testHasNoMemberBookAuthorName() throws Exception {
        HasMemberJPQLGenerator generator = new HasMemberJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("authors.name=hasnomember='Jon Doe'",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceFirst(":\\w+", ":XXX");

        String expected = "NOT EXISTS (SELECT 1 FROM example.Book _INNER_example_Book LEFT JOIN _INNER_example_Book.authors _INNER_example_Book_authors "
                + "WHERE _INNER_example_Book.id = example_Book.id AND _INNER_example_Book_authors.name = :XXX)";

        assertEquals(expected, actual);
    }

    @Test
    void testHasMemberAuthorBookChapterTitle() throws Exception {
        HasMemberJPQLGenerator generator = new HasMemberJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("books.chapters.title=hasmember='A title'",
                ClassType.of(Author.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceFirst(":\\w+", ":XXX");

        String expected = "EXISTS (SELECT 1 FROM example.Author _INNER_example_Author LEFT JOIN _INNER_example_Author.books _INNER_example_Author_books "
                + "LEFT JOIN _INNER_example_Author_books.chapters _INNER_example_Author_books_chapters WHERE _INNER_example_Author.id = example_Author.id AND _INNER_example_Author_books_chapters.title = :XXX)";

        assertEquals(expected, actual);
    }

    @Test
    void testHasMemberBookAwards() throws Exception {
        HasMemberJPQLGenerator generator = new HasMemberJPQLGenerator(dictionary);

        FilterExpression expression = dialect.parseFilterExpression("awards=hasmember='Foo'",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceFirst(":\\w+", ":XXX");

        String expected = ":XXX MEMBER OF example_Book.awards";

        assertEquals(expected, actual);
    }

    @Test
    void testHasNoMemberBookAwards() throws Exception {
        HasMemberJPQLGenerator generator = new HasMemberJPQLGenerator(dictionary, true);

        FilterExpression expression = dialect.parseFilterExpression("awards=hasnomember='Foo'",
                ClassType.of(Book.class),
                true);

        String actual = generator.generate((FilterPredicate) expression, aliasGenerator);
        actual = actual.replaceFirst(":\\w+", ":XXX");

        String expected = ":XXX NOT MEMBER OF example_Book.awards";

        assertEquals(expected, actual);
    }
}
