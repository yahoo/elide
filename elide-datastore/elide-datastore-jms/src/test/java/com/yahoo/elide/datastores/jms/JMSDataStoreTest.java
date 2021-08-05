package com.yahoo.elide.datastores.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yahoo.elide.core.datastore.DataStoreTransaction;
import com.yahoo.elide.core.dictionary.EntityDictionary;
import com.yahoo.elide.core.request.EntityProjection;
import com.yahoo.elide.core.type.ClassType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import example.Book;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;

public class JMSDataStoreTest {

    @Test
    public void myTest() throws Exception {
        EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();
        Configuration configuration = new ConfigurationImpl();
        configuration.addAcceptorConfiguration("default", "vm://0");
        configuration.setPersistenceEnabled(true);
        configuration.setSecurityEnabled(false);
        configuration.setJournalType(JournalType.NIO);

        embedded.setConfiguration(configuration);
        embedded.start();

        ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory("vm://0");

        Book book = new Book();
        book.setTitle("Enders Game");
        book.setId(1);
        ObjectMapper mapper = new ObjectMapper();

        JMSContext context = cf.createContext();
        context.start();
        Destination destination = context.createTopic("bookAdded");

        JMSProducer producer = context.createProducer();
        producer.setTimeToLive(50000);
        producer.send(destination, mapper.writeValueAsString(book));

        EntityDictionary dictionary = new EntityDictionary(new HashMap<>());
        dictionary.bindEntity(Book.class);

        JMSDataStore store = new JMSDataStore(Sets.newHashSet(ClassType.of(Book.class)), cf, dictionary);

        store.populateEntityDictionary(dictionary);

        DataStoreTransaction tx = store.beginReadTransaction();

        Iterable<Book> books = tx.loadObjects(
                EntityProjection.builder().type(Book.class).build(),
                null
        );

        Book received = books.iterator().next();
        assertEquals("Enders Game", received.getTitle());
    }
}
