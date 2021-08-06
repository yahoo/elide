package com.yahoo.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import example.Book;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
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

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);

        Book book1 = new Book();
        book1.setTitle("Enders Game");
        book1.setId(1);

        Book book2 = new Book();
        book2.setTitle("Grapes of Wrath");
        book2.setId(2);

        JMSDataStore store = new JMSDataStore(Sets.newHashSet(ClassType.of(Book.class)), cf, dictionary);
        store.populateEntityDictionary(dictionary);

        try (DataStoreTransaction tx = store.beginReadTransaction()) {

            Iterable<Book> books = tx.loadObjects(
                    EntityProjection.builder().type(Book.class).build(),
                    null
            );

            JMSContext context = cf.createContext();
            context.start();
            Destination destination = context.createTopic("bookAdded");

            JMSProducer producer = context.createProducer();
            ObjectMapper mapper = new ObjectMapper();
            producer.send(destination, mapper.writeValueAsString(book1));
            producer.send(destination, mapper.writeValueAsString(book2));

            Iterator<Book> booksIterator = books.iterator();

            assertTrue(booksIterator.hasNext());
            Book received = booksIterator.next();
            assertEquals("Enders Game", received.getTitle());
            assertEquals(1, received.getId());

            assertTrue(booksIterator.hasNext());
            received = booksIterator.next();
            assertEquals("Grapes of Wrath", received.getTitle());
            assertEquals(2, received.getId());

            assertFalse(booksIterator.hasNext());
        }
    }
}
