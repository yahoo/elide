/*
 * Copyright 2021, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.ElideSettingsBuilder;
import com.yahoo.elide.core.RequestScope;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.Argument;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.request.Relationship;
import com.yahoo.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import example.Author;
import example.Book;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;

public class JMSDataStoreTest {

    @Test
    public void testLoadObjects() throws Exception {
        EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(false);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://0");

        EntityDictionary dictionary = EntityDictionary.builder().build();

        Author author1 = new Author();
        author1.setId(1);
        author1.setName("Jon Doe");

        Book book1 = new Book();
        book1.setTitle("Enders Game");
        book1.setId(1);
        book1.setAuthors(Sets.newHashSet(author1));

        Book book2 = new Book();
        book2.setTitle("Grapes of Wrath");
        book2.setId(2);

        JMSDataStore store = new JMSDataStore(Sets.newHashSet(
                ClassType.of(Book.class), ClassType.of(Author.class)), cf, dictionary);
        store.populateEntityDictionary(dictionary);

        try (DataStoreTransaction tx = store.beginReadTransaction()) {

            RequestScope scope = new SubscriptionRequestScope(
                    "/json",
                    tx,
                    null,
                    "1.0",
                    new ElideSettingsBuilder(store)
                            .withEntityDictionary(dictionary)
                            .build(),
                    null,
                    null,
                    2000);

            Iterable<Book> books = tx.loadObjects(
                    EntityProjection.builder()
                            .argument(Argument.builder()
                                    .name("topic")
                                    .value(TopicType.ADDED)
                                    .build())
                            .type(Book.class).build(),
                    scope
            );

            JMSContext context = cf.createContext();
            Destination destination = context.createTopic("bookAdded");

            JMSProducer producer = context.createProducer();
            ObjectMapper mapper = new ObjectMapper();
            producer.send(destination, mapper.writeValueAsString(book1));
            producer.send(destination, mapper.writeValueAsString(book2));

            Iterator<Book> booksIterator = books.iterator();

            assertTrue(booksIterator.hasNext());
            Book receivedBook = booksIterator.next();
            assertEquals("Enders Game", receivedBook.getTitle());
            assertEquals(1, receivedBook.getId());

            Set<Author> receivedAuthors = tx.getRelation(tx, receivedBook,
                    Relationship.builder()
                            .name("authors")
                            .projection(EntityProjection.builder()
                                    .type(Author.class)
                                    .build())
                            .build(), scope);

            assertTrue(receivedAuthors.contains(author1));

            assertTrue(booksIterator.hasNext());
            receivedBook = booksIterator.next();
            assertEquals("Grapes of Wrath", receivedBook.getTitle());
            assertEquals(2, receivedBook.getId());

            assertFalse(booksIterator.hasNext());
        }
    }
}
